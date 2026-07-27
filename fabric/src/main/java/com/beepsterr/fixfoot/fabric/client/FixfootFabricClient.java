package com.beepsterr.fixfoot.fabric.client;

import com.beepsterr.fixfoot.client.FixfootClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class FixfootFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FixfootClient.initClient();
        FixfootClient.initEntityRenderers(EntityRendererRegistry::register);
    }
}
