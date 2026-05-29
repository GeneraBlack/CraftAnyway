package com.craftanyway.client;

import com.craftanyway.client.gui.PlanScreen;
import com.craftanyway.jei.CraftAnywayJeiPlugin;
import com.craftanyway.planning.RecipePlanner;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            GLFW.GLFW_KEY_P,
            new net.minecraft.client.KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath("craftanyway", "main"))
    );

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Keybinds::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(Keybinds::onKeyInput);
        NeoForge.EVENT_BUS.addListener(Keybinds::onScreenKeyPressed);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PLAN_KEY);
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && PLAN_KEY.isActiveAndMatches(InputConstants.Type.KEYSYM.getOrCreate(event.getKey()))) {
            tryOpenPlanUI();
        }
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (PLAN_KEY.isActiveAndMatches(InputConstants.Type.KEYSYM.getOrCreate(event.getKeyCode()))) {
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
                var opt = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().createTypedIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, ingredientUnderMouse);
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
