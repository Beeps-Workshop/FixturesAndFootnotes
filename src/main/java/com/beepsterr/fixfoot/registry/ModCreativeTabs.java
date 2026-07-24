package com.beepsterr.fixfoot.registry;

import com.beepsterr.fixfoot.Fixfoot;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Fixfoot.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + Fixfoot.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ModItems.DISPLAY_FIXTURE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.DISPLAY_FIXTURE.get());
                        output.accept(ModItems.COPPER_SHEETMETAL.get());
                        output.accept(ModItems.IRON_SHEETMETAL.get());
                        for (String key : com.beepsterr.fixfoot.content.SlatVariants.KEYS) {
                            output.accept(ModItems.SLAT_ITEMS.get(key).get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register() {
        TABS.register();
    }
}
