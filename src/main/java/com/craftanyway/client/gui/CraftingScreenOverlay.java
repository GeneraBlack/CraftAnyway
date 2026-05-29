package com.craftanyway.client.gui;

import com.craftanyway.execution.CraftExecutor;
import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = com.craftanyway.CraftAnyway.MODID, value = Dist.CLIENT)
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
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("No plan active! Press P in JEI."));
                    }
                }
            })
            .bounds(screen.getGuiLeft() + 10, screen.getGuiTop() - 25, 80, 20)
            .build();
            
            event.addListener(craftButton);
        }
    }
}
