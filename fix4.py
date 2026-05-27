import os

def fix4():
    # RecipePlanner
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('import mezz.jei.api.IJeiRuntime;', 'import mezz.jei.api.runtime.IJeiRuntime;')
    c = c.replace('Map<String, String> tagPreferences = ModPreferences.getTagPreferences();', 'Map<String, String> tagPreferences = new HashMap<>();')
    c = c.replace('isCraftingTable, recipe', 'category.getRecipeType().getUid().getPath().equals("crafting"), recipe')
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # PlanScreen
    with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('ItemStack stack = stepItem.stack.copy();', 'ITypedIngredient<?> stack = stepItem.ingredient;')
    c = c.replace('RecipePlanner.userPreferences.put(activeDropdownNode.getOutput().getItem().toString(), opt.recipeId);', 'RecipePlanner.setPreference(RecipePlanner.getUniqueId(activeDropdownNode.getOutput()), opt.recipeId);')
    c = c.replace('RecipePlanner.getAvailableOptions(node.getOutput().copy())', 'RecipePlanner.getAvailableOptions(node.getOutput())')
    
    # Fix renderer
    c = c.replace('renderer.render(guiGraphics, x, y, typedIng.getIngredient());', 'guiGraphics.pose().pushPose();\n            guiGraphics.pose().translate(x, y, 0);\n            renderer.render(guiGraphics, typedIng.getIngredient());\n            guiGraphics.pose().popPose();')
    
    with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # ShoppingListOverlay
    with open('src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java', 'r', encoding='utf-8') as f:
        c = f.read()
    
    c = c.replace('guiGraphics.renderItem(stack, x, ry);', 'var renderer = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().getIngredientRenderer(stack.getType());\n                guiGraphics.pose().pushPose();\n                guiGraphics.pose().translate(x, ry, 0);\n                ((mezz.jei.api.ingredients.IIngredientRenderer<Object>)renderer).render(guiGraphics, stack.getIngredient());\n                guiGraphics.pose().popPose();')
    
    c = c.replace('guiGraphics.renderItemDecorations(mc.font, stack, x, ry);', '// no simple decorations for typed ingredients')
    
    with open('src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java', 'w', encoding='utf-8') as f:
        f.write(c)

if __name__ == "__main__":
    fix4()
