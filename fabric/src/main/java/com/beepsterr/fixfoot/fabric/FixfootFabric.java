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
        registerBasinWeathering();
    }

    /**
     * Teaches Fabric's copies of the vanilla oxidation/waxing maps about the basins, which gets
     * both the random weathering tick and axe-scraping/honeycomb-waxing. NeoForge does the same
     * job from JSON — see {@code data/neoforge/data_maps/block/}.
     */
    private static void registerBasinWeathering() {
        for (int i = 0; i < ModBlocks.BASIN_STAGES.size(); i++) {
            WeatheringCopper.WeatherState stage = ModBlocks.BASIN_STAGES.get(i);
            Block unwaxed = basin(stage, false);
            OxidizableBlocksRegistry.registerWaxableBlockPair(unwaxed, basin(stage, true));
            if (i + 1 < ModBlocks.BASIN_STAGES.size()) {
                OxidizableBlocksRegistry.registerOxidizableBlockPair(
                        unwaxed, basin(ModBlocks.BASIN_STAGES.get(i + 1), false));
            }
        }
    }

    private static Block basin(WeatheringCopper.WeatherState stage, boolean waxed) {
        return ModBlocks.BASINS.get(ModBlocks.basinName(stage, waxed)).get();
    }
}
