package com.craftanyway.client.gui;

import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = com.craftanyway.CraftAnyway.MODID, value = Dist.CLIENT)
public class ShoppingListOverlay {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        renderShoppingList(event.getGuiGraphics(), 10, 10);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            int x = screen.getGuiLeft() - 80;
            int y = screen.getGuiTop();
            if (x < 10) x = 10;
            renderShoppingList(event.getGuiGraphics(), x, y);
        }
    }

    private static void renderShoppingList(GuiGraphicsExtractor guiGraphics, int x, int y) {
        try {
            CraftingPlan plan = RecipePlanner.getCurrentPlan();
            if (plan == null) return;
    
            Minecraft mc = Minecraft.getInstance();
            Inventory inv = mc.player != null ? mc.player.getInventory() : null;
    
            CraftingPlan.PlanResult result = plan.calculateRequirements(inv);
    
            if (result.steps.isEmpty()) return;
    
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0, 0); // Elevate entire shopping list
    
            int ry = y;
            guiGraphics.textRenderer().accept(x, ry, net.minecraft.network.chat.Component.literal("Crafting Steps:").withStyle(s -> s.withColor(0xFFFFFFAA)));
            ry += 15;
    
            for (CraftingPlan.CraftingStep step : result.steps) {
                guiGraphics.textRenderer().accept(x, ry, net.minecraft.network.chat.Component.literal("Step " + step.stepNumber + ":").withStyle(s -> s.withColor(0xFFAAAAAA)));
                ry += 12;
    
                for (CraftingPlan.StepItem stepItem : step.items.values()) {
                    mezz.jei.api.ingredients.ITypedIngredient<?> stack = stepItem.ingredient;
                    int have = (int) stepItem.have;
                    int needed = (int) stepItem.needed;
    
                    var renderer = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().getIngredientRenderer(stack.getType());
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(x, ry);
                    ((mezz.jei.api.ingredients.IIngredientRenderer<Object>)renderer).render(guiGraphics, stack.getIngredient());
                    guiGraphics.pose().popMatrix();
    
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(0, 0); // Elevate text above items
                    int color = have >= needed ? 0xFF55FF55 : 0xFFFFFFFF;
                    guiGraphics.textRenderer().accept(x + 20, ry + 4, net.minecraft.network.chat.Component.literal(have + "/" + needed).withStyle(s -> s.withColor(color)));
                    guiGraphics.pose().popMatrix();
                    ry += 20;
                }
                ry += 5;
            }
            
            guiGraphics.pose().popMatrix();
        } catch (Exception e) {
            // Ignore exception to prevent silent overlay crash
        }
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static String getFractionString(int have, int needed) {
        if (needed <= 0) return "0";
        int g = gcd(have, needed);
        int num = have / g;
        int den = needed / g;
        if (den == 1) {
            return String.valueOf(num);
        }
        return num + "/" + den;
    }

    private static int countItem(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
