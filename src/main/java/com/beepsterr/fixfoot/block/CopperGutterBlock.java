package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
//? if >=1.21.2 {
/*import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
*///?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<GutterShape> SHAPE = EnumProperty.create("shape", GutterShape.class);
    /**
     * How far along a run to look for rain. A gutter under an eave catches what the roof sheds, so
     * the one a pipe hangs off is usually sheltered and the open stretch is somewhere further along.
     */
    private static final int RUN_REACH = 64;

    /**
     * Which authored piece this block is. Not vanilla's {@code StairsShape}: a stair is symmetric
     * front-to-back and a channel is not, so a gutter tells apart three corners where stairs see
     * two — and it has no left/right pairs, because a corner's handedness is carried by which
     * facing the corner itself takes.
     */
    /**
     * Which piece this is. Nine values: the run passing through, and the eight ways it can turn.
     *
     * <p>A corner is fully described by three yes/no answers — which side the run arrives from,
     * whether it leaves by the wall side or the open side, and whether it turns at the near end of
     * the block or crosses to the far one. The flags are what the derivation works in; which model
     * draws it, and how far that model is turned, is the generator's business.
     *
     * <p>Deliberately independent of {@link #FACING}. Tying a model's rotation to the block's facing
     * is what forced the facing to be derived rather than placed, and deriving both is what made
     * every gutter's state depend on its neighbours' — one run, one fixpoint, endlessly re-solving.
     */
    public enum GutterShape implements StringRepresentable {
        STRAIGHT("straight"),
        LEFT_FRONT_NEAR("left_front_near", true, true, true),
        LEFT_FRONT_FAR("left_front_far", true, true, false),
        LEFT_BACK_NEAR("left_back_near", true, false, true),
        LEFT_BACK_FAR("left_back_far", true, false, false),
        RIGHT_FRONT_NEAR("right_front_near", false, true, true),
        RIGHT_FRONT_FAR("right_front_far", false, true, false),
        RIGHT_BACK_NEAR("right_back_near", false, false, true),
        RIGHT_BACK_FAR("right_back_far", false, false, false);

        private final String name;
        private final boolean corner;
        private final boolean fromLeft;
        private final boolean toFront;
        private final boolean near;

        GutterShape(String name) {
            this.name = name;
            this.corner = false;
            this.fromLeft = false;
            this.toFront = false;
            this.near = false;
        }

        GutterShape(String name, boolean fromLeft, boolean toFront, boolean near) {
            this.name = name;
            this.corner = true;
            this.fromLeft = fromLeft;
            this.toFront = toFront;
            this.near = near;
        }

        /**
         * Whether channel runs across the middle of the block, which is where a drain pipe's collar
         * arrives. A corner turning at the near end is done before it gets there, so a pipe below
         * one has nothing to meet and stays a plain column.
         */
        public boolean carriesAcross() {
            return !near;
        }

        /** The two sides this piece joins, for a block facing {@code facing}. */
        public List<Direction> sides(Direction facing) {
            Direction left = facing.getCounterClockWise();
            Direction right = facing.getClockWise();
            if (!corner) {
                return List.of(left, right);
            }
            return List.of(fromLeft ? left : right, toFront ? facing : facing.getOpposite());
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        static GutterShape corner(boolean fromLeft, boolean toFront, boolean near) {
            for (GutterShape shape : values()) {
                if (shape.corner && shape.fromLeft == fromLeft
                        && shape.toFront == toFront && shape.near == near) {
                    return shape;
                }
            }
            throw new IllegalStateException("no corner for " + fromLeft + " " + toFront + " " + near);
        }
    }

    /** Footprints of the authored models, un-turned, against the north wall. */
    private static final VoxelShape CHANNEL = box(0.0, 0.0, 0.0, 16.0, 2.0, 6.0);
    private static final VoxelShape CORNER = box(0.0, 0.0, 0.0, 6.0, 2.0, 6.0);
    private static final VoxelShape WRAP = Shapes.or(CHANNEL, box(10.0, 0.0, 0.0, 16.0, 2.0, 16.0));

    /**
     * Which footprint each shape wears and how far it is turned, matching what gen_drainage.py
     * points the blockstate at. Five models cover nine shapes because a corner turned is a
     * different corner — which is exactly what lets the facing stay as the player set it.
     */
    private static final Map<GutterShape, VoxelShape> FOOTPRINT = Map.of(
            GutterShape.STRAIGHT, CHANNEL,
            GutterShape.LEFT_FRONT_NEAR, CORNER,
            GutterShape.LEFT_FRONT_FAR, CHANNEL,
            GutterShape.LEFT_BACK_NEAR, CHANNEL,
            GutterShape.LEFT_BACK_FAR, WRAP,
            GutterShape.RIGHT_FRONT_NEAR, CORNER,
            GutterShape.RIGHT_FRONT_FAR, CHANNEL,
            GutterShape.RIGHT_BACK_NEAR, CHANNEL,
            GutterShape.RIGHT_BACK_FAR, WRAP);

    private static final Map<GutterShape, Integer> TURNS = Map.of(
            GutterShape.STRAIGHT, 0,
            GutterShape.LEFT_FRONT_NEAR, 0,
            GutterShape.LEFT_FRONT_FAR, 0,
            GutterShape.LEFT_BACK_NEAR, 3,
            GutterShape.LEFT_BACK_FAR, 0,
            GutterShape.RIGHT_FRONT_NEAR, 1,
            GutterShape.RIGHT_FRONT_FAR, 0,
            GutterShape.RIGHT_BACK_NEAR, 1,
            GutterShape.RIGHT_BACK_FAR, 3);

    private final Map<BlockState, VoxelShape> shapes;

    public CopperGutterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, GutterShape.STRAIGHT));
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
        GutterShape shape = state.getValue(SHAPE);
        return Facings.turn(FOOTPRINT.get(shape),
                TURNS.get(shape) + Facings.quarters(state.getValue(FACING)));
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

        return connect(defaultBlockState().setValue(FACING, facing), context.getLevel(), pos);
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
    //? if >=1.21.2 {
    /*protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
    *///?} else {
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
    //?}
        return direction.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    /**
     * Works out which piece this is, leaving {@link #FACING} exactly as it was placed.
     *
     * <p>Only two things are read, and both are the neighbours' facings — never a shape. Nothing
     * here depends on a value a neighbour is deriving at the same moment, so there is no order to
     * get right, nothing to deadlock, and a gutter placed across the room cannot re-solve this one.
     *
     * <p>A run passing straight through wins outright, which is the guard stairs use: a piece
     * already joined on both sides has nothing left to turn with, so a third gutter arriving
     * alongside cannot pull it round.
     */
    private static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        boolean fromLeft = continues(level, pos.relative(left), facing);
        boolean fromRight = continues(level, pos.relative(right), facing);
        // Both, or neither: either way there is no turn to make.
        if (fromLeft == fromRight) {
            return state.setValue(SHAPE, GutterShape.STRAIGHT);
        }

        for (boolean front : new boolean[] {true, false}) {
            Direction exit = front ? facing : facing.getOpposite();
            BlockState neighbour = level.getBlockState(pos.relative(exit));
            if (!isGutter(neighbour)) {
                continue;
            }
            // Where that gutter's channel meets the edge we share is set by the wall it backs onto,
            // which tells us whether the turn happens at the near end of this block or the far one.
            Direction other = neighbour.getValue(FACING);
            if (other != left && other != right) {
                continue;
            }
            return state.setValue(SHAPE,
                    GutterShape.corner(fromLeft, front, (other == left) == fromLeft));
        }
        return state.setValue(SHAPE, GutterShape.STRAIGHT);
    }

    /** A gutter carrying the same run: same wall, so its channel lines up with ours. */
    private static boolean continues(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return isGutter(state) && state.getValue(FACING) == facing;
    }

    /**
     * Whether any gutter on this run is out in the rain.
     *
     * <p>Asked by a drain rather than stored on the gutters. A gutter carries no wet state of its
     * own: it was only ever read in one place, and the answer is a question about the weather, which
     * is a single global that changes every few minutes — so it is cheaper to ask when something
     * cares than to keep hundreds of blocks polling for it.
     *
     * <p>The walk matters because the gutter a pipe hangs off is normally the sheltered one, tucked
     * under the eave catching what the roof sheds. Following the run finds the open stretch.
     */
    public static boolean runCatchesRain(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty() && seen.size() <= RUN_REACH) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!isGutter(state)) {
                continue;
            }
            // One block up: a gutter is the top of its column, and isRainingAt reports false for
            // the topmost block itself.
            if (level.isRainingAt(pos.above())) {
                return true;
            }
            for (Direction side : state.getValue(SHAPE).sides(state.getValue(FACING))) {
                BlockPos linked = pos.relative(side);
                if (seen.add(linked)) {
                    queue.add(linked);
                }
            }
        }
        return false;
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
