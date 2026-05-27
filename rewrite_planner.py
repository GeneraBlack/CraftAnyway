import re

def main():
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
        content = f.read()

    # Imports
    content = content.replace('import net.minecraft.world.item.ItemStack;', 'import net.minecraft.world.item.ItemStack;\nimport mezz.jei.api.ingredients.ITypedIngredient;\nimport mezz.jei.api.ingredients.subtypes.UidContext;\nimport mezz.jei.api.ingredients.IIngredientHelper;\nimport java.lang.reflect.Method;')

    # Plan
    content = content.replace('public static void plan(ItemStack target) {', 'public static void plan(ITypedIngredient<?> target, long targetAmount) {')
    content = content.replace('if (target == null || target.isEmpty()) return;', 'if (target == null) return;')
    content = content.replace('List<CraftingPlan.PlanNode> rootNodes = buildNodesForTarget(target, inv);', 'List<CraftingPlan.PlanNode> rootNodes = buildNodesForTarget(target, targetAmount, inv);')
    content = content.replace('CraftingPlan.PlanNode rootNode = new CraftingPlan.PlanNode(target, "Select Category...", false, null, target.getCount(), new ArrayList<>(), 0, "");', 'String uid = jeiRuntime.getIngredientManager().getIngredientHelper(target.getType()).getUniqueId(target.getIngredient(), UidContext.Ingredient);\n                CraftingPlan.PlanNode rootNode = new CraftingPlan.PlanNode(target, targetAmount, uid, "Select Category...", false, null, 1, new ArrayList<>(), 0, "");')
    content = content.replace('currentPlan = new CraftingPlan(target, rootNode);', 'currentPlan = new CraftingPlan(target, targetAmount, rootNode);')

    # Options
    content = content.replace('public static List<RecipeOption> getAvailableOptions(ItemStack target) {', 'public static List<RecipeOption> getAvailableOptions(ITypedIngredient<?> target) {')
    
    # buildNodesForTarget
    content = content.replace('private static List<CraftingPlan.PlanNode> buildNodesForTarget(ItemStack target, Inventory inv) {', 'private static List<CraftingPlan.PlanNode> buildNodesForTarget(ITypedIngredient<?> target, long requestedAmount, Inventory inv) {')
    content = content.replace('if (target == null || target.isEmpty()) return new ArrayList<>();', 'if (target == null) return new ArrayList<>();')
    
    content = content.replace('String targetKey = target.getItem().toString();', 'IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();\n        if (jeiRuntime == null) return new ArrayList<>();\n        mezz.jei.api.runtime.IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();\n        IIngredientHelper helper = ingredientManager.getIngredientHelper(target.getType());\n        String targetKey = helper.getUniqueId(target.getIngredient(), UidContext.Ingredient);\n        String uid = targetKey;')
    content = content.replace('Map<String, String> tagPreferences = ModPreferences.getTagPreferences();\n        String pref = itemPreferences.get(targetKey);', 'Map<String, String> tagPreferences = ModPreferences.getTagPreferences();\n        String pref = itemPreferences.get(targetKey);\n')
    
    content = content.replace('nodes.add(new CraftingPlan.PlanNode(target, "Raw", false, null, target.getCount(), new ArrayList<>(), 0, "craftanyway:raw"));', 'nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, "Raw", false, null, 1, new ArrayList<>(), 0, "craftanyway:raw"));')
    content = content.replace('nodes.add(new CraftingPlan.PlanNode(target, "Ignore", false, null, target.getCount(), new ArrayList<>(), 0, "craftanyway:ignore"));', 'nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, "Ignore", false, null, 1, new ArrayList<>(), 0, "craftanyway:ignore"));')
    
    content = content.replace('IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();\n        if (jeiRuntime == null) return nodes;\n\n        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();\n        mezz.jei.api.runtime.IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();', 'IRecipeManager recipeManager = jeiRuntime.getRecipeManager();')
    
    content = content.replace('var typedOpt = ingredientManager.createTypedIngredient(target);', 'var typedOpt = java.util.Optional.of(target);')
    content = content.replace('if (typedOpt.isPresent()) {\n            focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));\n        }', 'focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, target));')
    
    content = content.replace('int requestedAmount = target.getCount();', '// requestedAmount is passed in')
    content = content.replace('java.util.Map<String, ItemStack> groupedIngredients = new java.util.HashMap<>();', 'java.util.Map<String, ITypedIngredient<?>> groupedIngredients = new java.util.HashMap<>();')
    content = content.replace('java.util.Map<String, ItemStack[]> groupedOptions = new java.util.HashMap<>();', 'java.util.Map<String, ITypedIngredient<?>[]> groupedOptions = new java.util.HashMap<>();')
    content = content.replace('java.util.Map<String, Long> groupedAmounts = new java.util.HashMap<>();', 'java.util.Map<String, Long> groupedAmounts = new java.util.HashMap<>();') # actually we need to add this
    
    # We must completely rewrite the groupedIngredients logic
    
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    main()
