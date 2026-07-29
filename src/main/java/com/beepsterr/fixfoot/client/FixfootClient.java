package com.beepsterr.fixfoot.client;

import com.beepsterr.fixfoot.client.render.StorageNetRenderer;
import com.beepsterr.fixfoot.client.render.WidePlanterRenderer;
import com.beepsterr.fixfoot.content.SlatVariants;
import com.beepsterr.fixfoot.registry.ModBlocks;
import com.beepsterr.fixfoot.client.particle.SpoutWaterParticle;
import com.beepsterr.fixfoot.registry.ModEntities;
import com.beepsterr.fixfoot.registry.ModParticles;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.Function;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
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
    /** Bridges Fabric's ParticleFactoryRegistry and NeoForge's RegisterParticleProviders event. */
    @FunctionalInterface
    public interface ParticleSink {
        /**
         * Takes a plain {@link Function} rather than vanilla's own sprite-registration interface,
         * which is private in the compiled class however the decompiled source reads — the same
         * trap {@code DripParticle}'s constructor sets. Each loader adapts it to its own.
         */
        <T extends ParticleOptions> void register(ParticleType<T> type,
                                                  Function<SpriteSet, ParticleProvider<T>> provider);
    }

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

        // Same deal for the basin's water surface (tintindex 0 on the content quad), tinted per
        // biome like a cauldron's. The copper itself is untinted, so it needs no index.
        BlockColor water = (state, level, pos, tintIndex) ->
                (level != null && pos != null) ? BiomeColors.getAverageWaterColor(level, pos) : -1;
        for (RegistrySupplier<Block> basin : ModBlocks.COPPER.values()) {
            ColorHandlerRegistry.registerBlockColors(water, basin.get());
        }
    }

    /**
     * Particle providers.
     *
     * <p>Takes a sink for the same reason the entity renderers do: each loader has its own window
     * for this, and Architectury's wrapper warns in its own docs that it misregisters on the Forge
     * side unless it is called before client setup. Going direct is a few lines either way and
     * leaves nothing to guess about.
     */
    public static void initParticles(ParticleSink sink) {
        sink.register(ModParticles.SPOUT_WATER.get(), SpoutWaterParticle.Provider::new);
    }

    public static void initEntityRenderers(EntityRendererSink sink) {
        sink.register(ModEntities.WIDE_PLANTER.get(), WidePlanterRenderer::new);
        // One renderer serves every net; the preview layout comes from the entity.
        sink.register(ModEntities.SMALL_STORAGE_NET.get(), StorageNetRenderer::new);
        sink.register(ModEntities.STORAGE_NET.get(), StorageNetRenderer::new);
    }
}
