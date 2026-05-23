package com.craftanyway.client.gui;

import com.craftanyway.execution.CraftExecutor;
import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = com.craftanyway.CraftAnyway.MODID, bus = EventBusSubscriber.Bus.FORGE)
public class CraftingScreenOverlay {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CraftingScreen screen) {
            CraftingPlan currentPlan = RecipePlanner.getCurrentPlan();
            
            // We always add the button, or only if there is a plan?
            // Let's add it only if there is a plan, or enable/disable it.
            
            Button craftButton = Button.builder(Component.literal("Craft Plan"), btn -> {
                if (currentPlan != null) {
                    CraftExecutor.startExecution(currentPlan, screen);
                } else {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("No plan active! Press P in JEI."), false);
                    }
                }
            })
            .bounds(screen.getGuiLeft() + 10, screen.getGuiTop() - 25, 80, 20)
            .build();
            
            event.addListener(craftButton);
        }
    }
}
