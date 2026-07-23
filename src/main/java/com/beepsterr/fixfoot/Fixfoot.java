package com.beepsterr.fixfoot;

import com.beepsterr.fixfoot.registry.ModBlockEntities;
import com.beepsterr.fixfoot.registry.ModBlocks;
import com.beepsterr.fixfoot.registry.ModCreativeTabs;
import com.beepsterr.fixfoot.registry.ModItems;

/**
 * Loader-agnostic entrypoint. Each loader's initializer calls {@link #init()}.
 * All registration goes through Architectury's DeferredRegister so this one method
 * works identically on Fabric and NeoForge.
 */
public final class Fixfoot {
    public static final String MOD_ID = "fixfoot";

    private Fixfoot() {
    }

    public static void init() {
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModCreativeTabs.register();
    }
}
