package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.block.CopperBasinBlock;
import com.beepsterr.fixfoot.block.CopperDrainPipeBlock;
import com.beepsterr.fixfoot.block.CopperGutterBlock;
import com.beepsterr.fixfoot.block.SlatPanelBlock;
import com.beepsterr.fixfoot.block.SheetmetalBlock;
import com.beepsterr.fixfoot.block.SlatScreenBlock;
import com.beepsterr.fixfoot.block.WeatheringCopperBasinBlock;
import com.beepsterr.fixfoot.block.WeatheringCopperDrainPipeBlock;
import com.beepsterr.fixfoot.block.WeatheringCopperGutterBlock;
import com.beepsterr.fixfoot.content.SlatVariants;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
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
     * a waxed twin that doesn't. The generators build their assets from the same list.
     */
    public static final List<WeatheringCopper.WeatherState> COPPER_STAGES = List.of(
            WeatheringCopper.WeatherState.UNAFFECTED,
            WeatheringCopper.WeatherState.EXPOSED,
            WeatheringCopper.WeatherState.WEATHERED,
            WeatheringCopper.WeatherState.OXIDIZED);

    /** The copper things that weather, by the tail of their id — used to walk every family. */
    public static final List<String> COPPER_FAMILIES = List.of("basin", "gutter", "drain_pipe");

    /** e.g. {@code (WEATHERED, true, "basin")} -> {@code waxed_weathered_copper_basin}. */
    public static String copperName(WeatheringCopper.WeatherState stage, boolean waxed, String family) {
        String age = stage == WeatheringCopper.WeatherState.UNAFFECTED ? "" : stage.getSerializedName() + "_";
        return (waxed ? "waxed_" : "") + age + "copper_" + family;
    }

    /** Every copper block keyed by id: per family, the four stages and then their waxed twins. */
    public static final Map<String, RegistrySupplier<Block>> COPPER = new LinkedHashMap<>();

    // Per-variant slat blocks: a wall-hugging multiface block + a standing screen, keyed by variant.
    public static final Map<String, RegistrySupplier<Block>> WALL = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<Block>> SCREEN = new LinkedHashMap<>();

    static {
        copperFamily("basin", WeatheringCopperBasinBlock::new, CopperBasinBlock::new);
        copperFamily("gutter", WeatheringCopperGutterBlock::new, CopperGutterBlock::new);
        copperFamily("drain_pipe", WeatheringCopperDrainPipeBlock::new, CopperDrainPipeBlock::new);
    }

    /**
     * Registers one family's eight blocks: the four ageing stages, then the waxed twins that never
     * change. Waxed copies the vanilla waxed block's properties, so they don't idle on random ticks.
     */
    private static void copperFamily(
            String family,
            BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, Block> ageing,
            Function<BlockBehaviour.Properties, Block> waxed) {
        for (WeatheringCopper.WeatherState stage : COPPER_STAGES) {
            String id = copperName(stage, false, family);
            COPPER.put(id, BLOCKS.register(id, () -> ageing.apply(stage, copperProperties(Blocks.COPPER_BLOCK))));
        }
        for (WeatheringCopper.WeatherState stage : COPPER_STAGES) {
            String id = copperName(stage, true, family);
            COPPER.put(id, BLOCKS.register(id, () -> waxed.apply(copperProperties(Blocks.WAXED_COPPER_BLOCK))));
        }
    }

    /** Copper's hardness, sound and tool, but open so it doesn't cull its neighbours. */
    private static BlockBehaviour.Properties copperProperties(Block copper) {
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
