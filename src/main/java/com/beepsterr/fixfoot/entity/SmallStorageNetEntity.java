package com.beepsterr.fixfoot.entity;

import com.beepsterr.fixfoot.registry.ModEntities;
import com.beepsterr.fixfoot.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A ceiling-hung net holding a single chest's worth of items. Borrowed from the Avali of Starbound,
 * where storage hangs off the architecture rather than standing on it — so the net is deliberately
 * shallow, leaving the tile below walkable while still being reachable.
 *
 * <p>The 27 slots are a plain list — they reach the client through the open menu like any chest.
 * Only a short {@link #PREVIEW_SLOTS} summary is synched, which is what the renderer rests in the
 * net, so a stocked net reads as full at a glance without syncing a whole inventory per change.
 */
public class SmallStorageNetEntity extends AttachedFixtureEntity implements Container, MenuProvider {
    /** A single chest. Vanilla's three-row menu and screen handle it, so there's no custom GUI. */
    public static final int SLOTS = 27;

    /** Across the ceiling, and how far it sags below it — shallow enough to keep headroom. */
    public static final float WIDTH = 0.875F;
    public static final float HANG = 0.375F;

    /** How many of the stored stacks are shown resting in the net. */
    public static final int PREVIEW_SLOTS = 3;

    private static final EntityDataAccessor<ItemStack> PREVIEW_0 =
            SynchedEntityData.defineId(SmallStorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_1 =
            SynchedEntityData.defineId(SmallStorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_2 =
            SynchedEntityData.defineId(SmallStorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final List<EntityDataAccessor<ItemStack>> PREVIEW =
            List.of(PREVIEW_0, PREVIEW_1, PREVIEW_2);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public SmallStorageNetEntity(EntityType<? extends SmallStorageNetEntity> type, Level level) {
        super(type, level);
    }

    public SmallStorageNetEntity(Level level, BlockPos pos, Direction facing) {
        super(ModEntities.SMALL_STORAGE_NET.get(), level, pos, facing);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No super call — Entity declares this abstract. Only the preview is synched: the full 27
        // slots reach the client through the open menu, and nothing draws them.
        for (EntityDataAccessor<ItemStack> slot : PREVIEW) {
            builder.define(slot, ItemStack.EMPTY);
        }
    }

    /** A stack resting in the net, or empty. Client-safe — this is the synched summary. */
    public ItemStack getPreview(int index) {
        return entityData.get(PREVIEW.get(index));
    }

    /** Re-summarises the first few stored stacks for the client to draw. */
    private void updatePreview() {
        if (level().isClientSide) {
            return;
        }
        int shown = 0;
        for (int slot = 0; slot < SLOTS && shown < PREVIEW_SLOTS; slot++) {
            ItemStack stored = items.get(slot);
            if (!stored.isEmpty()) {
                // Count is irrelevant to the render and would resync on every insert.
                entityData.set(PREVIEW.get(shown++), stored.copyWithCount(1));
            }
        }
        while (shown < PREVIEW_SLOTS) {
            entityData.set(PREVIEW.get(shown++), ItemStack.EMPTY);
        }
    }

    /** Ceiling only: the support is the block above, so the face pointing away from it is DOWN. */
    @Override
    public boolean canAttachTo(Direction facing) {
        return facing == Direction.DOWN;
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos attachPos, Direction facing) {
        return faceHuggingBox(attachPos, facing, WIDTH, WIDTH, HANG);
    }

    @Override
    public ItemStack getFixtureItem() {
        return new ItemStack(ModItems.SMALL_STORAGE_NET.get());
    }

    @Override
    protected void dropContents() {
        for (int slot = 0; slot < SLOTS; slot++) {
            dropStack(items.get(slot));
        }
        items.clear();
        updatePreview();
    }

    // --- Interaction ---

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            player.openMenu(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public Component getDisplayName() {
        return getType().getDescription();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.canInteractWithEntity(this, 4.0D);
    }

    @Override
    public void clearContent() {
        items.clear();
        updatePreview();
    }

    /** The menu calls this whenever a slot changes, which is where the preview is refreshed from. */
    @Override
    public void setChanged() {
        updatePreview();
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ContainerHelper.saveAllItems(tag, items, registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // loadAllItems only writes the slots present in the tag, so clear first or a reload would
        // keep whatever the freshly-constructed list happened to hold.
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registryAccess());
        updatePreview();
    }
}
