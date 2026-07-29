package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.block.CopperBasinBlock;
import com.beepsterr.fixfoot.block.SlatPanelBlock;
import com.beepsterr.fixfoot.block.SheetmetalBlock;
import com.beepsterr.fixfoot.block.SlatScreenBlock;
import com.beepsterr.fixfoot.block.WeatheringCopperBasinBlock;
import com.beepsterr.fixfoot.content.SlatVariants;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> COPPER_SHEETMETAL = BLOCKS.register(
            "copper_sheetmetal",
            () -> new SheetmetalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)));

    public static final RegistrySupplier<Block> IRON_SHEETMETAL = BLOCKS.register(
            "iron_sheetmetal",
            () -> new SheetmetalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    /**
     * The copper oxidation chain, freshest first: each stage weathers into the next, and each has
     * a waxed twin that doesn't. {@code tools/gen_basin.py} builds the assets from the same list.
     */
    public static final List<WeatheringCopper.WeatherState> BASIN_STAGES = List.of(
            WeatheringCopper.WeatherState.UNAFFECTED,
            WeatheringCopper.WeatherState.EXPOSED,
            WeatheringCopper.WeatherState.WEATHERED,
            WeatheringCopper.WeatherState.OXIDIZED);

    /** Every basin keyed by id, the four stages first and then their waxed twins. */
    public static final Map<String, RegistrySupplier<Block>> BASINS = new LinkedHashMap<>();

    /** e.g. {@code (WEATHERED, true)} -> {@code waxed_weathered_copper_basin}. */
    public static String basinName(WeatheringCopper.WeatherState stage, boolean waxed) {
        String age = stage == WeatheringCopper.WeatherState.UNAFFECTED ? "" : stage.getSerializedName() + "_";
        return (waxed ? "waxed_" : "") + age + "copper_basin";
    }

    // Per-variant slat blocks: a wall-hugging multiface block + a standing screen, keyed by variant.
    public static final Map<String, RegistrySupplier<Block>> WALL = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<Block>> SCREEN = new LinkedHashMap<>();

    static {
        for (WeatheringCopper.WeatherState stage : BASIN_STAGES) {
            BASINS.put(basinName(stage, false), BLOCKS.register(basinName(stage, false),
                    () -> new WeatheringCopperBasinBlock(stage, basinProperties(Blocks.COPPER_BLOCK))));
        }
        for (WeatheringCopper.WeatherState stage : BASIN_STAGES) {
            BASINS.put(basinName(stage, true), BLOCKS.register(basinName(stage, true),
                    () -> new CopperBasinBlock(basinProperties(Blocks.WAXED_COPPER_BLOCK))));
        }
    }

    /** Copper's hardness, sound and tool, but open-topped so it doesn't cull its neighbours. */
    private static BlockBehaviour.Properties basinProperties(Block copper) {
        return BlockBehaviour.Properties.ofFullCopy(copper).noOcclusion();
    }

    static {
        for (String key : SlatVariants.KEYS) {
            WALL.put(key, BLOCKS.register(key + "_slats",
                    () -> new SlatPanelBlock(slatProperties(key).noOcclusion().noCollission())));
            SCREEN.put(key, BLOCKS.register(key + "_slat_screen",
                    () -> new SlatScreenBlock(slatProperties(key).noOcclusion())));
        }
    }

    /**
     * Woods share one set of plank-ish properties; the non-wood variants copy their vanilla block
     * instead, so glowstone/froglight slats emit light and magma slats keep magma's toughness.
     */
    private static BlockBehaviour.Properties slatProperties(String key) {
        Supplier<Block> special = SlatVariants.SPECIAL_BASE.get(key);
        if (special != null) {
            return BlockBehaviour.Properties.ofFullCopy(special.get());
        }
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(SoundType.WOOD);
    }

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
    }
}
