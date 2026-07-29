package com.beepsterr.fixfoot.entity;

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
import com.beepsterr.fixfoot.compat.Interactions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared behaviour for the storage nets — chests you hang off the building rather than stand on the
 * floor. Subclasses pick a size, a face to hang on, and which vanilla chest menu to open.
 *
 * <p>Slots are a plain list and reach the client through the open menu like any chest. Separately, a
 * short synched summary of the first {@link #previewCount()} stacks is what the renderer rests in
 * the mesh, so a stocked net reads as full at a glance without syncing an inventory per change.
 */
public abstract class StorageNetEntity extends AttachedFixtureEntity implements Container, MenuProvider {
    /** Ceiling on synched preview stacks. Each net shows {@link #previewCount()} of them. */
    public static final int MAX_PREVIEW = 5;

    private static final EntityDataAccessor<ItemStack> PREVIEW_0 =
            SynchedEntityData.defineId(StorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_1 =
            SynchedEntityData.defineId(StorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_2 =
            SynchedEntityData.defineId(StorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_3 =
            SynchedEntityData.defineId(StorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> PREVIEW_4 =
            SynchedEntityData.defineId(StorageNetEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final List<EntityDataAccessor<ItemStack>> PREVIEW =
            List.of(PREVIEW_0, PREVIEW_1, PREVIEW_2, PREVIEW_3, PREVIEW_4);

    private final NonNullList<ItemStack> items;

    protected StorageNetEntity(EntityType<? extends StorageNetEntity> type, Level level, int slots) {
        super(type, level);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
    }

    protected StorageNetEntity(EntityType<? extends StorageNetEntity> type, Level level,
                               BlockPos pos, Direction facing, int slots) {
        super(type, level, pos, facing);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No super call — Entity declares this abstract.
        for (EntityDataAccessor<ItemStack> slot : PREVIEW) {
            builder.define(slot, ItemStack.EMPTY);
        }
    }

    // --- Preview ---

    /** How many stacks this net shows resting in it, at most {@link #MAX_PREVIEW}. */
    public int previewCount() {
        return 3;
    }

    /** Where preview stack {@code index} rests, in the renderer's local model space. */
    public abstract Vec3 previewOffset(int index);

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
        int limit = Math.min(previewCount(), MAX_PREVIEW);
        for (int slot = 0; slot < items.size() && shown < limit; slot++) {
            ItemStack stored = items.get(slot);
            if (!stored.isEmpty()) {
                // Count is irrelevant to the render and would resync on every insert.
                entityData.set(PREVIEW.get(shown++), stored.copyWithCount(1));
            }
        }
        while (shown < MAX_PREVIEW) {
            entityData.set(PREVIEW.get(shown++), ItemStack.EMPTY);
        }
    }

    // --- Interaction ---

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            player.openMenu(this);
        }
        return Interactions.sided(level());
    }

    @Override
    public Component getDisplayName() {
        return getType().getDescription();
    }

    @Override
    protected void dropContents() {
        for (ItemStack stored : items) {
            dropStack(stored);
        }
        items.clear();
        updatePreview();
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return items.size();
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
