package com.beepsterr.fixfoot.fabric;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.registry.ModBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;

public final class FixfootFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Fixfoot.init();
        registerCopperWeathering();
    }

    /**
     * Teaches Fabric's copies of the vanilla oxidation/waxing maps about every copper family, which
     * gets both the random weathering tick and axe-scraping/honeycomb-waxing. NeoForge does the same
     * job from JSON — see {@code data/neoforge/data_maps/block/}.
     */
    private static void registerCopperWeathering() {
        for (String family : ModBlocks.COPPER_FAMILIES) {
            for (int i = 0; i < ModBlocks.COPPER_STAGES.size(); i++) {
                WeatheringCopper.WeatherState stage = ModBlocks.COPPER_STAGES.get(i);
                Block unwaxed = block(stage, false, family);
                OxidizableBlocksRegistry.registerWaxableBlockPair(unwaxed, block(stage, true, family));
                if (i + 1 < ModBlocks.COPPER_STAGES.size()) {
                    OxidizableBlocksRegistry.registerOxidizableBlockPair(
                            unwaxed, block(ModBlocks.COPPER_STAGES.get(i + 1), false, family));
                }
            }
        }
    }

    private static Block block(WeatheringCopper.WeatherState stage, boolean waxed, String family) {
        return ModBlocks.COPPER.get(ModBlocks.copperName(stage, waxed, family)).get();
    }
}
