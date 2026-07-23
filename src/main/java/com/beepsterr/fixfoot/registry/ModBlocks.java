package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.block.DisplayFixtureBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Central block registry. Adding a deco block is one {@code register(...)} line here
 * plus its blockstate/model/loot JSON under {@code src/main/resources}.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> DISPLAY_FIXTURE = BLOCKS.register(
            "display_fixture",
            () -> new DisplayFixtureBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
    }
}
