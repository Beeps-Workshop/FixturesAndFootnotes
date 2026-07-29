package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.PARTICLE_TYPE);

    /**
     * Falling water that keeps the speed it is thrown with. Vanilla's own drip particle discards
     * the velocity it is handed in its constructor, so it can only ever fall straight down — no
     * use for water leaving a spout. The sprite is vanilla's, only the motion differs.
     */
    public static final RegistrySupplier<SimpleParticleType> SPOUT_WATER =
            PARTICLE_TYPES.register("spout_water", () -> new SimpleParticleType(false) {
            });

    private ModParticles() {
    }

    public static void register() {
        PARTICLE_TYPES.register();
    }
}
