package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.WidePlanterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adds the three planted blocks on top of the trough drawn by {@link AttachedFixtureRenderer}.
 *
 * <p>No version conditionals here: the plants are read off the entity in the extract pass and drawn
 * from the state in the draw pass, which is the same on both versions, and renderSingleBlock did
 * not change.
 */
public class WidePlanterRenderer extends AttachedFixtureRenderer<WidePlanterEntity> {
    /** Local-space slot centre: soil top, and the middle of the trough front-to-back. */
    private static final double PLANT_Y = 0.0D;
    private static final double PLANT_Z = 0.25D;
    private static final float PLANT_SCALE = 0.5F;

    private final BlockRenderDispatcher blockRenderer;

    public WidePlanterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    protected void extractContents(WidePlanterEntity planter, FixtureRenderState into) {
        for (int slot = 0; slot < WidePlanterEntity.SLOTS; slot++) {
            BlockState plant = WidePlanterEntity.displayState(planter.getPlant(slot));
            into.plants.add(plant);
            // Sunflowers and rose bushes are two separate block states; drawing only the lower one
            // leaves a bare stem.
            into.plantTops.add(plant.isAir() ? plant : WidePlanterEntity.upperHalf(plant));
        }
    }

    @Override
    protected void renderContents(FixtureRenderState from, PoseStack poseStack,
                                  MultiBufferSource buffers, int packedLight) {
        for (int slot = 0; slot < from.plants.size(); slot++) {
            BlockState plant = from.plants.get(slot);
            if (plant.isAir()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(WidePlanterEntity.slotOffsetX(slot), PLANT_Y, PLANT_Z);
            poseStack.scale(PLANT_SCALE, PLANT_SCALE, PLANT_SCALE);
            // renderSingleBlock draws from the origin outwards, so recentre it on the slot.
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            blockRenderer.renderSingleBlock(plant, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);

            // The translate lands inside the scale, so it's exactly one block up.
            BlockState upper = from.plantTops.get(slot);
            if (!upper.isAir()) {
                poseStack.translate(0.0D, 1.0D, 0.0D);
                blockRenderer.renderSingleBlock(upper, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
    }
}
