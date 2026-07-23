package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.block.DisplayFixtureBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<DisplayFixtureBlockEntity>> DISPLAY_FIXTURE =
            BLOCK_ENTITIES.register("display_fixture", () ->
                    BlockEntityType.Builder.of(DisplayFixtureBlockEntity::new, ModBlocks.DISPLAY_FIXTURE.get())
                            .build(null));

    private ModBlockEntities() {
    }

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
