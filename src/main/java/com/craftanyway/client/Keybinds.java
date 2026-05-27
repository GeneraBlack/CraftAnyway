package com.craftanyway.client;

import com.craftanyway.client.gui.PlanScreen;
import com.craftanyway.jei.CraftAnywayJeiPlugin;
import com.craftanyway.planning.RecipePlanner;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.craftanyway"
    );

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Keybinds::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(Keybinds::onKeyInput);
        MinecraftForge.EVENT_BUS.addListener(Keybinds::onScreenKeyPressed);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PLAN_KEY);
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && PLAN_KEY.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))) {
            tryOpenPlanUI();
        }
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (PLAN_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            if (tryOpenPlanUI()) {
                event.setCanceled(true); // Consume the keypress so the screen doesn't process it (e.g., closing or typing 'p')
            }
        }
    }

    private static boolean tryOpenPlanUI() {
        ItemStack ingredientUnderMouse = CraftAnywayJeiPlugin.getIngredientUnderMouse();
        Minecraft mc = Minecraft.getInstance();
        
        if (ingredientUnderMouse != null && !ingredientUnderMouse.isEmpty()) {
            if (mc.level != null) {
                var opt = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().createTypedIngredient(ingredientUnderMouse);
                if (opt.isPresent()) RecipePlanner.plan(opt.get(), ingredientUnderMouse.getCount());
                
                if (!RecipePlanner.getAlternativePlans().isEmpty()) {
                    mc.setScreen(new PlanScreen(RecipePlanner.getAlternativePlans()));
                    return true;
                } else {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("No craftable recipe found for " + ingredientUnderMouse.getHoverName().getString()), false);
                    }
                }
            }
        } else {
            // No item under mouse. Re-open the current PlanScreen if available.
            if (RecipePlanner.getCurrentPlan() != null && !RecipePlanner.getAlternativePlans().isEmpty()) {
                mc.setScreen(new PlanScreen(RecipePlanner.getAlternativePlans()));
                return true;
            }
        }
        return false;
    }
}
