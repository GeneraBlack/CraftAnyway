import os

def fix():
    # ShoppingListOverlay
    with open('src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java', 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('ItemStack stack = stepItem.stack;', 'mezz.jei.api.ingredients.ITypedIngredient<?> stack = stepItem.ingredient;')
    c = c.replace('int have = stepItem.have;', 'int have = (int) stepItem.have;')
    c = c.replace('int needed = stepItem.needed;', 'int needed = (int) stepItem.needed;')
    c = c.replace('guiGraphics.renderItem(stack, x + 2, y + 2);', 'var jeiRuntime = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime();\n                if (jeiRuntime != null) {\n                    @SuppressWarnings("unchecked")\n                    mezz.jei.api.ingredients.IIngredientRenderer<Object> renderer = (mezz.jei.api.ingredients.IIngredientRenderer<Object>) jeiRuntime.getIngredientManager().getIngredientRenderer(stack.getType());\n                    renderer.render(guiGraphics, x + 2, y + 2, stack.getIngredient());\n                }')
    with open('src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # Keybinds
    with open('src/main/java/com/craftanyway/client/Keybinds.java', 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('RecipePlanner.plan(ingredientUnderMouse);', 'var opt = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().createTypedIngredient(ingredientUnderMouse);\n                if (opt.isPresent()) RecipePlanner.plan(opt.get(), ingredientUnderMouse.getCount());')
    with open('src/main/java/com/craftanyway/client/Keybinds.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # CraftExecutor
    with open('src/main/java/com/craftanyway/execution/CraftExecutor.java', 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace('flattenPlan(plan.getRootNode(), plan.getRootNode().getOutput().getCount(), remainingInv, nodesToCraft, screen);', 'flattenPlan(plan.getRootNode(), (int)plan.getRootNode().getAmount(), remainingInv, nodesToCraft, screen);')
    c = c.replace('ItemStack output = node.getOutput();', 'Object outIng = node.getOutput().getIngredient();\n        if (!(outIng instanceof ItemStack)) return;\n        ItemStack output = (ItemStack) outIng;')
    c = c.replace('+ currentNode.getOutput().getHoverName().getString()', '+ ((ItemStack)currentNode.getOutput().getIngredient()).getHoverName().getString()')
    with open('src/main/java/com/craftanyway/execution/CraftExecutor.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # RecipePlanner generics
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
        c = f.read()
    
    # Generic warnings fixes
    c = c.replace('IIngredientHelper helper = jeiRuntime.getIngredientManager().getIngredientHelper(target.getType());\n                String uid = helper.getUniqueId(target.getIngredient(), UidContext.Ingredient);', 'String uid = getUniqueId(target);')
    c = c.replace('IIngredientHelper helper = ingredientManager.getIngredientHelper(target.getType());\n        String targetKey = helper.getUniqueId(target.getIngredient(), UidContext.Ingredient);', 'String targetKey = getUniqueId(target);')
    c = c.replace('ingredientManager.getIngredientHelper(ti.getType()).getUniqueId(ti.getIngredient(), UidContext.Ingredient)', 'getUniqueId(ti)')
    c = c.replace('ingredientManager.getIngredientHelper(chosenOpt.getType()).getUniqueId(chosenOpt.getIngredient(), UidContext.Ingredient)', 'getUniqueId(chosenOpt)')
    c = c.replace('ingredientManager.getIngredientHelper(groupedTarget.getType()).getUniqueId(groupedTarget.getIngredient(), UidContext.Ingredient)', 'getUniqueId(groupedTarget)')
    
    helper_method = """
    @SuppressWarnings("unchecked")
    public static <T> String getUniqueId(ITypedIngredient<T> typedIng) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientHelper<T> helper = jeiRuntime.getIngredientManager().getIngredientHelper(typedIng.getType());
            return helper.getUniqueId(typedIng.getIngredient(), UidContext.Ingredient);
        }
        return "";
    }
"""
    c = c.replace('public static void plan(ITypedIngredient<?> target, long targetAmount) {', helper_method + '\n    public static void plan(ITypedIngredient<?> target, long targetAmount) {')

    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
        f.write(c)

if __name__ == "__main__":
    fix()
