package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import com.beepsterr.fixfoot.compat.Interactions;
import net.minecraft.world.InteractionResult;
//? if <1.21.2 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Modern slat panels — a multiface block (the glow-lichen / sculk-vein mechanic) restricted to
 * the four horizontal walls (N/E/S/W). Each wall a panel touches toggles independently, so a
 * 1-wide walkway can carry slats on both sides in the same block. Registered {@code .noCollission()}
 * so you can walk through the 2px slats and they overlap block space.
 *
 * <p>Optional overgrown look: use a Vine or Pink Petals on a slat to cover it (an {@link #OVERLAY}
 * renders ~1px in front of the bars); Shears strip it back. Vines and petals share one enum
 * property rather than taking a boolean each, so they're mutually exclusive by construction and
 * the state count grows by half instead of doubling.
 */
public class SlatPanelBlock extends MultifaceBlock {
    public static final MapCodec<SlatPanelBlock> CODEC = simpleCodec(SlatPanelBlock::new);
    public static final EnumProperty<Overlay> OVERLAY = EnumProperty.create("overlay", Overlay.class);

    /** What has grown over the bars. One at a time — shear it off before applying the other. */
    public enum Overlay implements StringRepresentable {
        NONE("none", () -> Items.AIR),
        VINE("vine", () -> Items.VINE),
        PINK_PETALS("pink_petals", () -> Items.PINK_PETALS);

        private final String name;
        private final Supplier<Item> item;

        Overlay(String name, Supplier<Item> item) {
            this.name = name;
            this.item = item;
        }

        /** The item that applies this overlay, and that Shears hand back. */
        public Item item() {
            return item.get();
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** The overlay {@code stack} would apply, or {@link #NONE} if it isn't an overlay item. */
        public static Overlay forItem(ItemStack stack) {
            for (Overlay overlay : values()) {
                if (overlay != NONE && stack.is(overlay.item())) {
                    return overlay;
                }
            }
            return NONE;
        }
    }

    /**
     * Depth of the shape per placed face, in pixels, matching the authored model: bars run 0..2,
     * and an overlay adds a plane one pixel further out. Vanilla {@link MultifaceBlock}
     * hard-codes 1px, which left the outline short of the bars — enough that anything measuring it
     * (a fixture attaching itself to the slats) ended up sitting inside the model.
     */
    private static final int BAR_DEPTH = 2;
    private static final int OVERLAY_DEPTH = 3;

    // MultifaceBlock requires a spreader (used by growth/bonemeal spreading). We never trigger
    // spreading, but the abstract method must return non-null.
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);
    private final Map<BlockState, VoxelShape> shapes;

    public SlatPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OVERLAY, Overlay.NONE));
        this.shapes = getShapeForEachState(SlatPanelBlock::calculateShape);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state);
    }

    /** Union of one slab per placed face. Registered {@code .noCollission()}, so this is the outline only. */
    private static VoxelShape calculateShape(BlockState state) {
        int depth = state.getValue(OVERLAY) == Overlay.NONE ? BAR_DEPTH : OVERLAY_DEPTH;
        VoxelShape shape = Shapes.empty();
        for (Direction face : Direction.values()) {
            if (hasFace(state, face)) {
                shape = Shapes.or(shape, faceShape(face, depth));
            }
        }
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    private static VoxelShape faceShape(Direction face, int depth) {
        return switch (face) {
            case NORTH -> Block.box(0, 0, 0, 16, 16, depth);
            case SOUTH -> Block.box(0, 0, 16 - depth, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, depth, 16, 16);
            case EAST -> Block.box(16 - depth, 0, 0, 16, 16, 16);
            case DOWN -> Block.box(0, 0, 0, 16, depth, 16);
            case UP -> Block.box(0, 16 - depth, 0, 16, 16, 16);
        };
    }

    @Override
    protected MapCodec<? extends MultifaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OVERLAY);
    }

    @Override
    protected boolean isFaceSupported(Direction face) {
        return face.getAxis() != Direction.Axis.Y; // walls only, no floor/ceiling
    }

    //? if <1.21.2 {
    @Override
    public MultifaceSpreader getSpreader() {
        return spreader;
    }
    //?}

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // Only more slats (same item) may replace this — to add another face. Other blocks such as
        // vanilla vines or glow lichen must not override the slats.
        return context.getItemInHand().is(asItem()) && super.canBeReplaced(state, context);
    }

    @Override
    //? if >=1.21.2 {
    /*protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
    *///?} else {
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
    //?}
        Overlay overlay = state.getValue(OVERLAY);
        // Cover bare slats with vines or pink petals. Covered slats take neither — shear first,
        // so swapping the look is deliberate rather than a misclick with a stack of petals.
        Overlay applied = Overlay.forItem(stack);
        if (overlay == Overlay.NONE && applied != Overlay.NONE) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(OVERLAY, applied), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return Interactions.used(level);
        }
        // Strip the overlay back off with shears.
        if (overlay != Overlay.NONE && stack.is(Items.SHEARS)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(OVERLAY, Overlay.NONE), Block.UPDATE_ALL);
                Block.popResource(level, pos, new ItemStack(overlay.item()));
            }
            return Interactions.used(level);
        }
        return Interactions.passToBlock();
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
        //? if >=1.21.2 {
        /*BlockState result = super.updateShape(state, level, tickAccess, pos, direction, neighborPos,
                neighborState, random);
        *///?} else {
        BlockState result = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        //?}
        // If one face lost its support but the block survives on another face, vanilla
        // MultifaceBlock removes that face silently. Each face is a placed slat, so drop it back.
        if (level instanceof ServerLevel serverLevel
                && !result.isAir()
                && hasFace(state, direction)
                && !hasFace(result, direction)) {
            Block.popResource(serverLevel, pos, new ItemStack(this));
        }
        return result;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // One slat per placed face, plus the overlay item back if it was covered.
        int faces = 0;
        for (Direction direction : Direction.values()) {
            if (hasFace(state, direction)) {
                faces++;
            }
        }
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this, Math.max(1, faces)));
        Overlay overlay = state.getValue(OVERLAY);
        if (overlay != Overlay.NONE) {
            drops.add(new ItemStack(overlay.item()));
        }
        return drops;
    }
}
