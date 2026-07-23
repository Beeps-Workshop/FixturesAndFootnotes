package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.block.DisplayFixtureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Renders the fixture's stored item floating and slowly spinning above the block —
 * the same technique used for items on bmcrm's Infuser/Former.
 *
 * <p>VERSION SEAM: the item-render call ({@code ItemRenderer#renderStatic} + PoseStack) is the
 * pre-1.21.5 rendering path, valid for 1.21.1 and 1.21.4. The 26.x branches use the reworked
 * render pipeline and will need a separate implementation behind a Stonecutter conditional.
 */
public class DisplayFixtureRenderer implements BlockEntityRenderer<DisplayFixtureBlockEntity> {

    public DisplayFixtureRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DisplayFixtureBlockEntity fixture, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stack = fixture.getDisplayedItem();
        if (stack.isEmpty()) {
            return;
        }

        Level level = fixture.getLevel();
        long gameTime = level != null ? level.getGameTime() : 0L;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.05D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees((gameTime + partialTick) * 2.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffers,
                level,
                0);

        poseStack.popPose();
    }
}
