package com.craftanyway.client.gui;

import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanScreen extends Screen {

    private List<CraftingPlan> plans;
    private double panX = 0;
    private double panY = 0;
    private double zoom = 1.0;
    private ItemStack targetItem;

    public PlanScreen(List<CraftingPlan> plans) {
        super(Component.literal("Crafting Plan Tree"));
        this.plans = plans;
        if (!plans.isEmpty()) {
            this.targetItem = plans.get(0).getTarget().copy();
        }
    }

    @Override
    protected void init() {
        super.init();
        
        if (targetItem != null) {
            this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
                if (targetItem.getCount() > 1) {
                    targetItem.shrink(1);
                    RecipePlanner.plan(targetItem);
                    this.plans = RecipePlanner.getAlternativePlans();
                    this.clearWidgets();
                    this.init();
                }
            }).bounds(10, 30, 20, 20).build());
            
            this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                if (targetItem.getCount() < targetItem.getMaxStackSize()) {
                    targetItem.grow(1);
                    RecipePlanner.plan(targetItem);
                    this.plans = RecipePlanner.getAlternativePlans();
                    this.clearWidgets();
                    this.init();
                }
            }).bounds(80, 30, 20, 20).build());
        }
        
        int startX = 50;
        for (int i = 0; i < plans.size(); i++) {
            final CraftingPlan plan = plans.get(i);
            int xPos = startX + (i * 400); // Match new spacing
            
            this.addRenderableWidget(Button.builder(Component.literal("Select Path " + (i + 1)), btn -> {
                RecipePlanner.setCurrentPlan(plan);
                this.minecraft.setScreen(null);
            }).bounds(xPos, 40, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw a premium custom dark semi-transparent background to avoid Minecraft's default blur shader
        guiGraphics.fill(0, 0, this.width, this.height, 0xD00A0A0A);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panX, panY, 0);
        guiGraphics.pose().scale((float) zoom, (float) zoom, 1f);

        int startX = 100;
        for (int i = 0; i < plans.size(); i++) {
            CraftingPlan plan = plans.get(i);
            int xPos = startX + (i * 400); // Increased spacing
            
            // Calculate dynamic requirements
            CraftingPlan.PlanResult result = plan.calculateRequirements(this.minecraft.player != null ? this.minecraft.player.getInventory() : null);

            int ry = 20;
            boolean hasRequired = false;
            int rx = xPos - 40;

            for (ItemStack stack : result.rawMaterials.values()) {
                int needed = stack.getCount();
                int have = this.minecraft.player != null ? countItem(this.minecraft.player.getInventory(), stack.getItem()) : 0;
                int missing = Math.max(0, needed - have);
                if (missing > 0) {
                    hasRequired = true;
                    break;
                }
            }

            if (hasRequired) {
                guiGraphics.drawString(this.font, "Required Materials:", xPos - 40, ry, 0xFFFFAA);
                ry += 15;
                for (ItemStack stack : result.rawMaterials.values()) {
                    int needed = stack.getCount();
                    int have = this.minecraft.player != null ? countItem(this.minecraft.player.getInventory(), stack.getItem()) : 0;
                    int missing = Math.max(0, needed - have);

                    if (missing <= 0) continue;

                    ItemStack copy = stack.copy();
                    copy.setCount(missing);

                    guiGraphics.renderItem(copy, rx, ry);
                    guiGraphics.renderItemDecorations(this.font, copy, rx, ry);
                    rx += 20;
                    if (rx > xPos + 120) {
                        rx = xPos - 40;
                        ry += 20;
                    }
                }
                ry += 25;
            } else {
                ry += 5;
            }

            if (!result.alternatives.isEmpty()) {
                guiGraphics.drawString(this.font, "Alternatives:", xPos - 40, ry, 0xFFFFAA);
                ry += 15;
                rx = xPos - 40;
                for (CraftingPlan.AlternativeItem alt : result.alternatives.values()) {
                    ItemStack stack = alt.getStack();
                    guiGraphics.renderItem(stack, rx, ry);
                    guiGraphics.renderItemDecorations(this.font, stack, rx, ry);

                    String frac = getFractionString(alt.getHave(), alt.getNeeded());
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(0.75f, 0.75f, 1f);
                    guiGraphics.drawString(this.font, frac, (int)((rx + 1) / 0.75f), (int)((ry + 17) / 0.75f), 0xAAAAAA);
                    guiGraphics.pose().popPose();

                    rx += 25;
                    if (rx > xPos + 120) {
                        rx = xPos - 40;
                        ry += 25;
                    }
                }
                ry += 25;
            }

            drawNodeTree(guiGraphics, plan.getRootNode(), xPos, ry + 20);
        }

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Drag to pan. Scroll to zoom. Click a button to select a path.", 10, 10, 0xAAAAAA);

        if (targetItem != null) {
            guiGraphics.drawString(this.font, "Qty: " + targetItem.getCount(), 35, 36, 0xFFFFFF);
        }
    }

    private void drawNodeTree(GuiGraphics guiGraphics, CraftingPlan.PlanNode node, int x, int y) {
        if (node == null) return;

        // Draw item
        guiGraphics.renderItem(node.getOutput(), x - 8, y);
        guiGraphics.renderItemDecorations(this.font, node.getOutput(), x - 8, y);

        // Draw category text below item
        String cat = node.getCategoryName();
        if (cat != null && !cat.equals("None")) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f, 0.5f, 1f);
            guiGraphics.drawCenteredString(this.font, cat, (x) * 2, (y + 18) * 2, 0xFFFF55);
            guiGraphics.pose().popPose();
        }

        if (!node.isLeaf()) {
            List<CraftingPlan.PlanNode> children = node.getChildren();

            // Calculate total width of all children subtrees
            int totalWidth = 0;
            int[] childWidths = new int[children.size()];
            for (int i = 0; i < children.size(); i++) {
                childWidths[i] = calculateSubtreeWidth(children.get(i));
                totalWidth += childWidths[i];
            }

            int startX = x - (totalWidth / 2);
            int childY = y + 50; // Increased vertical spacing for modern airy look

            int currentX = startX;
            for (int i = 0; i < children.size(); i++) {
                int childWidth = childWidths[i];
                int childX = currentX + (childWidth / 2);

                // Draw connecting line
                guiGraphics.fill(x, y + 16, x + 1, y + 28, 0xFFFFFFFF); // Vertical down from parent
                guiGraphics.fill(Math.min(x, childX), y + 28, Math.max(x, childX) + 1, y + 29, 0xFFFFFFFF); // Horizontal branch
                guiGraphics.fill(childX, y + 28, childX + 1, childY, 0xFFFFFFFF); // Vertical down to child

                drawNodeTree(guiGraphics, children.get(i), childX, childY);

                currentX += childWidth;
            }
        }
    }

    private int calculateSubtreeWidth(CraftingPlan.PlanNode node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 50; // Premium horizontal spacing per leaf node

        int total = 0;
        for (CraftingPlan.PlanNode child : node.getChildren()) {
            total += calculateSubtreeWidth(child);
        }
        return Math.max(50, total);
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

    private int countItem(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        panX += dragX;
        panY += dragY;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            zoom *= 1.1;
        } else if (scrollY < 0) {
            zoom /= 1.1;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Do nothing to prevent the vanilla background blur shader from being applied, keeping the panning/zooming tree completely crisp!
    }
}
