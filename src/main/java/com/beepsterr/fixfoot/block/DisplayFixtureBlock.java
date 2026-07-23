package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A pedestal that displays one item, rendered dynamically by
 * {@link com.beepsterr.fixfoot.client.render.DisplayFixtureRenderer}. Right-click with an
 * item to place it; right-click empty-handed to take it back. This is the minimal version
 * of the "flower pot, but dynamic" idea — a block entity holding a stack + a BER drawing it.
 */
public class DisplayFixtureBlock extends BaseEntityBlock {
    public static final MapCodec<DisplayFixtureBlock> CODEC = simpleCodec(DisplayFixtureBlock::new);

    public DisplayFixtureBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Draw the base model (BaseEntityBlock defaults to INVISIBLE).
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayFixtureBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DisplayFixtureBlockEntity fixture)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack displayed = fixture.getDisplayedItem();
        if (displayed.isEmpty() && !stack.isEmpty()) {
            if (!level.isClientSide) {
                fixture.setDisplayedItem(stack.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!displayed.isEmpty()) {
            if (!level.isClientSide) {
                player.getInventory().placeItemBackInInventory(displayed);
                fixture.setDisplayedItem(ItemStack.EMPTY);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
