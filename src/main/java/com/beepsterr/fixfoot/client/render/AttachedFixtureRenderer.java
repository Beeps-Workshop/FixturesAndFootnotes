package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.AttachedFixtureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws any fixture's body as its own item model, so a fixture needs nothing but a Blockbench JSON
 * — no entity model layer, no texture registration. Fixtures with contents to draw on top override
 * {@link #renderContents}.
 *
 * <p><b>Model space.</b> Only the Y rotation is applied, which keeps authoring predictable:
 * <ul>
 *   <li>Wall fixtures are drawn with the support at local +Z, so you model them facing you with the
 *       wall behind at z=16.</li>
 *   <li>Ceiling and floor fixtures have a yaw of 0, so no rotation happens at all — model them
 *       upright in ordinary block space, hanging from y=16 or standing up from y=0.</li>
 * </ul>
 * Everything is drawn around {@link AttachedFixtureEntity#getModelAnchor()} rather than the entity
 * origin, so the art follows the box when a fixture is pulled in onto a partial support.
 *
 * <p>VERSION SEAM: this is the pre-1.21.2 renderer shape. 1.21.2+ splits EntityRenderer into
 * {@code EntityRenderer<T, S extends EntityRenderState>} with a {@code createRenderState()} /
 * {@code extractRenderState()} pair. Keeping the body draw here means that port is one class, not
 * one per fixture.
 */
public class AttachedFixtureRenderer<T extends AttachedFixtureEntity> extends EntityRenderer<T> {
    protected final ItemRenderer itemRenderer;

    /** One renderer exists per entity type and the item never varies, so build the stack once. */
    private ItemStack body = ItemStack.EMPTY;

    public AttachedFixtureRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(T fixture) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(T fixture, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();

        Vec3 toAnchor = fixture.getModelAnchor().subtract(fixture.position());
        poseStack.translate(toAnchor.x, toAnchor.y, toAnchor.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - fixture.getYRot()));

        if (body.isEmpty()) {
            body = fixture.getFixtureItem();
        }
        // ItemDisplayContext.NONE applies no display transform, so the model renders in raw block
        // space, centred on the origin.
        itemRenderer.renderStatic(body, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, fixture.level(), 0);
        renderContents(fixture, poseStack, buffers, packedLight);

        poseStack.popPose();
        super.render(fixture, yaw, partialTick, poseStack, buffers, packedLight);
    }

    /** Anything the fixture is holding, drawn in the same local space as the body. */
    protected void renderContents(T fixture, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
    }
}
