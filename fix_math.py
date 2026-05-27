import os

def fix_math():
    with open('src/main/java/com/craftanyway/planning/CraftingPlan.java', 'r', encoding='utf-8') as f:
        c = f.read()

    # Add recipeYield to PlanNode constructors
    c = c.replace('private final int craftsNeeded;', 'private final int craftsNeeded;\n        private final long recipeYield;')
    c = c.replace('public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId) {')
    c = c.replace('this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, null, null);', 'this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, recipeYield, children, cost, recipeId, null, null, null, null);')
    c = c.replace('public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {')
    c = c.replace('this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, rawRecipe, recipeCategory);', 'this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, recipeYield, children, cost, recipeId, null, null, rawRecipe, recipeCategory);')
    c = c.replace('public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ITypedIngredient<?>[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId, ITypedIngredient<?>[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {')
    c = c.replace('this.craftsNeeded = craftsNeeded;', 'this.craftsNeeded = craftsNeeded;\n            this.recipeYield = recipeYield;')

    # Add getters
    c = c.replace('public int getCraftsNeeded() { return craftsNeeded; }', 'public int getCraftsNeeded() { return craftsNeeded; }\n        public long getRecipeYield() { return recipeYield; }')

    # Replace old aggregateSteps calculation
    c = c.replace('long recipeYield = 1;\n        if (node.getRecipe() != null) {\n            Minecraft mc = Minecraft.getInstance();\n            if (mc.level != null) {\n                try {\n                    ItemStack result = node.getRecipe().getResultItem(mc.level.registryAccess());\n                    if (!result.isEmpty()) {\n                        recipeYield = result.getCount();\n                    }\n                } catch (Exception e) {}\n            }\n        }\n        if (recipeYield <= 0) recipeYield = 1;', 'long recipeYield = node.getRecipeYield();\n        if (recipeYield <= 0) recipeYield = 1;')
    
    # Fix recursive call multiplication
    c = c.replace('aggregateSteps(child, child.getAmount() * craftsNeeded, depth + 1, remainingInv, stepMap, inv);', 'long originalCraftsNeeded = Math.max(1, node.getCraftsNeeded());\n            long childAmountPerCraft = (long) Math.ceil((double) child.getAmount() / originalCraftsNeeded);\n            aggregateSteps(child, childAmountPerCraft * craftsNeeded, depth + 1, remainingInv, stepMap, inv);')

    with open('src/main/java/com/craftanyway/planning/CraftingPlan.java', 'w', encoding='utf-8') as f:
        f.write(c)

    # Now fix RecipePlanner.java
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
        rp = f.read()

    # Update instantiation of PlanNode
    rp = rp.replace('new CraftingPlan.PlanNode(target, targetAmount, uid, "Select Category...", false, null, 1, new ArrayList<>(), 0, "")', 'new CraftingPlan.PlanNode(target, targetAmount, uid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, "")')
    rp = rp.replace('new CraftingPlan.PlanNode(target, requestedAmount, uid, "Raw", false, null, 1, new ArrayList<>(), 0, "craftanyway:raw")', 'new CraftingPlan.PlanNode(target, requestedAmount, uid, "Raw", false, null, 1, 1L, new ArrayList<>(), 0, "craftanyway:raw")')
    rp = rp.replace('new CraftingPlan.PlanNode(target, requestedAmount, uid, "Ignore", false, null, 1, new ArrayList<>(), 0, "craftanyway:ignore")', 'new CraftingPlan.PlanNode(target, requestedAmount, uid, "Ignore", false, null, 1, 1L, new ArrayList<>(), 0, "craftanyway:ignore")')
    rp = rp.replace('new CraftingPlan.PlanNode(target, requestedAmount, uid, "Select Category...", false, null, 1, new ArrayList<>(), 0, "")', 'new CraftingPlan.PlanNode(target, requestedAmount, uid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, "")')
    rp = rp.replace('new CraftingPlan.PlanNode(groupedTarget, totalRequired, childUid, "Select Category...", false, null, 1, new ArrayList<>(), 0, "")', 'new CraftingPlan.PlanNode(groupedTarget, totalRequired, childUid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, "")')
    rp = rp.replace('new CraftingPlan.PlanNode(candidate.getOutput(), candidate.getAmount(), candidate.getUniqueId(), candidate.getCategoryName(), candidate.isCraftingTable(), candidate.getRecipe(), candidate.getCraftsNeeded(), candidate.getChildren(), 0, candidate.getRecipeId(), items, tagSignature, candidate.getRawRecipe(), candidate.getRecipeCategory())', 'new CraftingPlan.PlanNode(candidate.getOutput(), candidate.getAmount(), candidate.getUniqueId(), candidate.getCategoryName(), candidate.isCraftingTable(), candidate.getRecipe(), candidate.getCraftsNeeded(), candidate.getRecipeYield(), candidate.getChildren(), 0, candidate.getRecipeId(), items, tagSignature, candidate.getRawRecipe(), candidate.getRecipeCategory())')
    rp = rp.replace('new CraftingPlan.PlanNode(target, requestedAmount, uid, category.getTitle().getString(), category.getRecipeType().getUid().getPath().equals("crafting"), recipe, craftsNeeded, children, 0, recipeId, recipeObj, category)', 'new CraftingPlan.PlanNode(target, requestedAmount, uid, category.getTitle().getString(), category.getRecipeType().getUid().getPath().equals("crafting"), recipe, craftsNeeded, recipeYield, children, 0, recipeId, recipeObj, category)')

    rp = rp.replace('''long recipeYield = getIngredientAmount(target.getIngredient());
                    if (recipeYield <= 0) recipeYield = 1;
                    
                    int craftsNeeded = (int) Math.ceil((double) requestedAmount / recipeYield);
                    
                    Map<String, ITypedIngredient<?>> groupedIngredients = new HashMap<>();
                    Map<String, ITypedIngredient<?>[]> groupedOptions = new HashMap<>();
                    Map<String, Long> groupedAmounts = new HashMap<>();

                    Optional<mezz.jei.api.gui.IRecipeLayoutDrawable<Object>> opt = recipeManager.createRecipeLayoutDrawable(
                        (IRecipeCategory<Object>) category, recipeObj, focusGroup
                    );''', '''Optional<mezz.jei.api.gui.IRecipeLayoutDrawable<Object>> opt = recipeManager.createRecipeLayoutDrawable(
                        (IRecipeCategory<Object>) category, recipeObj, focusGroup
                    );
                    
                    long recipeYield = 1;
                    if (opt.isPresent()) {
                        for (mezz.jei.api.gui.ingredient.IRecipeSlotView view : opt.get().getRecipeSlotsView().getSlotViews()) {
                            if (view.getRole() == mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT) {
                                Optional<ITypedIngredient<?>> outIng = view.getDisplayedIngredient();
                                if (outIng.isPresent() && getUniqueId(outIng.get()).equals(getUniqueId(target))) {
                                    long amt = getIngredientAmount(outIng.get().getIngredient());
                                    if (amt > 0) recipeYield = amt;
                                }
                            }
                        }
                    }
                    
                    int craftsNeeded = (int) Math.ceil((double) requestedAmount / recipeYield);
                    
                    Map<String, ITypedIngredient<?>> groupedIngredients = new HashMap<>();
                    Map<String, ITypedIngredient<?>[]> groupedOptions = new HashMap<>();
                    Map<String, Long> groupedAmounts = new HashMap<>();''')

    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
        f.write(rp)

if __name__ == "__main__":
    fix_math()
