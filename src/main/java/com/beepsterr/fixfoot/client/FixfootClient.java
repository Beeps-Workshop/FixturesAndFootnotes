package com.beepsterr.fixfoot.client;

import com.beepsterr.fixfoot.client.render.DisplayFixtureRenderer;
import com.beepsterr.fixfoot.content.SlatVariants;
import com.beepsterr.fixfoot.registry.ModBlockEntities;
import com.beepsterr.fixfoot.registry.ModBlocks;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;

/**
 * Loader-agnostic client init, split by the window each registration needs:
 * {@link #initClient()} for colors + render types (general client setup), and
 * {@link #initRenderers()} for block-entity renderers (NeoForge's EntityRenderersEvent window).
 */
public final class FixfootClient {
    private FixfootClient() {
    }

    public static void initClient() {
        // Biome-tint the vine overlay (tintindex 0) green like vanilla vines; wood faces have no
        // tintindex so they keep their normal colour.
        BlockColor foliage = (state, level, pos, tintIndex) ->
                (level != null && pos != null) ? BiomeColors.getAverageFoliageColor(level, pos) : -1;
        for (String key : SlatVariants.KEYS) {
            Block wall = ModBlocks.WALL.get(key).get();
            RenderTypeRegistry.register(RenderType.cutout(), wall); // cutout so the vine shows through
            ColorHandlerRegistry.registerBlockColors(foliage, wall);
        }
    }

    public static void initRenderers() {
        BlockEntityRendererRegistry.register(ModBlockEntities.DISPLAY_FIXTURE.get(), DisplayFixtureRenderer::new);
    }
}
