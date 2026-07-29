package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.entity.SmallStorageNetEntity;
import com.beepsterr.fixfoot.entity.WallStorageNetEntity;
import com.beepsterr.fixfoot.entity.WidePlanterEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
                    .build(buildKey("wide_planter")));

    public static final RegistrySupplier<EntityType<SmallStorageNetEntity>> SMALL_STORAGE_NET =
            ENTITY_TYPES.register("small_storage_net", () -> EntityType.Builder
                    .<SmallStorageNetEntity>of(SmallStorageNetEntity::new, MobCategory.MISC)
                    .sized(SmallStorageNetEntity.WIDTH, SmallStorageNetEntity.HANG)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(buildKey("small_storage_net")));

    public static final RegistrySupplier<EntityType<WallStorageNetEntity>> STORAGE_NET =
            ENTITY_TYPES.register("storage_net", () -> EntityType.Builder
                    .<WallStorageNetEntity>of(WallStorageNetEntity::new, MobCategory.MISC)
                    .sized(WallStorageNetEntity.WIDTH, WallStorageNetEntity.HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .build(buildKey("storage_net")));

    private ModEntities() {
    }

    /**
     * What {@code EntityType.Builder.build} wants for an id: a bare name before 1.21.2, a registry
     * key after. Returning whichever the version expects keeps the call sites version-agnostic.
     */
    //? if >=1.21.2 {
    /*private static ResourceKey<EntityType<?>> buildKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Fixfoot.MOD_ID, name));
    }
    *///?} else {
    private static String buildKey(String name) {
        return name;
    }
    //?}

    public static void register() {
        ENTITY_TYPES.register();
    }
}
