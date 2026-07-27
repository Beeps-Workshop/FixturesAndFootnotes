package com.beepsterr.fixfoot.client;

import com.beepsterr.fixfoot.client.render.StorageNetRenderer;
import com.beepsterr.fixfoot.client.render.WidePlanterRenderer;
import com.beepsterr.fixfoot.content.SlatVariants;
import com.beepsterr.fixfoot.registry.ModBlocks;
import com.beepsterr.fixfoot.registry.ModEntities;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * Loader-agnostic client init, split by the window each registration needs:
 * {@link #initClient()} for colors + render types (general client setup), and
 * {@link #initEntityRenderers(EntityRendererSink)} for entity renderers, which need NeoForge's
 * EntityRenderersEvent window.
 *
 * <p>Entity renderers take a sink because Architectury has no registry for them, so each loader
 * hands in its own registrar and the list still lives here rather than being duplicated per loader.
 */
public final class FixfootClient {
    /** Bridges Fabric's EntityRendererRegistry and NeoForge's RegisterRenderers event. */
    @FunctionalInterface
    public interface EntityRendererSink {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> renderer);
    }

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

    public static void initEntityRenderers(EntityRendererSink sink) {
        sink.register(ModEntities.WIDE_PLANTER.get(), WidePlanterRenderer::new);
        // One renderer serves every net; the preview layout comes from the entity.
        sink.register(ModEntities.SMALL_STORAGE_NET.get(), StorageNetRenderer::new);
        sink.register(ModEntities.STORAGE_NET.get(), StorageNetRenderer::new);
    }
}
