import re

def main():
    with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r', encoding='utf-8') as f:
        content = f.read()

    # Imports
    content = content.replace('import net.minecraft.world.item.ItemStack;', 'import net.minecraft.world.item.ItemStack;\nimport mezz.jei.api.ingredients.ITypedIngredient;\nimport mezz.jei.api.ingredients.IIngredientRenderer;\nimport mezz.jei.api.ingredients.IIngredientHelper;')

    # Fields
    content = content.replace('private ItemStack targetItem;', 'private ITypedIngredient<?> targetIngredient;\n    private long targetQuantity;')
    content = content.replace('this.targetItem = plans.get(0).getTarget().copy();', 'this.targetIngredient = plans.get(0).getTarget();\n            this.targetQuantity = plans.get(0).getTargetAmount();')
    
    # Init
    content = content.replace('if (targetItem != null) {', 'if (targetIngredient != null) {')
    content = content.replace('if (targetItem.getCount() > 1) {\n                    targetItem.shrink(1);', 'if (targetQuantity > 1) {\n                    targetQuantity--;')
    content = content.replace('if (targetItem.getCount() < 999) {\n                    targetItem.grow(1);', 'if (targetQuantity < 999999) {\n                    targetQuantity++;')
    
    # refreshPlan
    content = content.replace('RecipePlanner.plan(targetItem);', 'RecipePlanner.plan(targetIngredient, targetQuantity);')
    
    # render
    content = content.replace('guiGraphics.drawString(this.font, "Qty: " + (targetItem != null ? targetItem.getCount() : 1), 20, 26, 0xFFFFFF);', 'guiGraphics.drawString(this.font, "Qty: " + (targetIngredient != null ? targetQuantity : 1), 20, 26, 0xFFFFFF);')
    
    # aggregateSteps render
    content = content.replace('guiGraphics.drawString(this.font, "Step " + step.stepNumber + ":", 10, ry, 0xFFAAAAAA);', 'guiGraphics.drawString(this.font, "Step " + step.stepNumber + ":", 10, ry, 0xFFAAAAAA);')
    
    # rendering step items
    content = content.replace('guiGraphics.renderItem(item.stack, rx, ry);', 'renderIngredient(guiGraphics, item.stack, rx, ry);')
    content = content.replace('guiGraphics.renderItemDecorations(this.font, item.stack, rx, ry, item.have + "/" + item.needed);', 'renderIngredientDecorations(guiGraphics, item.stack, rx, ry, item.have + "/" + item.needed);')

    # DrawNodeTree
    content = content.replace('guiGraphics.drawCenteredString(this.font, node.getOutput().getHoverName().getString(), x, y, 0xFFFFFF);', 'guiGraphics.drawCenteredString(this.font, getIngredientName(node.getOutput()), x, y, 0xFFFFFF);')
    content = content.replace('guiGraphics.renderItem(node.getOutput(), x - 8, y + 12);', 'renderIngredient(guiGraphics, node.getOutput(), x - 8, y + 12);')
    content = content.replace('guiGraphics.renderItemDecorations(this.font, node.getOutput(), x - 8, y + 12, String.valueOf(node.getOutput().getCount()));', 'renderIngredientDecorations(guiGraphics, node.getOutput(), x - 8, y + 12, String.valueOf(node.getAmount()));')

    # activeDropdownNode hover
    content = content.replace('guiGraphics.renderTooltip(this.font, node.getOutput(), localMouseX, localMouseY);', 'renderIngredientTooltip(guiGraphics, node.getOutput(), localMouseX, localMouseY);')

    # Dropdown render options
    content = content.replace('guiGraphics.renderItem(preview, px, pY + 2);', 'renderIngredient(guiGraphics, preview, px, pY + 2);')

    # Add helper methods
    helpers = """
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
            renderer.render(guiGraphics, x, y, typedIng.getIngredient());
        }
    }

    private void renderIngredientDecorations(GuiGraphics guiGraphics, ITypedIngredient<?> typedIng, int x, int y, String text) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);
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
"""
    content = content.replace('public boolean isPauseScreen()', helpers + '\n    @Override\n    public boolean isPauseScreen()')

    with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    main()
