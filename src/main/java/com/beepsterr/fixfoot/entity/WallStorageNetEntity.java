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
 * A 2x1 net slung along a wall, holding a double chest.
 *
 * <p>It hangs from a single block and overhangs half a block to either side, rather than claiming
 * two blocks the way a bed or a painting does. That keeps one attachment block — no half/part state,
 * no multi-block bookkeeping — and lets a row of them line up on block boundaries. The trade is that
 * only the block it hangs on needs to be solid, so a net can overhang the end of a wall.
 */
public class WallStorageNetEntity extends StorageNetEntity {
    /** A double chest. Vanilla's six-row menu and screen handle it, so there's no custom GUI. */
    public static final int SLOTS = 54;

    /** Two blocks along the wall, and shallow enough to keep the tile in front walkable. */
    public static final float WIDTH = 2.0F;
    public static final float HEIGHT = 0.875F;
    public static final float DEPTH = 0.3125F;

    /** Resting on the pouch floor (model y=1, so local y=-0.4375), spread over both blocks. */
    private static final Vec3[] PREVIEW_LAYOUT = {
            new Vec3(-0.76D, -0.40D, 0.30D),
            new Vec3(-0.38D, -0.40D, 0.38D),
            new Vec3(0.00D, -0.40D, 0.28D),
            new Vec3(0.38D, -0.40D, 0.37D),
            new Vec3(0.76D, -0.40D, 0.31D),
    };

    public WallStorageNetEntity(EntityType<? extends WallStorageNetEntity> type, Level level) {
        super(type, level, SLOTS);
    }

    public WallStorageNetEntity(Level level, BlockPos pos, Direction facing) {
        super(ModEntities.STORAGE_NET.get(), level, pos, facing, SLOTS);
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos attachPos, Direction facing) {
        // WIDTH of 2 centres a two-block span on the attachment block, giving the half-block
        // overhang each side. faceHuggingBox needs no special case for it.
        return faceHuggingBox(attachPos, facing, WIDTH, HEIGHT, DEPTH);
    }

    /** Twice the width to fill, so it shows more of its contents than the small net does. */
    @Override
    public int previewCount() {
        return PREVIEW_LAYOUT.length;
    }

    @Override
    public Vec3 previewOffset(int index) {
        return PREVIEW_LAYOUT[index];
    }

    @Override
    public ItemStack getFixtureItem() {
        return new ItemStack(ModItems.STORAGE_NET.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return ChestMenu.sixRows(containerId, playerInventory, this);
    }
}
