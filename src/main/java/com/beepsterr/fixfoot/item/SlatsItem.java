package com.beepsterr.fixfoot.item;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One item, two placements. Clicking a vertical wall face places the wall-hugging multiface slats
 * (this item's own block); placing against a floor/ceiling (a Y-axis face) places the standing
 * screen instead. Both blocks drop / pick-block back to this single item.
 */
public class SlatsItem extends BlockItem {
    private final Block screenBlock;

    public SlatsItem(Block wallBlock, Block screenBlock, Properties properties) {
        super(wallBlock, properties);
        this.screenBlock = screenBlock;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (context.getClickedFace().getAxis() == Direction.Axis.Y) {
            // Floor/ceiling face → standing screen.
            BlockState screenState = screenBlock.getStateForPlacement(context);
            return screenState != null && context.canPlace() ? screenState : null;
        }
        // Vertical wall face → wall-hugging multiface slats (this item's block).
        return super.getPlacementState(context);
    }
}
