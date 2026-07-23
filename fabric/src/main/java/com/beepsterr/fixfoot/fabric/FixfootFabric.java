package com.beepsterr.fixfoot.fabric;

import com.beepsterr.fixfoot.Fixfoot;
import net.fabricmc.api.ModInitializer;

public final class FixfootFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Fixfoot.init();
    }
}
