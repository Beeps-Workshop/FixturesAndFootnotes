package com.beepsterr.fixfoot.client;

import com.beepsterr.fixfoot.client.render.DisplayFixtureRenderer;
import com.beepsterr.fixfoot.registry.ModBlockEntities;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

/**
 * Loader-agnostic client init. Architectury's {@link BlockEntityRendererRegistry} defers to
 * each platform's renderer-registration event, so this one call wires the BER on both loaders.
 * Invoked from the Fabric client entrypoint and the NeoForge client-side mod init.
 */
public final class FixfootClient {
    private FixfootClient() {
    }

    public static void init() {
        BlockEntityRendererRegistry.register(ModBlockEntities.DISPLAY_FIXTURE.get(), DisplayFixtureRenderer::new);
    }
}
