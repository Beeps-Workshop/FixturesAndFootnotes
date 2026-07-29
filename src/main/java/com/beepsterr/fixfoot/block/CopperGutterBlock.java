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
    public enum GutterShape implements StringRepresentable {
        STRAIGHT("straight"),
        /** Turns out the front edge, toward a neighbour facing left. */
        INNER("inner"),
        /** Crosses the block, then turns out the front edge toward a neighbour facing right. */
        TIGHT("tight"),
        /** Crosses the block, then wraps out the back edge toward a neighbour facing right. */
        WIDE("wide"),
        /** {@link #TIGHT} mirrored: the same turn the other way round, which no facing can reach. */
        TIGHT_MIRRORED("tight_mirrored");

        private final String name;

        GutterShape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Footprints against the north wall, tracing each authored model. */
    private static final VoxelShape CHANNEL = box(0.0, 0.0, 0.0, 16.0, 2.0, 6.0);
    private static final VoxelShape CORNER = box(0.0, 0.0, 0.0, 6.0, 2.0, 6.0);
    private static final VoxelShape WRAP = Shapes.or(CHANNEL, box(10.0, 0.0, 0.0, 16.0, 2.0, 16.0));

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
        VoxelShape shape = switch (state.getValue(SHAPE)) {
            case STRAIGHT, TIGHT, TIGHT_MIRRORED -> CHANNEL;
            case INNER -> CORNER;
            case WIDE -> WRAP;
        };
        return Facings.turn(shape, state.getValue(FACING));
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
     * Which neighbours each authored piece joins, described in the base (north-facing) frame as
     * "a gutter on this side, backing onto that wall". Read straight off the models: the channel
     * leaves a block edge at a fixed offset, and only one neighbour facing puts a channel there.
     */
    private record Link(Direction side, Direction backsOnto) {
    }

    private static final Map<GutterShape, List<Link>> LINKS = Map.of(
            GutterShape.STRAIGHT, List.of(new Link(Direction.WEST, Direction.NORTH),
                                          new Link(Direction.EAST, Direction.NORTH)),
            GutterShape.INNER, List.of(new Link(Direction.WEST, Direction.NORTH),
                                       new Link(Direction.NORTH, Direction.WEST)),
            GutterShape.TIGHT, List.of(new Link(Direction.WEST, Direction.NORTH),
                                       new Link(Direction.NORTH, Direction.EAST)),
            GutterShape.WIDE, List.of(new Link(Direction.WEST, Direction.NORTH),
                                      new Link(Direction.SOUTH, Direction.EAST)),
            GutterShape.TIGHT_MIRRORED, List.of(new Link(Direction.EAST, Direction.NORTH),
                                                new Link(Direction.NORTH, Direction.WEST)));

    /**
     * Works out which piece this is from the run around it, deriving facing as well as shape.
     *
     * <p>Every corner is tried at every facing rather than pattern-matched, because the corners are
     * not mirror-symmetric: an L turning left is a different piece from one turning right, and only
     * some of those mirrors happen to coincide with another corner under another facing. Searching
     * finds whichever of the sixteen states actually joins the neighbours that are there.
     */
    private static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        for (GutterShape shape : List.of(GutterShape.INNER, GutterShape.TIGHT,
                GutterShape.WIDE, GutterShape.TIGHT_MIRRORED)) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                if (joins(shape, facing, level, pos)) {
                    return state.setValue(FACING, facing).setValue(SHAPE, shape);
                }
            }
        }
        // Not a corner, so the facing the player placed it with stands.
        return state.setValue(SHAPE, GutterShape.STRAIGHT);
    }

    private static boolean joins(GutterShape shape, Direction facing, BlockGetter level, BlockPos pos) {
        for (Link link : LINKS.get(shape)) {
            Direction side = Facings.turn(link.side(), facing);
            if (!linksBack(level, pos.relative(side), side.getOpposite(), Facings.turn(link.backsOnto(), facing))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the gutter at {@code pos} can take a connection from {@code toUs} — it has to back
     * onto the right wall for the channels to line up, and its own shape has to spend one of its
     * links on us. A corner already using both of its links is full, so a run arriving at one is
     * left as a straight rather than quietly stealing a connection the corner cannot return.
     */
    private static boolean linksBack(BlockGetter level, BlockPos pos, Direction toUs, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (!isGutter(state) || state.getValue(FACING) != facing) {
            return false;
        }
        for (Link link : LINKS.get(state.getValue(SHAPE))) {
            if (Facings.turn(link.side(), facing) == toUs) {
                return true;
            }
        }
        return false;
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
            for (Link link : LINKS.get(state.getValue(SHAPE))) {
                BlockPos linked = pos.relative(Facings.turn(link.side(), state.getValue(FACING)));
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
