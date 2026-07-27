package com.beepsterr.fixfoot.neoforge;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.client.FixfootClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(Fixfoot.MOD_ID)
public final class FixfootNeoForge {
    public FixfootNeoForge(IEventBus modBus) {
        Fixfoot.init();

        // Client-only registration, each in its correct window (see FixfootClient). Guarded so the
        // client classes never load on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(FixfootClient::initClient));
            modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
                FixfootClient.initRenderers();
                FixfootClient.initEntityRenderers(event::registerEntityRenderer);
            });
        }
    }
}
