package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.StorageNetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Rests a net's synched preview stacks in the mesh drawn by the base renderer. Serves every net —
 * the layout comes from the entity, which is where the dimensions live.
 */
public class StorageNetRenderer extends AttachedFixtureRenderer<StorageNetEntity> {
    private static final float ITEM_SCALE = 0.75F;

    // Fixed rather than jittered at runtime: a random spin would reshuffle every frame.
    private static final float[] SPIN = {20.0F, -35.0F, 60.0F, -15.0F, 45.0F};

    public StorageNetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderContents(StorageNetEntity net, PoseStack poseStack,
                                  MultiBufferSource buffers, int packedLight) {
        for (int slot = 0; slot < net.previewCount(); slot++) {
            ItemStack stack = net.getPreview(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Vec3 at = net.previewOffset(slot);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            // Tip the sprite flat so it lies in the net like a dropped item, then spin it in that
            // plane — which is the local Z axis once the item is on its back.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(SPIN[slot % SPIN.length]));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, buffers, net.level(), 0);
            poseStack.popPose();
        }
    }
}
