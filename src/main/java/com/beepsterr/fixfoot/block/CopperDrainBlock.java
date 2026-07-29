package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        this.shapes = getShapeForEachState(state -> Facings.turn(SHAPE, state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
