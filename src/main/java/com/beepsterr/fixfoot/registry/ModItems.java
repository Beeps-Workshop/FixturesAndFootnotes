package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * Item registry — mostly {@link BlockItem}s for the deco blocks.
 *
 * <p>NOTE (version seam): 1.21.2+ requires the item's registry id to be set on
 * {@code Item.Properties} at construction ({@code new Item.Properties().setId(key)}).
 * On 1.21.1 (the active dev version) the bare constructor is correct. When 1.21.4 is
 * activated this line needs a Stonecutter conditional to supply the id.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> DISPLAY_FIXTURE = ITEMS.register(
            "display_fixture",
            () -> new BlockItem(ModBlocks.DISPLAY_FIXTURE.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
