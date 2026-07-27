package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.entity.WidePlanterEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.ENTITY_TYPE);

    // Fixtures never move, so they track like item frames: a short range and no periodic position
    // updates — the only thing worth resending is their synched contents.
    public static final RegistrySupplier<EntityType<WidePlanterEntity>> WIDE_PLANTER =
            ENTITY_TYPES.register("wide_planter", () -> EntityType.Builder
                    .<WidePlanterEntity>of(WidePlanterEntity::new, MobCategory.MISC)
                    .sized(WidePlanterEntity.WIDTH, WidePlanterEntity.HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("wide_planter"));

    private ModEntities() {
    }

    public static void register() {
        ENTITY_TYPES.register();
    }
}
