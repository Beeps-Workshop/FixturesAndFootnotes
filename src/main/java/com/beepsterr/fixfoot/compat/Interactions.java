package com.beepsterr.fixfoot.compat;

import net.minecraft.world.InteractionResult;
//? if <1.21.2 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.level.Level;

/**
 * The results an interaction can return, which changed shape in 1.21.2.
 *
 * <p>Two things moved at once. {@code ItemInteractionResult} — the type a block's {@code useItemOn}
 * returned — was folded back into {@link InteractionResult}, and {@code sidedSuccess(boolean)} gave
 * way to constants that already know which side they are on. Both are mechanical, but they appear
 * all over the blocks and fixtures, so the seam is kept here and everything else calls through.
 *
 * <p>The only thing callers still have to spell out per version is the return type on a
 * {@code useItemOn} signature, since Java has no way to alias it.
 */
public final class Interactions {
    private Interactions() {
    }

    /** Handled, on whichever side is asking. For anything returning a plain InteractionResult. */
    public static InteractionResult sided(Level level) {
        //? if >=1.21.2 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide);
        //?}
    }

    //? if >=1.21.2 {
    /*public static InteractionResult used(Level level) {
        return InteractionResult.SUCCESS;
    }

    // Not ours — let the block's own click behaviour have it.
    public static InteractionResult passToBlock() {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    *///?} else {

    /** Handled by the item in hand. */
    public static ItemInteractionResult used(Level level) {
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Not ours — let the block's own click behaviour have it. */
    public static ItemInteractionResult passToBlock() {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    //?}
}
