package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.SmallStorageNetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Rests the net's synched preview stacks in the bottom of the mesh drawn by the base renderer. */
public class SmallStorageNetRenderer extends AttachedFixtureRenderer<SmallStorageNetEntity> {
    /** Just above the net floor (model y=10, so local y=0.125). */
    private static final double ITEM_Y = 0.14D;
    private static final float ITEM_SCALE = 0.75F;

    // Scattered by hand rather than jittered at runtime: a random offset would reshuffle every
    // frame, and the layout has to be stable across client and server anyway.
    private static final double[] OFFSET_X = {-0.20D, 0.05D, 0.22D};
    private static final double[] OFFSET_Z = {-0.12D, 0.14D, -0.05D};
    private static final float[] SPIN = {20.0F, -35.0F, 60.0F};

    public SmallStorageNetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderContents(SmallStorageNetEntity net, PoseStack poseStack,
                                  MultiBufferSource buffers, int packedLight) {
        for (int slot = 0; slot < SmallStorageNetEntity.PREVIEW_SLOTS; slot++) {
            ItemStack stack = net.getPreview(slot);
            if (stack.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(OFFSET_X[slot], ITEM_Y, OFFSET_Z[slot]);
            // Tip the sprite flat so it lies in the net like a dropped item, then spin it in that
            // plane — which is the local Z axis once the item is on its back.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(SPIN[slot]));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, buffers, net.level(), 0);
            poseStack.popPose();
        }
    }
}
