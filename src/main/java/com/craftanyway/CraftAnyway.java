package com.craftanyway;

import com.craftanyway.client.Keybinds;
import com.craftanyway.client.gui.CraftingScreenOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(CraftAnyway.MODID)
public class CraftAnyway {
    public static final String MODID = "craftanyway";

    public CraftAnyway(IEventBus modEventBus, ModContainer modContainer) {
        // Register client setup event
        modEventBus.addListener(this::onClientSetup);
        
        // Register keybinds
        Keybinds.register(modEventBus);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Initialization code if needed
    }
}
