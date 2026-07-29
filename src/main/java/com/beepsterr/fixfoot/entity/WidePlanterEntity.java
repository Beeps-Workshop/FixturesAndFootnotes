package com.beepsterr.fixfoot.entity;

import com.beepsterr.fixfoot.registry.ModEntities;
import com.beepsterr.fixfoot.registry.ModItems;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import com.beepsterr.fixfoot.compat.Interactions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A wall-mounted trough that displays three plants side by side. Right-click one third of it with a
 * flower, sapling or crop to plant it there; right-click a filled third to take it back.
 *
 * <p>Slot 0 is on the left as you face the planter. {@link #slotFor} and the renderer share
 * {@link #slotOffsetX}, so where you click and where the plant appears can't drift apart.
 *
 * <p>The slots live in {@link SynchedEntityData}, so the client gets them for free — the renderer
 * reads exactly what an interaction writes, with no extra packets.
 */
public class WidePlanterEntity extends AttachedFixtureEntity {
    public static final int SLOTS = 3;

    /** Across the wall, tall, and standing out of the wall — in blocks. */
    public static final float WIDTH = 1.0F;
    public static final float HEIGHT = 0.5F;
    public static final float DEPTH = 0.5F;

    /** How far off the block floor the trough hangs, so it reads as a window box. */
    private static final double BOTTOM = 0.125D;

    private static final EntityDataAccessor<ItemStack> PLANT_0 =
            SynchedEntityData.defineId(WidePlanterEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PLANT_1 =
            SynchedEntityData.defineId(WidePlanterEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PLANT_2 =
            SynchedEntityData.defineId(WidePlanterEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final List<EntityDataAccessor<ItemStack>> PLANTS = List.of(PLANT_0, PLANT_1, PLANT_2);

    public WidePlanterEntity(EntityType<? extends WidePlanterEntity> type, Level level) {
        super(type, level);
    }

    public WidePlanterEntity(Level level, BlockPos pos, Direction facing) {
        super(ModEntities.WIDE_PLANTER.get(), level, pos, facing);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No super call — Entity declares this abstract and HangingEntity never implements it.
        for (EntityDataAccessor<ItemStack> plant : PLANTS) {
            builder.define(plant, ItemStack.EMPTY);
        }
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos attachPos, Direction facing) {
        AABB box = faceHuggingBox(attachPos, facing, WIDTH, HEIGHT, DEPTH);
        if (!facing.getAxis().isHorizontal()) {
            return box;
        }
        // faceHuggingBox centres the box in the block; drop it so the trough hangs low on the wall.
        return box.move(0.0D, BOTTOM - (0.5D - HEIGHT / 2.0D), 0.0D);
    }

    @Override
    public ItemStack getFixtureItem() {
        return new ItemStack(ModItems.WIDE_PLANTER.get());
    }

    // --- Contents ---

    public ItemStack getPlant(int slot) {
        return entityData.get(PLANTS.get(slot));
    }

    public void setPlant(int slot, ItemStack stack) {
        entityData.set(PLANTS.get(slot), stack);
    }

    /** Flowers, saplings, crops, and the odd plants that fit none of those categories. */
    public static boolean isPlantable(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        //? if >=1.21.2 {
        /*if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.SMALL_FLOWERS)) {
        *///?} else {
        if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.FLOWERS)) {
        //?}
            return true;
        }
        // Seeds, carrots and potatoes are all ItemNameBlockItems pointing at their planted block.
        Block block = blockItem.getBlock();
        return block instanceof CropBlock
                || block instanceof StemBlock
                // Vanilla gives these no shared supertype or tag, but they all read as something
                // you'd grow in a trough. Matching on class keeps modded equivalents working.
                || block instanceof CactusBlock
                || block instanceof BambooStalkBlock
                || block instanceof BambooSaplingBlock
                || block instanceof SeaPickleBlock
                || block instanceof SweetBerryBushBlock;
    }

    /**
     * The state drawn for a slot. Anything that grows is shown at full size — the planter is
     * decorative, so a slot holding wheat seeds should look like wheat rather than a bare sprout.
     */
    public static BlockState displayState(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return Blocks.AIR.defaultBlockState();
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        // Matching the property by name rather than the block by class ripens crops, sweet berries
        // and any modded plant that follows the same convention, in one branch.
        if (block.getStateDefinition().getProperty("age") instanceof IntegerProperty age) {
            state = state.setValue(age, Collections.max(age.getPossibleValues()));
        }
        // A bare stalk is the bamboo default; give it foliage so it reads as a plant.
        if (block instanceof BambooStalkBlock) {
            state = state.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE);
        }
        return state;
    }

    /**
     * The top half of a two-block plant such as a sunflower or rose bush, or air for everything
     * else. Without this the renderer draws only the lower half and a sunflower is a bare stem.
     */
    public static BlockState upperHalf(BlockState state) {
        if (state.hasProperty(DoublePlantBlock.HALF)) {
            return state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void dropContents() {
        for (int slot = 0; slot < SLOTS; slot++) {
            dropStack(getPlant(slot));
            setPlant(slot, ItemStack.EMPTY);
        }
    }

    // --- Interaction ---

    /** Centre of a slot along the planter's own left-to-right axis, relative to the block centre. */
    public static double slotOffsetX(int slot) {
        return (slot + 0.5D) / SLOTS - 0.5D;
    }

    /**
     * Which third of the trough a click landed in. {@code hitVec} is relative to the entity origin,
     * so project it onto the planter's left-to-right axis — {@code direction.getClockWise()}, the
     * same axis the renderer's Y rotation produces.
     */
    private int slotFor(Vec3 hitVec) {
        if (!direction.getAxis().isHorizontal()) {
            // canAttachTo keeps the planter off floors and ceilings, so this is only reachable on a
            // /summon'd entity. Bail out rather than let getClockWise() throw on the vertical axis.
            return 0;
        }
        Direction right = direction.getClockWise();
        double along = hitVec.x * right.getStepX() + hitVec.z * right.getStepZ();
        return Mth.clamp((int) ((along / WIDTH + 0.5D) * SLOTS), 0, SLOTS - 1);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitVec, InteractionHand hand) {
        int slot = slotFor(hitVec);
        ItemStack planted = getPlant(slot);

        if (!planted.isEmpty()) {
            if (!level().isClientSide) {
                player.getInventory().placeItemBackInInventory(planted);
                setPlant(slot, ItemStack.EMPTY);
                playSound(SoundEvents.CROP_BREAK, 1.0F, 1.0F);
            }
            return Interactions.sided(level());
        }

        ItemStack held = player.getItemInHand(hand);
        if (isPlantable(held)) {
            if (!level().isClientSide) {
                setPlant(slot, held.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(SoundEvents.CROP_PLANTED, 1.0F, 1.0F);
            }
            return Interactions.sided(level());
        }

        return InteractionResult.PASS;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag plants = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            plants.add(getPlant(slot).saveOptional(registryAccess()));
        }
        tag.put("Plants", plants);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag plants = tag.getList("Plants", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < SLOTS; slot++) {
            setPlant(slot, slot < plants.size()
                    ? ItemStack.parseOptional(registryAccess(), plants.getCompound(slot))
                    : ItemStack.EMPTY);
        }
    }
}
