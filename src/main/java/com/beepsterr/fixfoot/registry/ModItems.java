package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import com.beepsterr.fixfoot.content.SlatVariants;
import com.beepsterr.fixfoot.entity.SmallStorageNetEntity;
import com.beepsterr.fixfoot.entity.WallStorageNetEntity;
import com.beepsterr.fixfoot.entity.WidePlanterEntity;
import com.beepsterr.fixfoot.item.AttachedFixtureItem;
import com.beepsterr.fixfoot.item.SlatsItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> COPPER_SHEETMETAL = ITEMS.register(
            "copper_sheetmetal",
            () -> new BlockItem(ModBlocks.COPPER_SHEETMETAL.get(), identifiedBlock("copper_sheetmetal")));

    public static final RegistrySupplier<Item> IRON_SHEETMETAL = ITEMS.register(
            "iron_sheetmetal",
            () -> new BlockItem(ModBlocks.IRON_SHEETMETAL.get(), identifiedBlock("iron_sheetmetal")));

    // Not a BlockItem: this places a hanging entity onto the clicked face, item-frame style.
    public static final RegistrySupplier<Item> WIDE_PLANTER = ITEMS.register(
            "wide_planter",
            () -> new AttachedFixtureItem(WidePlanterEntity::new, identified("wide_planter")));

    public static final RegistrySupplier<Item> SMALL_STORAGE_NET = ITEMS.register(
            "small_storage_net",
            () -> new AttachedFixtureItem(SmallStorageNetEntity::new, identified("small_storage_net")));

    public static final RegistrySupplier<Item> STORAGE_NET = ITEMS.register(
            "storage_net",
            () -> new AttachedFixtureItem(WallStorageNetEntity::new, identified("storage_net")));

    /** Plain block items, one per copper block, in the same order as {@link ModBlocks#COPPER}. */
    public static final Map<String, RegistrySupplier<Item>> COPPER_ITEMS = new LinkedHashMap<>();

    // One item per variant — places wall slats on a wall face, the standing screen on a floor.
    public static final Map<String, RegistrySupplier<Item>> SLAT_ITEMS = new LinkedHashMap<>();

    static {
        for (String name : ModBlocks.COPPER.keySet()) {
            COPPER_ITEMS.put(name, ITEMS.register(name,
                    () -> new BlockItem(ModBlocks.COPPER.get(name).get(), identifiedBlock(name))));
        }
    }

    static {
        for (String key : SlatVariants.KEYS) {
            SLAT_ITEMS.put(key, ITEMS.register(key + "_slats",
                    () -> new SlatsItem(ModBlocks.WALL.get(key).get(), ModBlocks.SCREEN.get(key).get(),
                            identifiedBlock(key + "_slats"))));
        }
    }

    /**
     * Stamps an item's registry id onto its properties. Same 1.21.2 requirement the blocks have —
     * the properties are read during construction, so an item built without one throws at
     * registration rather than failing to compile.
     */
    /**
     * As {@link #identified}, for an item that places a block. 1.21.2 stopped BlockItem borrowing
     * the block's name automatically and made it an opt-in, so without this every block item falls
     * back to an item.* key and shows up untranslated — the block itself is unaffected, which is
     * why placed blocks read fine and only the inventory is wrong.
     */
    private static Item.Properties identifiedBlock(String name) {
        Item.Properties properties = identified(name);
        //? if >=1.21.2 {
        /*return properties.useBlockDescriptionPrefix();
        *///?} else {
        return properties;
        //?}
    }

    private static Item.Properties identified(String name) {
        Item.Properties properties = new Item.Properties();
        //? if >=1.21.2 {
        /*return properties.setId(ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Fixfoot.MOD_ID, name)));
        *///?} else {
        return properties;
        //?}
    }

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
