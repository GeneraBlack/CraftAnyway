package com.craftanyway;

import com.craftanyway.client.Keybinds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CraftAnyway.MODID)
public class CraftAnyway {
    public static final String MODID = "craftanyway";

    public CraftAnyway() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register client setup event
        modEventBus.addListener(this::onClientSetup);
        
        // Register keybinds
        Keybinds.register(modEventBus);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Initialization code if needed
    }
}
