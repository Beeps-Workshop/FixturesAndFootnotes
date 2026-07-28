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
 * <p>This is the one following the Avali of Starbound, whose storage hangs off the walls so the
 * floor stays clear and walkable — so it wants to stay shallow, flush to the wall it hangs on.
 *
 * <p>It hangs from a single block and overhangs to either side, rather than claiming two blocks the
 * way a bed or a painting does. That keeps one attachment block — no half/part state, no multi-block
 * bookkeeping — and lets a row of them line up on block boundaries. The trade is that only the block
 * it hangs on needs to be solid, so a net can overhang the end of a wall.
 */
public class WallStorageNetEntity extends StorageNetEntity {
    /** A double chest. Vanilla's six-row menu and screen handle it, so there's no custom GUI. */
    public static final int SLOTS = 54;

    /**
     * Matched to the model: 26px along the wall (so it overhangs ~5px each side rather than a full
     * half block), 15px tall, and 8px of sag out from the wall.
     */
    public static final float WIDTH = 1.625F;
    public static final float HEIGHT = 0.9375F;
    public static final float DEPTH = 0.5F;

    /** The model sits at y 2..17, but faceHuggingBox centres vertically, so nudge it up to match. */
    private static final double VERTICAL_OFFSET = 0.09375D;

    /**
     * Resting on the bag's lowest webbing layer (model y=4, so local y=-0.25), pulled in far enough
     * to stay over that layer rather than the shallower ends, and sagging slightly toward the middle.
     */
    private static final Vec3[] PREVIEW_LAYOUT = {
            new Vec3(-0.45D, -0.20D, 0.24D),
            new Vec3(-0.23D, -0.23D, 0.28D),
            new Vec3(0.00D, -0.25D, 0.22D),
            new Vec3(0.23D, -0.23D, 0.27D),
            new Vec3(0.45D, -0.20D, 0.25D),
    };

    public WallStorageNetEntity(EntityType<? extends WallStorageNetEntity> type, Level level) {
        super(type, level, SLOTS);
    }

    public WallStorageNetEntity(Level level, BlockPos pos, Direction facing) {
        super(ModEntities.STORAGE_NET.get(), level, pos, facing, SLOTS);
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos attachPos, Direction facing) {
        // A WIDTH above 1 centres the span on the attachment block, giving the overhang each side;
        // faceHuggingBox needs no special case for it.
        return faceHuggingBox(attachPos, facing, WIDTH, HEIGHT, DEPTH).move(0.0D, VERTICAL_OFFSET, 0.0D);
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
