package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.WidePlanterEntity;
import com.beepsterr.fixfoot.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the planter body plus whatever is growing in its three slots.
 *
 * <p>The body is the planter item's own model, drawn with {@link ItemDisplayContext#NONE} so no
 * display transform is applied and the JSON renders in raw block space. That keeps the art pipeline
 * the same as every other block in the mod — one Blockbench model, no entity model layer.
 *
 * <p>VERSION SEAM: this is the pre-1.21.2 renderer shape. 1.21.2+ splits EntityRenderer into
 * {@code EntityRenderer<T, S extends EntityRenderState>} with a {@code createRenderState()} /
 * {@code extractRenderState()} pair, so the 1.21.4 branch needs its own version of this class.
 */
public class WidePlanterRenderer extends EntityRenderer<WidePlanterEntity> {
    /** Local-space slot centre: soil top, and the middle of the trough front-to-back. */
    private static final double PLANT_Y = 0.0D;
    private static final double PLANT_Z = 0.25D;
    private static final float PLANT_SCALE = 0.5F;

    private final ItemRenderer itemRenderer;
    private final BlockRenderDispatcher blockRenderer;
    private final ItemStack body;

    public WidePlanterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.body = new ItemStack(ModItems.WIDE_PLANTER.get());
    }

    @Override
    public ResourceLocation getTextureLocation(WidePlanterEntity planter) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(WidePlanterEntity planter, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();

        // The model is authored in ordinary block space, so draw it around the model anchor rather
        // than the entity origin, which sits down inside the trough. The anchor carries the same
        // pull-in as the hitbox, so on a slat panel the art stays on the box.
        Vec3 toAnchor = planter.getModelAnchor().subtract(planter.position());
        poseStack.translate(toAnchor.x, toAnchor.y, toAnchor.z);
        // Same rotation item frames use. Leaves local +Z pointing into the wall and local +X to the
        // viewer's right, which is the axis WidePlanterEntity#slotFor projects clicks onto.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - planter.getYRot()));

        itemRenderer.renderStatic(body, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, planter.level(), 0);

        for (int slot = 0; slot < WidePlanterEntity.SLOTS; slot++) {
            BlockState plant = WidePlanterEntity.displayState(planter.getPlant(slot));
            if (plant.isAir()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(WidePlanterEntity.slotOffsetX(slot), PLANT_Y, PLANT_Z);
            poseStack.scale(PLANT_SCALE, PLANT_SCALE, PLANT_SCALE);
            // renderSingleBlock draws from the origin outwards, so recentre it on the slot.
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            blockRenderer.renderSingleBlock(plant, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(planter, yaw, partialTick, poseStack, buffers, packedLight);
    }
}
