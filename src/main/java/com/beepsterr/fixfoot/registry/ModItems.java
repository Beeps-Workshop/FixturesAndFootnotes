package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.content.SlatVariants;
import com.beepsterr.fixfoot.item.SlatsItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> DISPLAY_FIXTURE = ITEMS.register(
            "display_fixture",
            () -> new BlockItem(ModBlocks.DISPLAY_FIXTURE.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> COPPER_SHEETMETAL = ITEMS.register(
            "copper_sheetmetal",
            () -> new BlockItem(ModBlocks.COPPER_SHEETMETAL.get(), new Item.Properties()));

    // One item per variant — places wall slats on a wall face, the standing screen on a floor.
    public static final Map<String, RegistrySupplier<Item>> SLAT_ITEMS = new LinkedHashMap<>();

    static {
        for (String key : SlatVariants.KEYS) {
            SLAT_ITEMS.put(key, ITEMS.register(key + "_slats",
                    () -> new SlatsItem(ModBlocks.WALL.get(key).get(), ModBlocks.SCREEN.get(key).get(),
                            new Item.Properties())));
        }
    }

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
