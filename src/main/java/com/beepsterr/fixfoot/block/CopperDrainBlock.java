package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.beepsterr.fixfoot.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The outlet at the foot of a {@link CopperDrainPipeBlock} run: a length of column that elbows out
 * into a spout, lining up with the pipe above and emptying to one side.
 *
 * <p>A block of its own rather than a state of the pipe, so a run can just as well disappear into
 * the ground or stop at a wall. Nothing derives it and nothing derives from it — {@link #FACING} is
 * purely which way you pointed the mouth, and the pipe above neither knows nor cares it is there.
 */
public class CopperDrainBlock extends Block {
    public static final MapCodec<? extends Block> CODEC = simpleCodec(CopperDrainBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Flow> FLOW = EnumProperty.create("flow", Flow.class);

    /** What is coming out of the mouth. */
    public enum Flow implements StringRepresentable {
        EMPTY("empty"),
        /** A filled gutter is feeding the pipe run above. */
        FLOWING("flowing"),
        /** That gutter has dried; what is left in the run is still finding its way out. */
        DRIPPING("dripping");

        private final String name;

        Flow(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Ticks between drops while flowing. Every tick, so the stream is continuous rather than pulsed. */
    private static final int FLOW_INTERVAL = 1;
    /** Drops per burst. One a tick reads as a stream without a packet storm behind it. */
    private static final int DROPS_PER_BURST = 1;
    /**
     * Average ticks per layer poured into whatever is underneath — twenty seconds, so a basin takes
     * about a minute of steady rain to fill. Still far quicker than the crawl of a cauldron left out
     * in the weather, which is the point of plumbing a roof, but slow enough to be worth roofing.
     */
    private static final int FILL_INTERVAL = 400;
    /** How far below the spout to look for something to fill before giving up. */
    private static final int MAX_FALL = 16;
    /** How hard water leaves the mouth, so it arcs clear of the wall instead of dribbling down it. */
    private static final double SPOUT_SPEED = 0.14;

    /**
     * Tracing the authored model: the pipe-width column, and the spout elbowing out of its foot.
     * The spout carries on a pixel past the block edge, which the shape clips off — a VoxelShape
     * outside the unit cube misbehaves, and the overhang is decoration.
     */
    private static final VoxelShape SHAPE = Shapes.or(
            box(6.0, 3.0, 6.0, 10.0, 16.0, 10.0),
            box(6.0, 3.0, 0.0, 10.0, 7.0, 6.0));

    private final Map<BlockState, VoxelShape> shapes;

    public CopperDrainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FLOW, Flow.EMPTY));
        this.shapes = getShapeForEachState(state -> Facings.turn(SHAPE, state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FLOW);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Mouth toward the player, the way the sheetmetal blocks orient.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /**
     * Steps toward whatever the run above is doing. Losing its feed drops the drain to dripping
     * rather than straight to empty, so a run tails off over a few ticks the way a real one would
     * instead of shutting off the moment the rain stops.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        settle(state, level, pos);
    }

    /** Poked by a gutter upstream changing, so the mouth answers the weather rather than a poll. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        settle(state, level, pos);
    }

    private static void settle(BlockState state, ServerLevel level, BlockPos pos) {
        Flow flow = state.getValue(FLOW);
        Flow next = CopperDrainPipeBlock.fedRun(level, pos.above()) ? Flow.FLOWING : flow == Flow.FLOWING ? Flow.DRIPPING : Flow.EMPTY;
        if (next != flow) {
            level.setBlock(pos, state.setValue(FLOW, next), Block.UPDATE_CLIENTS);
        }
        // Dripping is left to the random tick to stop, so a run tails off unevenly rather than
        // on a timer. Flowing drives itself, since animateTick is far too sparse for a stream.
        if (next == Flow.FLOWING) {
            spout(level, pos, state.getValue(FACING));
            if (level.random.nextInt(FILL_INTERVAL) == 0) {
                pour(level, pos, state.getValue(FACING));
            }
            if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
                level.scheduleTick(pos, state.getBlock(), FLOW_INTERVAL);
            }
        }
    }

    /**
     * Throws water out of the mouth with some speed behind it.
     *
     * <p>Uses the mod's own drip particle, which is vanilla's falling water with the velocity kept
     * rather than discarded. Sent a particle at a time with a count of zero, since that is the form
     * where the offsets are read as a velocity rather than as a spread.
     */
    /**
     * Tips a layer into whatever the spout is raining on: one block out in the direction it points,
     * which is where the drops actually land, then straight down until the water meets something.
     */
    private static void pour(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos cursor = pos.relative(facing);
        for (int i = 0; i < MAX_FALL; i++) {
            BlockState state = level.getBlockState(cursor);
            if (catchWater(level, cursor, state)) {
                return;
            }
            if (!state.isAir() && !state.canBeReplaced()) {
                return; // the water is landing on this, whatever it is
            }
            cursor = cursor.below();
        }
    }

    /** True when this block is something falling water collects in, whether or not it had room. */
    private static boolean catchWater(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CopperBasinBlock) {
            return CopperBasinBlock.catchWater(level, pos, state);
        }
        if (state.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
            return true;
        }
        // Only the water one: a lava or powder snow cauldron is a different block, so it is left be.
        if (state.is(Blocks.WATER_CAULDRON)) {
            int fill = state.getValue(LayeredCauldronBlock.LEVEL);
            if (fill < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                level.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, fill + 1));
            }
            return true;
        }
        return false;
    }

    private static void spout(ServerLevel level, BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.55;
        double y = pos.getY() + 0.28;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.55;
        for (int i = 0; i < DROPS_PER_BURST; i++) {
            double fan = (level.random.nextDouble() - 0.5) * 0.35; // splay across the mouth
            level.sendParticles(ModParticles.SPOUT_WATER.get(),
                    x + (level.random.nextDouble() - 0.5) * 0.12,
                    y + (level.random.nextDouble() - 0.5) * 0.06,
                    z + (level.random.nextDouble() - 0.5) * 0.12,
                    0,
                    facing.getStepX() + facing.getStepZ() * fan,
                    0.25, // a little lift, so it arcs out of the mouth rather than falling off it
                    facing.getStepZ() + facing.getStepX() * fan,
                    SPOUT_SPEED);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FLOW) != Flow.DRIPPING) {
            return; // flowing is driven server-side, at a rate animateTick cannot reach
        }
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.55;
        double y = pos.getY() + 0.28;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.55;
        level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, 0.0, 0.0);
        if (random.nextInt(3) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                    SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
