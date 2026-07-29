package com.beepsterr.fixfoot.fabric.client;

import com.beepsterr.fixfoot.client.FixfootClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import java.util.function.Function;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public final class FixfootFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FixfootClient.initParticles(new FixfootClient.ParticleSink() {
            @Override
            public <T extends ParticleOptions> void register(ParticleType<T> type,
                                                             Function<SpriteSet, ParticleProvider<T>> provider) {
                ParticleFactoryRegistry.getInstance().register(type, provider::apply);
            }
        });
        FixfootClient.initClient();
        FixfootClient.initEntityRenderers(EntityRendererRegistry::register);
    }
}
