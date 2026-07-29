package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
                .setValue(CONNECTED, false));
        this.shapes = getShapeForEachState(CopperDrainPipeBlock::calculateShape);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    private static VoxelShape calculateShape(BlockState state) {
        if (!state.getValue(CONNECTED)) {
            return COLUMN;
        }
        return Shapes.or(COLUMN, Facings.turn(COLLAR, state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
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
