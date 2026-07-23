package com.beepsterr.fixfoot.fabric.client;

import com.beepsterr.fixfoot.client.FixfootClient;
import net.fabricmc.api.ClientModInitializer;

public final class FixfootFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FixfootClient.initClient();
        FixfootClient.initRenderers();
    }
}
