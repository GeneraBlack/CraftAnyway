package com.craftanyway.client.gui;

import com.craftanyway.jei.CraftAnywayJeiPlugin;
import com.craftanyway.planning.CraftingPlan;
import com.craftanyway.planning.RecipePlanner;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class PlanScreen extends Screen {

    private List<CraftingPlan> plans;
    private double panX = 300;
    private double panY = 50;
    private double zoom = 1.0;
    private ITypedIngredient<?> targetIngredient;
    private long targetQuantity;

    private EditBox quantityField;

    private Map<CraftingPlan.PlanNode, IRecipeLayoutDrawable<?>> jeiLayouts = new HashMap<>();
    
    // UI state
    private boolean isDragging = false;
    
    // Dropdown state
    private CraftingPlan.PlanNode activeDropdownNode = null;
    private boolean isCategoryDropdown = false;
    private int dropdownX = 0;
    private int dropdownY = 0;
    private List<RecipePlanner.RecipeOption> currentDropdownOptions = new ArrayList<>();

    public PlanScreen(List<CraftingPlan> plans) {
        super(Component.literal("Crafting Planner"));
        this.plans = plans;
        if (!plans.isEmpty()) {
            this.targetIngredient = plans.get(0).getTarget();
            this.targetQuantity = plans.get(0).getTargetAmount();
        }
    }

    @Override
    protected void init() {
        super.init();
        
        if (targetIngredient != null) {
            this.quantityField = new EditBox(this.font, 45, 20, 50, 20, Component.literal("Quantity"));
            this.quantityField.setValue(String.valueOf(targetQuantity));
            this.addRenderableWidget(this.quantityField);

            this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
                if (targetQuantity > 1) {
                    targetQuantity--;
                    refreshPlan();
                }
            }).bounds(100, 20, 20, 20).build());
            
            this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                if (targetQuantity < 999999) {
                    targetQuantity++;
                    refreshPlan();
                }
            }).bounds(125, 20, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Set"), btn -> {
                try {
                    int newVal = Integer.parseInt(this.quantityField.getValue());
                    if (newVal > 0) {
                        targetQuantity = newVal;
                        refreshPlan();
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }).bounds(150, 20, 35, 20).build());
        }
    }
    
    private void refreshPlan() {
        RecipePlanner.plan(targetIngredient, targetQuantity);
        this.plans = RecipePlanner.getAlternativePlans();
        this.jeiLayouts.clear();
        this.activeDropdownNode = null;
        this.clearWidgets();
        this.init();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark grey background as requested in mockup
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF2B2B2B);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Split screen: 25% sidebar, 75% pathbuilder
        int sidebarWidth = Math.max(200, this.width / 4);
        
        // Render Pathbuilder Area
        guiGraphics.pose().pushPose();
        // Enable scissoring for the right panel so panning doesn't draw over the sidebar
        guiGraphics.enableScissor(sidebarWidth, 0, this.width, this.height);
        
        guiGraphics.pose().translate(panX, panY, 0);
        guiGraphics.pose().scale((float) zoom, (float) zoom, 1f);

        if (!plans.isEmpty()) {
            CraftingPlan plan = plans.get(0);
            drawNodeTree(guiGraphics, plan.getRootNode(), (this.width - sidebarWidth) / 2, 50, mouseX, mouseY);
        }

        guiGraphics.disableScissor();
        guiGraphics.pose().popPose();
        
        // Render JEI Overlays (Tooltips) and hitboxes
        guiGraphics.pose().pushPose();
        guiGraphics.enableScissor(sidebarWidth, 0, this.width, this.height);
        guiGraphics.pose().translate(panX, panY, 400); // High Z for overlays
        guiGraphics.pose().scale((float) zoom, (float) zoom, 1f);
        
        if (!plans.isEmpty()) {
            try {
                drawNodeOverlays(guiGraphics, plans.get(0).getRootNode(), (this.width - sidebarWidth) / 2, 50, mouseX, mouseY);
            } catch (Exception e) {
                // Ignore exception to prevent skipping disableScissor
            }
        }
        
        guiGraphics.disableScissor();
        guiGraphics.pose().popPose();

        // Render Sidebar
        guiGraphics.fill(0, 0, sidebarWidth, this.height, 0xFF353535);
        guiGraphics.fill(sidebarWidth, 0, sidebarWidth + 2, this.height, 0xFF111111); // separator
        
        guiGraphics.drawString(this.font, "Qty:", 20, 26, 0xFFFFFF);
        
        guiGraphics.drawString(this.font, "Step-by-Step Breakdown:", 10, 50, 0xFFFFFFFF);
        
        if (!plans.isEmpty()) {
            CraftingPlan.PlanResult result = plans.get(0).calculateRequirements(this.minecraft.player != null ? this.minecraft.player.getInventory() : null);
            int ry = 70;
            
            for (CraftingPlan.CraftingStep step : result.steps) {
                guiGraphics.drawString(this.font, "Step " + step.stepNumber + ":", 10, ry, 0xAAAAAA);
                ry += 15;
                int rx = 10;
                
                for (CraftingPlan.StepItem stepItem : step.items.values()) {
                    ITypedIngredient<?> stack = stepItem.ingredient;
                    renderIngredient(guiGraphics, stack, rx, ry);
                    renderIngredientDecorations(guiGraphics, stack, rx, ry, "");
                    
                    String text = stepItem.have + "/" + stepItem.needed;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 400); // Elevate amount high above items
                    guiGraphics.pose().scale(0.75f, 0.75f, 1f);
                    guiGraphics.drawString(this.font, text, (int)((rx + 1) / 0.75f), (int)((ry + 17) / 0.75f), 0xAAAAAA);
                    guiGraphics.pose().popPose();
                    
                    rx += 40;
                    if (rx > sidebarWidth - 40) {
                        rx = 10;
                        ry += 35;
                    }
                }
                ry += 25;
            }
        }
        
        // Render Header
        guiGraphics.drawCenteredString(this.font, "Crafting Planner", this.width / 2 + sidebarWidth / 2, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Pathbuilder:", sidebarWidth + 10, 30, 0xFFFFFFFF);
        
        // Render Dropdown if active (drawn last to be on top)
        if (activeDropdownNode != null) {
            renderDropdown(guiGraphics, mouseX, mouseY);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private <T> IRecipeLayoutDrawable<T> getJeiDrawable(CraftingPlan.PlanNode node) {
        if (jeiLayouts.containsKey(node)) {
            return (IRecipeLayoutDrawable<T>) jeiLayouts.get(node);
        }
        
        if (node.getRawRecipe() == null || node.getRecipeCategory() == null) {
            jeiLayouts.put(node, null);
            return null;
        }
        
        try {
            var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
            if (jeiRuntime != null) {
                IRecipeCategory<T> category = (IRecipeCategory<T>) node.getRecipeCategory();
                T recipe = (T) node.getRawRecipe();
                var focusGroup = jeiRuntime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
                
                Optional<IRecipeLayoutDrawable<T>> opt = jeiRuntime.getRecipeManager().createRecipeLayoutDrawable(category, recipe, focusGroup);
                if (opt.isPresent()) {
                    IRecipeLayoutDrawable<T> drawable = opt.get();
                    jeiLayouts.put(node, drawable);
                    return drawable;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        jeiLayouts.put(node, null);
        return null;
    }

    private void drawNodeTree(GuiGraphics guiGraphics, CraftingPlan.PlanNode node, int x, int y, int mouseX, int mouseY) {
        if (node == null) return;

        // Draw Name
        guiGraphics.drawCenteredString(this.font, getIngredientName(node.getOutput()), x, y, 0xFFFFFF);
        
        // Draw Icon and Qty
        renderIngredient(guiGraphics, node.getOutput(), x - 8, y + 12);
        renderIngredientDecorations(guiGraphics, node.getOutput(), x - 8, y + 12, String.valueOf(node.getAmount()));
        
        // Category Dropdown Box
        int catTextWidth = this.font.width(node.getCategoryName());
        int catWidth = Math.max(100, catTextWidth + 10);
        
        String recName = "Recipe";
        if (node.getRecipeId() != null && node.getRecipeId().contains(":")) {
            String[] parts = node.getRecipeId().split(":");
            recName = parts[parts.length - 1];
        }
        if (node.getCategoryName().equals("Raw") || node.getCategoryName().equals("Select Category...")) {
            recName = node.getCategoryName();
        }
        int recTextWidth = this.font.width(recName);
        int recWidth = Math.max(100, recTextWidth + 10);

        int dropY = y + 32;
        int catX = x - catWidth - 2;
        int recX = x + 2;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400); // Ensure boxes render above everything including items
        guiGraphics.fill(catX, dropY, catX + catWidth, dropY + 12, 0xFF555555);
        guiGraphics.fill(recX, dropY, recX + recWidth, dropY + 12, 0xFF555555);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 401); // Ensure text renders above boxes
        guiGraphics.drawString(this.font, node.getCategoryName(), catX + 5, dropY + 2, 0xFFFFFF);
        guiGraphics.drawString(this.font, recName, recX + 5, dropY + 2, 0xFFFFFF);
        guiGraphics.pose().popPose();
        
        int nextY = dropY + 16;
        
        // Draw JEI background manually
        IRecipeLayoutDrawable<?> drawable = getJeiDrawable(node);
        if (drawable != null) {
            int drawX = x - (drawable.getRect().getWidth() / 2);
            drawable.setPosition(drawX, nextY);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 2); // Ensure it renders above background
            // Draw a grey background panel for the JEI recipe since JEI 1.21 doesn't draw it automatically
            guiGraphics.fill(drawX - 5, nextY - 5, drawX + drawable.getRect().getWidth() + 5, nextY + drawable.getRect().getHeight() + 5, 0xFFC6C6C6);
            
            // Draw a dark border
            guiGraphics.renderOutline(drawX - 5, nextY - 5, drawable.getRect().getWidth() + 10, drawable.getRect().getHeight() + 10, 0xFF555555);
            guiGraphics.pose().popPose();
            
            // Adjust mouse coordinates to match zoom/pan for JEI internal checks
            int localMouseX = (int)((mouseX - panX) / zoom);
            int localMouseY = (int)((mouseY - panY) / zoom);
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 10);
            drawable.drawRecipe(guiGraphics, localMouseX, localMouseY);
            guiGraphics.pose().popPose();
            
            nextY += drawable.getRect().getHeight() + 15;
        } else {
            nextY += 15; // Spacing if no recipe
        }

        if (!node.isLeaf()) {
            List<CraftingPlan.PlanNode> children = node.getChildren();

            int totalWidth = 0;
            int[] childWidths = new int[children.size()];
            for (int i = 0; i < children.size(); i++) {
                childWidths[i] = calculateSubtreeWidth(children.get(i));
                totalWidth += childWidths[i];
            }

            int startX = x - (totalWidth / 2);
            int childY = nextY + 30; // 30px vertical gap between layers

            int currentX = startX;
            for (int i = 0; i < children.size(); i++) {
                int childWidth = childWidths[i];
                int childX = currentX + (childWidth / 2);

                // Orthogonal lines
                int midY = nextY + 15;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 5); // ensure lines are above background
                guiGraphics.fill(x, nextY, x + 2, midY, 0xFF555555);
                guiGraphics.fill(Math.min(x, childX), midY, Math.max(x, childX) + 2, midY + 2, 0xFF555555);
                guiGraphics.fill(childX, midY, childX + 2, childY, 0xFF555555);
                guiGraphics.pose().popPose();

                drawNodeTree(guiGraphics, children.get(i), childX, childY, mouseX, mouseY);

                currentX += childWidth;
            }
        }
    }
    
    private void drawNodeOverlays(GuiGraphics guiGraphics, CraftingPlan.PlanNode node, int x, int y, int mouseX, int mouseY) {
        if (node == null) return;
        
        // Local mouse for JEI
        int localMouseX = (int)((mouseX - panX) / zoom);
        int localMouseY = (int)((mouseY - panY) / zoom);
        
        IRecipeLayoutDrawable<?> drawable = getJeiDrawable(node);
        if (drawable != null) {
            drawable.drawOverlays(guiGraphics, localMouseX, localMouseY);
        }
        
        if (!node.isLeaf()) {
            List<CraftingPlan.PlanNode> children = node.getChildren();
            int totalWidth = 0;
            int[] childWidths = new int[children.size()];
            for (int i = 0; i < children.size(); i++) {
                childWidths[i] = calculateSubtreeWidth(children.get(i));
                totalWidth += childWidths[i];
            }

            int startX = x - (totalWidth / 2);
            int dropY = y + 32;
            int nextY = dropY + 16;
            if (drawable != null) nextY += drawable.getRect().getHeight() + 10;
            else nextY += 10;
            int childY = nextY + 30;

            int currentX = startX;
            for (int i = 0; i < children.size(); i++) {
                int childWidth = childWidths[i];
                int childX = currentX + (childWidth / 2);
                drawNodeOverlays(guiGraphics, children.get(i), childX, childY, mouseX, mouseY);
                currentX += childWidth;
            }
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 2) + "..";
    }

    private int calculateSubtreeWidth(CraftingPlan.PlanNode node) {
        if (node == null) return 0;
        
        int catTextWidth = this.font.width(node.getCategoryName());
        int catWidth = Math.max(100, catTextWidth + 10);
        
        String recName = "Recipe";
        if (node.getRecipeId() != null && node.getRecipeId().contains(":")) {
            String[] parts = node.getRecipeId().split(":");
            recName = parts[parts.length - 1];
        }
        if (node.getCategoryName().equals("Raw") || node.getCategoryName().equals("Select Category...")) {
            recName = node.getCategoryName();
        }
        int recTextWidth = this.font.width(recName);
        int recWidth = Math.max(100, recTextWidth + 10);
        
        int myWidth = Math.max(240, catWidth + recWidth + 10);
        
        IRecipeLayoutDrawable<?> drawable = getJeiDrawable(node);
        if (drawable != null) {
            myWidth = Math.max(myWidth, drawable.getRect().getWidth() + 20);
        }
        
        if (node.isLeaf()) return myWidth;

        int total = 0;
        for (CraftingPlan.PlanNode child : node.getChildren()) {
            total += calculateSubtreeWidth(child);
        }
        return Math.max(myWidth, total);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.quantityField != null && this.quantityField.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            try {
                int newVal = Integer.parseInt(this.quantityField.getValue());
                if (newVal > 0) {
                    targetQuantity = newVal;
                    refreshPlan();
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeDropdownNode != null) {
            // Check if clicked inside dropdown
            int rowHeight = 15;
            int dropH = currentDropdownOptions.size() * rowHeight;
            if (mouseX >= dropdownX && mouseX <= dropdownX + 250 && mouseY >= dropdownY && mouseY <= dropdownY + dropH) {
                int index = (int)((mouseY - dropdownY) / rowHeight);
                if (index >= 0 && index < currentDropdownOptions.size()) {
                    RecipePlanner.RecipeOption opt = currentDropdownOptions.get(index);
                    RecipePlanner.setPreference(RecipePlanner.getUniqueId(activeDropdownNode.getOutput()), opt.recipeId);
                    refreshPlan();
                    return true;
                }
            }
            // Clicked outside, close dropdown
            activeDropdownNode = null;
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0 && !plans.isEmpty()) {
            double worldX = (mouseX - panX) / zoom;
            double worldY = (mouseY - panY) / zoom;
            
            int sidebarWidth = Math.max(200, this.width / 4);
            if (mouseX > sidebarWidth) {
                if (checkNodeClicks(plans.get(0).getRootNode(), (this.width - sidebarWidth) / 2, 50, worldX, worldY)) {
                    return true;
                }
                isDragging = true;
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkNodeClicks(CraftingPlan.PlanNode node, int x, int y, double worldX, double worldY) {
        if (node == null) return false;
        
        int catTextWidth = this.font.width(node.getCategoryName());
        int catWidth = Math.max(100, catTextWidth + 10);
        
        String recName = "Recipe";
        if (node.getRecipeId() != null && node.getRecipeId().contains(":")) {
            String[] parts = node.getRecipeId().split(":");
            recName = parts[parts.length - 1];
        }
        if (node.getCategoryName().equals("Raw") || node.getCategoryName().equals("Select Category...")) {
            recName = node.getCategoryName();
        }
        int recTextWidth = this.font.width(recName);
        int recWidth = Math.max(100, recTextWidth + 10);
        
        int dropY = y + 32;
        int catX = x - catWidth - 2;
        int recX = x + 2;
        
        // Check Category Click
        if (worldX >= catX && worldX <= catX + catWidth && worldY >= dropY && worldY <= dropY + 12) {
            openDropdown(node, true, (int)(catX * zoom + panX), (int)((dropY + 14) * zoom + panY));
            return true;
        }
        
        // Check Recipe Click
        if (worldX >= recX && worldX <= recX + recWidth && worldY >= dropY && worldY <= dropY + 12) {
            openDropdown(node, false, (int)(recX * zoom + panX), (int)((dropY + 14) * zoom + panY));
            return true;
        }
        
        // Inline JEI interaction?
        IRecipeLayoutDrawable<?> drawable = getJeiDrawable(node);
        int nextY = dropY + 16;
        if (drawable != null) {
            nextY += drawable.getRect().getHeight() + 15;
        } else {
            nextY += 15;
        }
        
        if (!node.isLeaf()) {
            List<CraftingPlan.PlanNode> children = node.getChildren();
            int totalWidth = 0;
            int[] childWidths = new int[children.size()];
            for (int i = 0; i < children.size(); i++) {
                childWidths[i] = calculateSubtreeWidth(children.get(i));
                totalWidth += childWidths[i];
            }
            int startX = x - (totalWidth / 2);
            int childY = nextY + 30;
            int currentX = startX;
            for (int i = 0; i < children.size(); i++) {
                int childWidth = childWidths[i];
                int childX = currentX + (childWidth / 2);
                if (checkNodeClicks(children.get(i), childX, childY, worldX, worldY)) return true;
                currentX += childWidth;
            }
        }
        return false;
    }
    
    private void openDropdown(CraftingPlan.PlanNode node, boolean isCategory, int x, int y) {
        this.activeDropdownNode = node;
        this.isCategoryDropdown = isCategory;
        this.dropdownX = x;
        this.dropdownY = y;
        
        List<RecipePlanner.RecipeOption> allOptions = RecipePlanner.getAvailableOptions(node.getOutput());
        this.currentDropdownOptions.clear();
        
        if (isCategory) {
            // Group by category, show 1 option per category
            Set<String> seenCats = new HashSet<>();
            for (RecipePlanner.RecipeOption opt : allOptions) {
                if (!seenCats.contains(opt.name)) {
                    seenCats.add(opt.name);
                    this.currentDropdownOptions.add(opt); // opt.name is category
                }
            }
        } else {
            // Show recipes ONLY for the current category
            String currentCat = node.getCategoryName();
            for (RecipePlanner.RecipeOption opt : allOptions) {
                if (opt.name.equals(currentCat) || opt.name.equals("Raw")) {
                    this.currentDropdownOptions.add(opt);
                }
            }
        }
    }
    
    private void renderDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int rowHeight = 15;
        int width = 250;
        int height = currentDropdownOptions.size() * rowHeight;
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 800); // Very high Z
        
        guiGraphics.fill(dropdownX, dropdownY, dropdownX + width, dropdownY + height, 0xEE111111);
        guiGraphics.renderOutline(dropdownX, dropdownY, width, height, 0xFFAAAAAA);
        
        int cy = dropdownY;
        for (RecipePlanner.RecipeOption opt : currentDropdownOptions) {
            boolean hovered = mouseX >= dropdownX && mouseX <= dropdownX + width && mouseY >= cy && mouseY <= cy + rowHeight;
            if (hovered) {
                guiGraphics.fill(dropdownX + 1, cy, dropdownX + width - 1, cy + rowHeight, 0x55FFFFFF);
            }
            
            String text = isCategoryDropdown ? opt.name : opt.recipeId;
            if (!isCategoryDropdown && text.contains(":")) {
                String[] parts = text.split(":");
                text = parts[parts.length - 1];
            }
            if (!isCategoryDropdown && opt.name.equals("Raw")) {
                text = "Raw";
            }
            
            guiGraphics.drawString(this.font, truncate(text, 35), dropdownX + 5, cy + 4, hovered ? 0xFFFFAA : 0xFFFFFF);
            cy += rowHeight;
        }
        
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            zoom *= 1.1;
        } else if (scrollY < 0) {
            zoom /= 1.1;
        }
        return true;
    }

    private String getIngredientName(ITypedIngredient<?> typedIng) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientHelper helper = jeiRuntime.getIngredientManager().getIngredientHelper(typedIng.getType());
            return helper.getDisplayName(typedIng.getIngredient());
        }
        return "Unknown";
    }

    private void renderIngredient(GuiGraphics guiGraphics, ITypedIngredient<?> typedIng, int x, int y) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientRenderer renderer = jeiRuntime.getIngredientManager().getIngredientRenderer(typedIng.getType());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 150); // Ensure items render above background
            renderer.render(guiGraphics, typedIng.getIngredient(), x, y);
            guiGraphics.pose().popPose();
        }
    }

    private void renderIngredientDecorations(GuiGraphics guiGraphics, ITypedIngredient<?> typedIng, int x, int y, String text) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400); // Elevate above items
        guiGraphics.drawString(this.font, text, x + 17 - this.font.width(text), y + 9, 16777215, true);
        guiGraphics.pose().popPose();
    }

    private void renderIngredientTooltip(GuiGraphics guiGraphics, ITypedIngredient<?> typedIng, int mouseX, int mouseY) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientRenderer renderer = jeiRuntime.getIngredientManager().getIngredientRenderer(typedIng.getType());
            List<Component> tooltip = renderer.getTooltip(typedIng.getIngredient(), net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    public boolean isPauseScreen() {
        return false;
    }
}
