package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A copper wash basin: a cauldron that only ever holds water, and stands 4px shorter than one.
 *
 * <p>Deliberately not an {@code AbstractCauldronBlock}. Vanilla's cauldron machinery is welded to
 * vanilla blocks — {@code CauldronInteraction.FILL_WATER} hands you a {@code Blocks.WATER_CAULDRON},
 * and the static {@code LayeredCauldronBlock.lowerFillLevel} turns the block into a
 * {@code Blocks.CAULDRON} when the last layer drains, so a subclass would transmute into a vanilla
 * cauldron the first time you rinsed a banner in it. The interactions below are the vanilla ones
 * re-pointed at this block.
 *
 * <p>Also one block with {@code level=0..3} rather than vanilla's empty/filled block pair — that
 * split only exists because vanilla's interaction map is keyed per block, and this handles items
 * itself. The trade is no dripstone filling: {@code PointedDripstoneBlock} only looks for an
 * {@code AbstractCauldronBlock} beneath a stalactite.
 */
public class CopperBasinBlock extends Block {
    public static final MapCodec<? extends Block> CODEC = simpleCodec(CopperBasinBlock::new);

    /** 0 = empty, 3 = brim-full. Same three visible water layers a cauldron has. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final int MAX_LEVEL = 3;

    /**
     * Geometry in pixels, tracing the authored model (assets/models/blockbench/CopperBasin.bbmodel,
     * which {@code tools/gen_basin.py} reads to build the block models): four L-shaped feet run
     * 0..2, a solid base slab 2..4, and the bowl walls 4..14 around a 2..14 interior. The water
     * surface sits at 7/10/13 for levels 1..3 — a pixel below the rim when full, as a cauldron reads.
     */
    private static final int FOOT_HEIGHT = 2;
    private static final int FLOOR = 4;
    private static final int RIM = 14;
    private static final int WATER_PER_LEVEL = 3;

    private static final VoxelShape INSIDE = box(2.0, FLOOR, 2.0, 14.0, RIM, 14.0);
    private static final VoxelShape SHAPE = Shapes.or(
            // Feet: an L at each corner, leaving the gaps between them open.
            box(0.0, 0.0, 0.0, 2.0, FOOT_HEIGHT, 4.0), box(0.0, 0.0, 0.0, 4.0, FOOT_HEIGHT, 2.0),
            box(14.0, 0.0, 0.0, 16.0, FOOT_HEIGHT, 4.0), box(12.0, 0.0, 0.0, 16.0, FOOT_HEIGHT, 2.0),
            box(0.0, 0.0, 12.0, 2.0, FOOT_HEIGHT, 16.0), box(0.0, 0.0, 14.0, 4.0, FOOT_HEIGHT, 16.0),
            box(14.0, 0.0, 12.0, 16.0, FOOT_HEIGHT, 16.0), box(12.0, 0.0, 14.0, 16.0, FOOT_HEIGHT, 16.0),
            // Base slab, then the four walls ringing the bowl.
            box(0.0, FOOT_HEIGHT, 0.0, 16.0, FLOOR, 16.0),
            box(0.0, FLOOR, 0.0, 16.0, RIM, 2.0), box(0.0, FLOOR, 14.0, 16.0, RIM, 16.0),
            box(0.0, FLOOR, 2.0, 2.0, RIM, 14.0), box(14.0, FLOOR, 2.0, 16.0, RIM, 14.0));

    public CopperBasinBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Lets a click land on the water inside rather than sliding off the rim. */
    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    /** Height of the water surface as a fraction of the block, matching the model's content quad. */
    private static double contentHeight(BlockState state) {
        return (FLOOR + WATER_PER_LEVEL * state.getValue(LEVEL)) / 16.0;
    }

    /**
     * Tips one layer in, if there is room. Returns true either way — a full basin still counts as
     * something the water has landed in, so a spout above it does not keep looking further down.
     */
    static boolean catchWater(Level level, BlockPos pos, BlockState state) {
        int fill = state.getValue(LEVEL);
        if (fill < MAX_LEVEL) {
            setLevel(state, level, pos, fill + 1);
        }
        return true;
    }

    private static void setLevel(BlockState state, Level level, BlockPos pos, int fill) {
        BlockState result = state.setValue(LEVEL, fill);
        level.setBlockAndUpdate(pos, result);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(result));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int fill = state.getValue(LEVEL);

        // A bucket always fills to the brim, however little was in there — same as a cauldron.
        if (stack.is(Items.WATER_BUCKET) && fill < MAX_LEVEL) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(Items.WATER_BUCKET));
                setLevel(state, level, pos, MAX_LEVEL);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // ...and only a full basin gives one back, so three bottles can't become a bucket.
        if (stack.is(Items.BUCKET) && fill == MAX_LEVEL) {
            if (!level.isClientSide) {
                player.setItemInHand(hand,
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
                setLevel(state, level, pos, 0);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.GLASS_BOTTLE) && fill > 0) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                        PotionContents.createItemStack(Items.POTION, Potions.WATER)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
                setLevel(state, level, pos, fill - 1);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.POTION) && fill < MAX_LEVEL) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                if (!level.isClientSide) {
                    player.setItemInHand(hand,
                            ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(Items.POTION));
                    setLevel(state, level, pos, fill + 1);
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // Washing. Each of these costs a layer. Matched on components/tags rather than vanilla's
        // hardcoded item lists, so modded leather armour and banners work too.
        if (fill > 0) {
            if (stack.is(ItemTags.DYEABLE) && stack.has(DataComponents.DYED_COLOR)) {
                if (!level.isClientSide) {
                    stack.remove(DataComponents.DYED_COLOR);
                    player.awardStat(Stats.CLEAN_ARMOR);
                    setLevel(state, level, pos, fill - 1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            BannerPatternLayers patterns = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            if (!patterns.layers().isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack washed = stack.copyWithCount(1);
                    washed.set(DataComponents.BANNER_PATTERNS, patterns.removeLast());
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, washed, false));
                    player.awardStat(Stats.CLEAN_BANNER);
                    setLevel(state, level, pos, fill - 1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                if (!level.isClientSide) {
                    // transmuteCopy keeps the contents; only the colour is washed out.
                    ItemStack washed = stack.transmuteCopy(Blocks.SHULKER_BOX, 1);
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, washed, false));
                    player.awardStat(Stats.CLEAN_SHULKER_BOX);
                    setLevel(state, level, pos, fill - 1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // Honeycomb and axes fall through to the item, which is what applies/strips the wax.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Douses anything burning that steps in, spending a layer like a cauldron does. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int fill = state.getValue(LEVEL);
        if (level.isClientSide || fill == 0 || !entity.isOnFire()) {
            return;
        }
        if (entity.getY() < pos.getY() + contentHeight(state)
                && entity.getBoundingBox().maxY > pos.getY() + FLOOR / 16.0) {
            entity.clearFire();
            if (entity.mayInteract(level, pos)) {
                setLevel(state, level, pos, fill - 1);
            }
        }
    }

    /** Rain tops it up one layer, on vanilla's 5%-per-precipitation-tick roll. Snow does nothing. */
    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        int fill = state.getValue(LEVEL);
        if (precipitation == Biome.Precipitation.RAIN && fill < MAX_LEVEL
                && level.getRandom().nextFloat() < 0.05F) {
            setLevel(state, level, pos, fill + 1);
        }
    }
}
