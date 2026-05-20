package com.craftanyway.client.gui;

import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = com.craftanyway.CraftAnyway.MODID, bus = EventBusSubscriber.Bus.GAME)
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

    private static void renderShoppingList(GuiGraphics guiGraphics, int x, int y) {
        CraftingPlan plan = RecipePlanner.getCurrentPlan();
        if (plan == null) return;

        Minecraft mc = Minecraft.getInstance();
        Inventory inv = mc.player != null ? mc.player.getInventory() : null;

        CraftingPlan.PlanResult result = plan.calculateRequirements(inv);

        boolean hasRaw = false;
        for (ItemStack stack : result.rawMaterials.values()) {
            int needed = stack.getCount();
            int have = inv != null ? countItem(inv, stack.getItem()) : 0;
            int missing = Math.max(0, needed - have);
            if (missing > 0) {
                hasRaw = true;
                break;
            }
        }

        int ry = y;

        if (hasRaw) {
            guiGraphics.drawString(mc.font, "Shopping List:", x, ry, 0xFFFFAA);
            ry += 15;

            for (ItemStack stack : result.rawMaterials.values()) {
                int needed = stack.getCount();
                int have = inv != null ? countItem(inv, stack.getItem()) : 0;
                int missing = Math.max(0, needed - have);

                if (missing <= 0) continue; // Hide fully satisfied items

                guiGraphics.renderItem(stack, x, ry);
                guiGraphics.renderItemDecorations(mc.font, stack, x, ry);

                guiGraphics.drawString(mc.font, have + "/" + needed, x + 20, ry + 4, 0xFFFFFF);
                ry += 20;
            }
        }

        if (!result.alternatives.isEmpty()) {
            if (hasRaw) {
                ry += 5;
            }
            guiGraphics.drawString(mc.font, "Alternatives:", x, ry, 0xFFFFAA);
            ry += 15;

            for (CraftingPlan.AlternativeItem alt : result.alternatives.values()) {
                ItemStack stack = alt.getStack();
                int have = alt.getHave();
                int needed = alt.getNeeded();

                guiGraphics.renderItem(stack, x, ry);
                guiGraphics.renderItemDecorations(mc.font, stack, x, ry);

                String fractionStr = getFractionString(have, needed) + " (" + have + "/" + needed + ")";
                guiGraphics.drawString(mc.font, fractionStr, x + 20, ry + 4, 0xAAAAAA);
                ry += 20;
            }
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
