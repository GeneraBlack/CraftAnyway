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
import java.util.ArrayList;

public class PlanScreen extends Screen {

    private List<CraftingPlan> plans;
    private double panX = 0;
    private double panY = 0;
    private double zoom = 1.0;
    private ItemStack targetItem;

    private static class NodeHitbox {
        CraftingPlan.PlanNode node;
        int x, y, width, height;
        NodeHitbox(CraftingPlan.PlanNode node, int x, int y, int width, int height) {
            this.node = node;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }
    private List<NodeHitbox> nodeHitboxes = new ArrayList<>();

    private boolean popupActive = false;
    private CraftingPlan.PlanNode popupNode = null;
    private List<RecipePlanner.RecipeOption> popupOptions = null;
    private List<RecipePlanner.RecipeOption> popupVariants = null;
    private double popupX, popupY;

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
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw a premium custom dark opaque background to avoid Minecraft's default blur shader and prevent ghosting
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF0A0A0A);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panX, panY, 50); // Translate Z by 50 to avoid any background overlap
        guiGraphics.pose().scale((float) zoom, (float) zoom, 1f);

        nodeHitboxes.clear();

        int startX = 100;
        for (int i = 0; i < plans.size(); i++) {
            CraftingPlan plan = plans.get(i);
            int xPos = startX + (i * 400); // Increased spacing
            
            // Calculate dynamic requirements
            CraftingPlan.PlanResult result = plan.calculateRequirements(this.minecraft.player != null ? this.minecraft.player.getInventory() : null);

            int ry = 20;
            int rx = xPos - 40;

            if (!result.steps.isEmpty()) {
                guiGraphics.drawString(this.font, "Step-by-Step Breakdown:", xPos - 40, ry, 0xFFFFAA);
                ry += 15;
                for (CraftingPlan.CraftingStep step : result.steps) {
                    guiGraphics.drawString(this.font, "Step " + step.stepNumber + ":", xPos - 40, ry, 0xAAAAAA);
                    ry += 15;
                    rx = xPos - 40;
                    
                    for (CraftingPlan.StepItem stepItem : step.items.values()) {
                        ItemStack stack = stepItem.stack.copy();
                        guiGraphics.renderItem(stack, rx, ry);
                        guiGraphics.renderItemDecorations(this.font, stack, rx, ry);
                        
                        String text = stepItem.have + "/" + stepItem.needed;
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().scale(0.75f, 0.75f, 1f);
                        guiGraphics.drawString(this.font, text, (int)((rx + 1) / 0.75f), (int)((ry + 17) / 0.75f), 0xAAAAAA);
                        guiGraphics.pose().popPose();
                        
                        rx += 25;
                        if (rx > xPos + 120) {
                            rx = xPos - 40;
                            ry += 25;
                        }
                    }
                    ry += 25;
                }
            } else {
                ry += 5;
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

        if (popupActive && popupOptions != null) {
            renderPopup(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderPopup(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int width = 200;
        int rowHeight = 20;
        int headerHeight = 15;
        
        int optionsCount = popupOptions != null ? popupOptions.size() : 0;
        int variantsCount = popupVariants != null ? popupVariants.size() : 0;
        
        int totalRows = optionsCount + variantsCount;
        int height = totalRows * rowHeight + 10;
        if (optionsCount > 0) height += headerHeight;
        if (variantsCount > 0) height += headerHeight;
        
        guiGraphics.fill((int)popupX, (int)popupY, (int)popupX + width, (int)popupY + height, 0xEE0A0A0A);
        guiGraphics.renderOutline((int)popupX, (int)popupY, width, height, 0xFFAAAAAA);
        
        int currentY = (int)popupY + 5;
        
        if (optionsCount > 0) {
            guiGraphics.drawString(this.font, "Alternative Recipes:", (int)popupX + 5, currentY, 0xAAAAAA);
            currentY += headerHeight;
            for (RecipePlanner.RecipeOption opt : popupOptions) {
                boolean hovered = mouseX >= popupX && mouseX <= popupX + width && mouseY >= currentY && mouseY <= currentY + rowHeight;
                if (hovered) {
                    guiGraphics.fill((int)popupX + 1, currentY, (int)popupX + width - 1, currentY + rowHeight, 0x55FFFFFF);
                }
                
                String text = opt.name + " (Cost: " + opt.cost + ")";
                guiGraphics.drawString(this.font, text, (int)popupX + 5, currentY + 6, hovered ? 0xFFFFAA : 0xFFFFFF);
                currentY += rowHeight;
            }
        }
        
        if (variantsCount > 0) {
            guiGraphics.drawString(this.font, "Alternative Variants:", (int)popupX + 5, currentY, 0xAAAAAA);
            currentY += headerHeight;
            for (RecipePlanner.RecipeOption opt : popupVariants) {
                boolean hovered = mouseX >= popupX && mouseX <= popupX + width && mouseY >= currentY && mouseY <= currentY + rowHeight;
                if (hovered) {
                    guiGraphics.fill((int)popupX + 1, currentY, (int)popupX + width - 1, currentY + rowHeight, 0x55FFFFFF);
                }
                
                String text = opt.name;
                guiGraphics.drawString(this.font, text, (int)popupX + 5, currentY + 6, hovered ? 0xAAFFAA : 0xFFFFFF);
                currentY += rowHeight;
            }
        }
    }

    private void drawNodeTree(GuiGraphics guiGraphics, CraftingPlan.PlanNode node, int x, int y) {
        if (node == null) return;

        // Draw item
        guiGraphics.renderItem(node.getOutput(), x - 8, y);
        // Force rendering the count even if 1 to show quantities everywhere
        String qtyText = String.valueOf(node.getOutput().getCount());
        guiGraphics.renderItemDecorations(this.font, node.getOutput(), x - 8, y, qtyText);
        nodeHitboxes.add(new NodeHitbox(node, x - 8, y, 16, 16));
        
        if (node.getTagOptions() != null && node.getTagOptions().length > 1) {
            guiGraphics.drawString(this.font, "*", x + 4, y - 4, 0xFFFF55);
        }

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

                // Draw connecting line with thickness 4 to survive any zoom scaling and center properly
                guiGraphics.fill(x - 1, y + 16, x + 3, y + 28, 0xFFFFFFFF); // Vertical down from parent
                guiGraphics.fill(Math.min(x, childX) - 1, y + 28, Math.max(x, childX) + 3, y + 32, 0xFFFFFFFF); // Horizontal branch
                guiGraphics.fill(childX - 1, y + 28, childX + 3, childY, 0xFFFFFFFF); // Vertical down to child

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popupActive) {
            int width = 200;
            int rowHeight = 20;
            int headerHeight = 15;
            
            int optionsCount = popupOptions != null ? popupOptions.size() : 0;
            int variantsCount = popupVariants != null ? popupVariants.size() : 0;
            
            int totalRows = optionsCount + variantsCount;
            int height = totalRows * rowHeight + 10;
            if (optionsCount > 0) height += headerHeight;
            if (variantsCount > 0) height += headerHeight;
            
            if (mouseX >= popupX && mouseX <= popupX + width && mouseY >= popupY && mouseY <= popupY + height) {
                int currentY = (int)popupY + 5;
                
                if (optionsCount > 0) {
                    currentY += headerHeight;
                    for (RecipePlanner.RecipeOption opt : popupOptions) {
                        if (mouseY >= currentY && mouseY <= currentY + rowHeight) {
                            RecipePlanner.userPreferences.put(popupNode.getOutput().getItem().toString(), opt.recipeId);
                            RecipePlanner.plan(targetItem);
                            this.plans = RecipePlanner.getAlternativePlans();
                            this.clearWidgets();
                            this.init();
                            popupActive = false;
                            return true;
                        }
                        currentY += rowHeight;
                    }
                }
                
                if (variantsCount > 0) {
                    currentY += headerHeight;
                    for (RecipePlanner.RecipeOption opt : popupVariants) {
                        if (mouseY >= currentY && mouseY <= currentY + rowHeight) {
                            RecipePlanner.tagPreferences.put(popupNode.getTagSignature(), opt.recipeId);
                            RecipePlanner.plan(targetItem);
                            this.plans = RecipePlanner.getAlternativePlans();
                            this.clearWidgets();
                            this.init();
                            popupActive = false;
                            return true;
                        }
                        currentY += rowHeight;
                    }
                }
            } else {
                popupActive = false;
                return true;
            }
        }

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0 || button == 1) { // Left or right click
            double worldX = (mouseX - panX) / zoom;
            double worldY = (mouseY - panY) / zoom;

            for (NodeHitbox box : nodeHitboxes) {
                if (box.contains(worldX, worldY)) {
                    this.popupActive = true;
                    this.popupNode = box.node;
                    ItemStack targetForOptions = box.node.getOutput().copy();
                    this.popupOptions = RecipePlanner.getAvailableOptions(targetForOptions);
                    this.popupVariants = RecipePlanner.getVariantOptions(box.node);
                    this.popupX = mouseX;
                    this.popupY = mouseY;
                    return true;
                }
            }
        }
        
        return false;
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
}
