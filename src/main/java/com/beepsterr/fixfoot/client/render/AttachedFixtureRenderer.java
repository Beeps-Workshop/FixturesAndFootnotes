package com.beepsterr.fixfoot.client.render;

import com.beepsterr.fixfoot.entity.AttachedFixtureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;
*///?} else {
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}

/**
 * Draws a fixture's body, and gives its subclasses a hook for whatever the fixture is holding.
 *
 * <p>Everything is drawn around {@link AttachedFixtureEntity#getModelAnchor()} rather than the
 * entity origin, so the art follows the box when a fixture is pulled in onto a partial support.
 *
 * <p>1.21.2 split rendering into an extract pass and a draw pass. Both versions funnel through the
 * same {@link FixtureRenderState}, so {@link #extractContents} and {@link #renderContents} — the
 * two hooks subclasses actually implement — have one shape across both, and only the plumbing
 * around them is conditional. That is also why the state type is shared rather than per subclass:
 * it keeps this class at one type parameter, so the subclasses extend it identically.
 */
public class AttachedFixtureRenderer<T extends AttachedFixtureEntity>
        //? if >=1.21.2 {
        /*extends EntityRenderer<T, FixtureRenderState> {
        *///?} else {
        extends EntityRenderer<T> {
        //?}

    //? if >=1.21.2 {
    /*protected final ItemModelResolver itemModels;
    *///?} else {
    protected final ItemRenderer itemRenderer;
    private final ModelBlockRenderer modelRenderer;
    //?}

    /** One renderer exists per entity type and the item never varies, so build the stack once. */
    private ItemStack body = ItemStack.EMPTY;

    public AttachedFixtureRenderer(EntityRendererProvider.Context context) {
        super(context);
        //? if >=1.21.2 {
        /*this.itemModels = context.getItemModelResolver();
        *///?} else {
        this.itemRenderer = context.getItemRenderer();
        this.modelRenderer = context.getBlockRenderDispatcher().getModelRenderer();
        //?}
    }

    // ---- version plumbing -------------------------------------------------------------------

    //? if >=1.21.2 {
    /*@Override
    public FixtureRenderState createRenderState() {
        return new FixtureRenderState();
    }

    @Override
    public void extractRenderState(T fixture, FixtureRenderState state, float partialTick) {
        super.extractRenderState(fixture, state, partialTick);
        extract(fixture, state);
        itemModels.updateForNonLiving(state.bodyModel, state.body, ItemDisplayContext.NONE, fixture);
    }

    @Override
    public void render(FixtureRenderState state, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        draw(state, poseStack, buffers, packedLight);
        super.render(state, poseStack, buffers, packedLight);
    }
    *///?} else {
    private final FixtureRenderState state = new FixtureRenderState();

    @Override
    public ResourceLocation getTextureLocation(T fixture) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(T fixture, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        // No extract pass before 1.21.2, so fill the same carrier here and draw straight from it.
        extract(fixture, state);
        draw(state, poseStack, buffers, packedLight);
        super.render(fixture, yaw, partialTick, poseStack, buffers, packedLight);
    }
    //?}

    // ---- shared -----------------------------------------------------------------------------

    /** Pulls the common placement off the entity, then hands over to the subclass. */
    private void extract(T fixture, FixtureRenderState into) {
        if (body.isEmpty()) {
            body = fixture.getFixtureItem();
        }
        into.clearContents();
        into.toAnchor = fixture.getModelAnchor().subtract(fixture.position());
        into.yRot = fixture.getYRot();
        into.body = body;
        into.level = fixture.level();
        extractContents(fixture, into);
    }

    private void draw(FixtureRenderState from, PoseStack poseStack, MultiBufferSource buffers,
                      int packedLight) {
        poseStack.pushPose();

        Vec3 toAnchor = from.toAnchor;
        poseStack.translate(toAnchor.x, toAnchor.y, toAnchor.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - from.yRot));

        poseStack.pushPose();
        poseStack.translate(-0.5D, -0.5D, -0.5D); // both paths draw from the origin outwards
        //? if >=1.21.2 {
        /*from.bodyModel.render(poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
        *///?} else {
        // Drawn through the model renderer rather than ItemRenderer#renderStatic so the render type
        // is ours to pick. Fixture items aren't BlockItems, so ItemBlockRenderTypes would fall
        // through to the translucent sheet, which depth-sorts per quad — fine for a solid trough,
        // but it makes the netting shimmer where you see one webbing face through another. The
        // webbing textures are binary alpha, so cutout is both correct and cheaper.
        modelRenderer.renderModel(poseStack.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), null,
                itemRenderer.getModel(from.body, from.level, null, 0),
                1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
        //?}
        poseStack.popPose();

        renderContents(from, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    /** Reads whatever the fixture is holding off the entity, for {@link #renderContents} to draw. */
    protected void extractContents(T fixture, FixtureRenderState into) {
    }

    /** Anything the fixture is holding, drawn in the same local space as the body. */
    protected void renderContents(FixtureRenderState from, PoseStack poseStack,
                                  MultiBufferSource buffers, int packedLight) {
    }
}
