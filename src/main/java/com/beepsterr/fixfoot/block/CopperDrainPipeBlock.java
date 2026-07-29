package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The downpipe a {@link CopperGutterBlock} run empties into — a plain vertical column that grows a
 * collar at whichever end meets a gutter.
 *
 * <p>Neither property is chosen by the player. {@link #CONNECTED} is true when a gutter sits one
 * block up and one block out — a roof overhangs the wall the pipe runs down, so the pipe tucks back
 * against that wall instead of hanging in the air under the eave. {@link #FACING} then points the
 * collar at whichever side that gutter was on. Both are recomputed when the block above changes, so
 * stacking a pipe under an existing one quietly demotes the old top section back to plain column.
 */
public class CopperDrainPipeBlock extends Block {
    public static final MapCodec<? extends Block> CODEC = simpleCodec(CopperDrainPipeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    /** Carrying water, mirrored down the run from the gutter at its head. */
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    /** How far along a run to walk before giving up on finding its gutter or its drain. */
    private static final int MAX_RUN = 32;

    private static final VoxelShape COLUMN = box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    /**
     * The collar's arm reaching north toward its gutter. The authored model carries on past the
     * block edge to meet a gutter a whole block away; the shape stops at the boundary, since a
     * VoxelShape outside the unit cube misbehaves and the overhang is decoration either way.
     */
    private static final VoxelShape COLLAR = box(7.0, 10.0, 0.0, 9.0, 13.0, 10.0);

    private final Map<BlockState, VoxelShape> shapes;

    public CopperDrainPipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, false)
                .setValue(FILLED, false));
        this.shapes = getShapeForEachState(CopperDrainPipeBlock::calculateShape);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED, FILLED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    /**
     * Whether the gutter at the head of this run is carrying water. Walks up through the pipes to
     * whichever one wears the collar, then asks the gutter it points at.
     */
    static boolean fedRun(LevelReader level, BlockPos pipe) {
        BlockPos cursor = pipe;
        for (int i = 0; i < MAX_RUN; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof CopperDrainPipeBlock)) {
                return false;
            }
            if (state.getValue(CONNECTED)) {
                BlockState gutter = level.getBlockState(cursor.above().relative(state.getValue(FACING)));
                return CopperGutterBlock.isGutter(gutter) && gutter.getValue(CopperGutterBlock.FILLED);
            }
            cursor = cursor.above();
        }
        return false;
    }

    /**
     * Carries the wet or dry state of every run hanging off the gutter at {@code pos} down its
     * pipes, and wakes the drain at the foot of each.
     *
     * <p>Pushed from the gutter rather than polled: a drain left to its own random tick would only
     * look about once a minute, and only count if it happened to land while the gutter upstream was
     * still wet, so a shower could come and go without the mouth ever running.
     */
    static void refreshRunsBelow(ServerLevel level, BlockPos gutter) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos cursor = gutter.below().relative(direction);
            if (!(level.getBlockState(cursor).getBlock() instanceof CopperDrainPipeBlock)) {
                continue;
            }
            boolean wet = fedRun(level, cursor);
            for (int i = 0; i < MAX_RUN; i++) {
                BlockState state = level.getBlockState(cursor);
                if (state.getBlock() instanceof CopperDrainPipeBlock) {
                    if (state.getValue(FILLED) != wet) {
                        level.setBlock(cursor, state.setValue(FILLED, wet), Block.UPDATE_CLIENTS);
                    }
                    cursor = cursor.below();
                    continue;
                }
                if (state.getBlock() instanceof CopperDrainBlock) {
                    level.scheduleTick(cursor, state.getBlock(), 1);
                }
                break;
            }
        }
    }

    private static VoxelShape calculateShape(BlockState state) {
        if (!state.getValue(CONNECTED)) {
            return COLUMN;
        }
        return Shapes.or(COLUMN, Facings.turn(COLLAR, state.getValue(FACING)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP ? connect(state, level, pos)
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Finds the gutter this pipe hangs off: one block up and one block out, never straight
     * overhead. A roof overhangs the wall the pipe runs down, so the gutter is always offset —
     * and the authored collar reaches past the block edge on that assumption, which is why a
     * gutter directly above deliberately does not count.
     */
    static BlockState connect(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (CopperGutterBlock.isGutter(level.getBlockState(above.relative(direction)))) {
                return state.setValue(CONNECTED, true).setValue(FACING, direction);
            }
        }
        return state.setValue(CONNECTED, false);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
        return state.setValue(FILLED, fedRun(context.getLevel(), context.getClickedPos().above()));
    }

    /**
     * Re-evaluates the pipes a gutter at {@code pos} could feed. A diagonal neighbour never sends
     * a block update this far, so the gutter has to knock on the door itself when it is placed or
     * broken; the pipe directly beneath is covered by the ordinary neighbour update.
     */
    static void refreshBelow(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos below = pos.below().relative(direction);
            BlockState state = level.getBlockState(below);
            if (state.getBlock() instanceof CopperDrainPipeBlock) {
                BlockState updated = connect(state, level, below);
                if (updated != state) {
                    level.setBlock(below, updated, Block.UPDATE_CLIENTS);
                }
            }
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
