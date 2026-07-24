package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.block.DisplayFixtureBlock;
import com.beepsterr.fixfoot.block.SlatPanelBlock;
import com.beepsterr.fixfoot.block.SlatScreenBlock;
import com.beepsterr.fixfoot.content.SlatVariants;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

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

    public static final RegistrySupplier<Block> COPPER_SHEETMETAL = BLOCKS.register(
            "copper_sheetmetal",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)));

    public static final RegistrySupplier<Block> IRON_SHEETMETAL = BLOCKS.register(
            "iron_sheetmetal",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    // Per-variant slat blocks: a wall-hugging multiface block + a standing screen, keyed by variant.
    public static final Map<String, RegistrySupplier<Block>> WALL = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<Block>> SCREEN = new LinkedHashMap<>();

    static {
        for (String key : SlatVariants.KEYS) {
            WALL.put(key, BLOCKS.register(key + "_slats",
                    () -> new SlatPanelBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
                            .noCollission())));
            SCREEN.put(key, BLOCKS.register(key + "_slat_screen",
                    () -> new SlatScreenBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion())));
        }
    }

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
    }
}
