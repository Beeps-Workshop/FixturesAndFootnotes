package com.beepsterr.fixfoot.entity;

import com.beepsterr.fixfoot.registry.ModEntities;
import com.beepsterr.fixfoot.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A 1x1 net hung from a ceiling, holding a single chest. It sags only {@link #HANG}, leaving the
 * tile below walkable so the storage reads as part of the ceiling rather than an object in the room.
 */
public class SmallStorageNetEntity extends StorageNetEntity {
    /** A single chest. Vanilla's three-row menu and screen handle it, so there's no custom GUI. */
    public static final int SLOTS = 27;

    /** Across the ceiling, and how far it sags below it — shallow enough to keep headroom. */
    public static final float WIDTH = 0.875F;
    public static final float HANG = 0.375F;

    /** Just above the net floor (model y=10, so local y=0.125), scattered by hand. */
    private static final Vec3[] PREVIEW_LAYOUT = {
            new Vec3(-0.20D, 0.14D, -0.12D),
            new Vec3(0.05D, 0.14D, 0.14D),
            new Vec3(0.22D, 0.14D, -0.05D),
    };

    public SmallStorageNetEntity(EntityType<? extends SmallStorageNetEntity> type, Level level) {
        super(type, level, SLOTS);
    }

    public SmallStorageNetEntity(Level level, BlockPos pos, Direction facing) {
        super(ModEntities.SMALL_STORAGE_NET.get(), level, pos, facing, SLOTS);
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
    public Vec3 previewOffset(int index) {
        return PREVIEW_LAYOUT[index];
    }

    @Override
    public ItemStack getFixtureItem() {
        return new ItemStack(ModItems.SMALL_STORAGE_NET.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.threeRows(containerId, playerInventory, this);
    }
}
