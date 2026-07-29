package com.beepsterr.fixfoot.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Everything the fixture renderers need out of an entity, pulled off it before drawing.
 *
 * <p>1.21.2 split rendering into an extract pass and a draw pass, so a renderer no longer sees the
 * entity while drawing. This is that carrier — and on 1.21.1, where no such split exists, the same
 * object is filled in and drawn from immediately, which keeps the drawing code itself identical
 * across both versions.
 *
 * <p>One type serves every fixture rather than one per subclass. That is what lets
 * {@link AttachedFixtureRenderer} keep a single type parameter on both versions, so the planter and
 * net renderers extend it the same way regardless — at the cost of a few fields being of interest
 * to only one of them.
 */
public class FixtureRenderState
        //? if >=1.21.2 {
        /*extends net.minecraft.client.renderer.entity.state.EntityRenderState
        *///?}
{
    /** Offset from the entity origin to the model anchor, so art follows a pulled-in fixture. */
    public Vec3 toAnchor = Vec3.ZERO;
    public float yRot;

    /** The fixture's own item, drawn as the body. */
    public ItemStack body = ItemStack.EMPTY;

    /** Where the fixture is, for the item model lookups that take a level. Nullable. */
    public net.minecraft.world.level.Level level;

    /** Planter: the plant in each slot, plus the upper half of any two-block flower. */
    public final List<BlockState> plants = new ArrayList<>();
    public final List<BlockState> plantTops = new ArrayList<>();

    /** Nets: the synched preview stacks and where each rests in the mesh. */
    public final List<ItemStack> previews = new ArrayList<>();
    public final List<Vec3> previewOffsets = new ArrayList<>();

    //? if >=1.21.2 {
    /*/^* Resolved models. On 1.21.2+ an item is drawn through a prepared state, not a baked model. ^/
    public final net.minecraft.client.renderer.item.ItemStackRenderState bodyModel =
            new net.minecraft.client.renderer.item.ItemStackRenderState();
    public final List<net.minecraft.client.renderer.item.ItemStackRenderState> previewModels =
            new ArrayList<>();
    *///?}

    /** Cleared and refilled each frame; the renderer reuses one instance per entity. */
    public void clearContents() {
        plants.clear();
        plantTops.clear();
        previews.clear();
        previewOffsets.clear();
        //? if >=1.21.2 {
        /*previewModels.clear();
        *///?}
    }
}
