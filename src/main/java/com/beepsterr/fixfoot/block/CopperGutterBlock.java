package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A copper gutter: an open channel that hugs one wall of its block and runs along it, meeting its
 * neighbours at inner and outer corners the way stairs do.
 *
 * <p>{@link #FACING} is the wall the channel backs onto — placement sets it from the face you
 * clicked, so the gutter always lands against the block you aimed at. That is a placement rule
 * only: once down, a gutter survives its support being broken. Requiring support would tear runs
 * apart at outer corners, where an arm of the run deliberately overhangs nothing.
 *
 * <p>{@link #SHAPE} reuses vanilla's stairs vocabulary and is derived from the neighbours along the
 * run, never set by the player. The authored model dips a pixel below the block so the channel
 * floor meets the collar of a {@link CopperDrainPipeBlock} directly beneath; the collision shape
 * stops at 0 and lets that pixel overhang.
 */
public class CopperGutterBlock extends Block {
    public static final MapCodec<? extends Block> CODEC = simpleCodec(CopperGutterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;

    /** Channel footprint against the north wall, tracing the authored model. */
    private static final VoxelShape CHANNEL = box(0.0, 0.0, 0.0, 16.0, 2.0, 6.0);
    /** An outer corner turns the run out across the block; the arm runs down one side. */
    private static final VoxelShape ARM_RIGHT = box(10.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final VoxelShape ARM_LEFT = box(0.0, 0.0, 0.0, 6.0, 2.0, 16.0);

    private final Map<BlockState, VoxelShape> shapes;

    public CopperGutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, StairsShape.STRAIGHT));
        this.shapes = getShapeForEachState(CopperGutterBlock::calculateShape);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    private static VoxelShape calculateShape(BlockState state) {
        VoxelShape shape = switch (state.getValue(SHAPE)) {
            case STRAIGHT, INNER_LEFT, INNER_RIGHT -> CHANNEL;
            case OUTER_LEFT -> Shapes.or(CHANNEL, ARM_LEFT);
            case OUTER_RIGHT -> Shapes.or(CHANNEL, ARM_RIGHT);
        };
        // Authored facing north; turn it to match.
        for (Direction facing = Direction.NORTH; facing != state.getValue(FACING); facing = facing.getClockWise()) {
            shape = rotateClockwise(shape);
        }
        return shape;
    }

    /** Turns a shape a quarter turn about the block's vertical centre line. */
    private static VoxelShape rotateClockwise(VoxelShape shape) {
        List<VoxelShape> boxes = new ArrayList<>();
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> boxes.add(Shapes.box(1.0 - z2, y1, x1, 1.0 - z1, y2, x2)));
        return boxes.stream().reduce(Shapes::or).orElse(Shapes.empty());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        BlockState against = context.getLevel().getBlockState(pos.relative(clicked.getOpposite()));

        Direction facing;
        if (isGutter(against)) {
            // Extending a run: copy the gutter you clicked. Backing onto it instead would leave
            // the new piece square to the one it continues, which reads as a broken corner.
            facing = against.getValue(FACING);
        } else if (clicked.getAxis().isHorizontal()) {
            // Clicking a wall backs the channel onto it.
            facing = clicked.getOpposite();
        } else {
            // A floor or ceiling offers no wall to pick, so follow the way the player is looking.
            facing = context.getHorizontalDirection();
        }

        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.setValue(SHAPE, shapeFor(state, context.getLevel(), pos));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        CopperDrainPipeBlock.refreshBelow(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!newState.is(this)) {
            CopperDrainPipeBlock.refreshBelow(level, pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal()
                ? state.setValue(SHAPE, shapeFor(state, level, pos))
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Reads the corner off the run: a neighbour to either side whose channel backs onto a
     * different axis means the run turns here. Which way it turns picks inner versus outer.
     */
    private static StairsShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);

        Direction right = facing.getClockWise();
        BlockState beside = level.getBlockState(pos.relative(right));
        if (isGutter(beside)) {
            Direction other = beside.getValue(FACING);
            if (other.getAxis() != facing.getAxis()) {
                return other == right ? StairsShape.OUTER_RIGHT : StairsShape.INNER_RIGHT;
            }
        }

        Direction left = facing.getCounterClockWise();
        beside = level.getBlockState(pos.relative(left));
        if (isGutter(beside)) {
            Direction other = beside.getValue(FACING);
            if (other.getAxis() != facing.getAxis()) {
                return other == left ? StairsShape.OUTER_LEFT : StairsShape.INNER_LEFT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    /** True for every ageing stage and their waxed twins, so runs corner across mismatched copper. */
    public static boolean isGutter(BlockState state) {
        return state.getBlock() instanceof CopperGutterBlock;
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
