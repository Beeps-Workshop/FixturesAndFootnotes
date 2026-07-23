package com.beepsterr.fixfoot.client;

import com.beepsterr.fixfoot.client.render.DisplayFixtureRenderer;
import com.beepsterr.fixfoot.registry.ModBlockEntities;
import com.beepsterr.fixfoot.registry.ModBlocks;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;

/**
 * Loader-agnostic client init, split by the window each registration needs:
 * {@link #initClient()} for colors + render types (general client setup), and
 * {@link #initRenderers()} for block-entity renderers (NeoForge's EntityRenderersEvent window,
 * since Architectury registers those straight through to vanilla with no deferral).
 */
public final class FixfootClient {
    private FixfootClient() {
    }

    public static void initClient() {
        // Slats render in cutout so the vine overlay's transparency shows through.
        RenderTypeRegistry.register(RenderType.cutout(), ModBlocks.OAK_SLATS.get());
        // Biome-tint the vine overlay (tintindex 0) green, like vanilla vines. The wood bars have
        // no tintindex, so they stay their normal colour.
        ColorHandlerRegistry.registerBlockColors(
                (state, level, pos, tintIndex) ->
                        (level != null && pos != null) ? BiomeColors.getAverageFoliageColor(level, pos) : -1,
                ModBlocks.OAK_SLATS.get());
    }

    public static void initRenderers() {
        BlockEntityRendererRegistry.register(ModBlockEntities.DISPLAY_FIXTURE.get(), DisplayFixtureRenderer::new);
    }
}
