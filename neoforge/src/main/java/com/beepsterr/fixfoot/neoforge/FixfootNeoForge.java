package com.beepsterr.fixfoot.neoforge;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.client.FixfootClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(Fixfoot.MOD_ID)
public final class FixfootNeoForge {
    public FixfootNeoForge(IEventBus modBus) {
        Fixfoot.init();

        // Client-only: register the block-entity renderer during the renderer-registration
        // event. On NeoForge, Architectury's DeferredRegister doesn't flush until RegisterEvent
        // (AFTER mod construction), so calling FixfootClient.init() — which resolves
        // ModBlockEntities.DISPLAY_FIXTURE.get() — in the constructor is too early and NPEs.
        // EntityRenderersEvent.RegisterRenderers runs after registration and is the correct
        // window for BlockEntityRenderers.register. (Fabric's client entrypoint is already
        // late enough, so the shared FixfootClient.init() is just invoked at each loader's
        // appropriate time.)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> FixfootClient.init());
        }
    }
}
