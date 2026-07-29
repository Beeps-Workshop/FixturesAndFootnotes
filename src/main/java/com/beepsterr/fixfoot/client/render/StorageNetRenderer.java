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
    protected void extractContents(StorageNetEntity net, FixtureRenderState into) {
        for (int slot = 0; slot < net.previewCount(); slot++) {
            ItemStack stack = net.getPreview(slot);
            into.previews.add(stack);
            into.previewOffsets.add(net.previewOffset(slot));
            //? if >=1.21.2 {
            /*net.minecraft.client.renderer.item.ItemStackRenderState resolved =
                    new net.minecraft.client.renderer.item.ItemStackRenderState();
            if (!stack.isEmpty()) {
                itemModels.updateForNonLiving(resolved, stack, ItemDisplayContext.GROUND, net);
            }
            into.previewModels.add(resolved);
            *///?}
        }
    }

    @Override
    protected void renderContents(FixtureRenderState from, PoseStack poseStack,
                                  MultiBufferSource buffers, int packedLight) {
        for (int slot = 0; slot < from.previews.size(); slot++) {
            if (from.previews.get(slot).isEmpty()) {
                continue;
            }
            Vec3 at = from.previewOffsets.get(slot);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            // Tip the sprite flat so it lies in the net like a dropped item, then spin it in that
            // plane — which is the local Z axis once the item is on its back.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(SPIN[slot % SPIN.length]));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            //? if >=1.21.2 {
            /*from.previewModels.get(slot).render(poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
            *///?} else {
            itemRenderer.renderStatic(from.previews.get(slot), ItemDisplayContext.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, buffers, from.level, 0);
            //?}
            poseStack.popPose();
        }
    }
}
