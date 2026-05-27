import os
import re

def process_crafting_plan():
    with open('src/main/java/com/craftanyway/planning/CraftingPlan.java', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Imports
    content = content.replace('import net.minecraft.world.item.ItemStack;', 'import net.minecraft.world.item.ItemStack;\nimport mezz.jei.api.ingredients.ITypedIngredient;')
    
    # Class fields
    content = content.replace('private final ItemStack target;', 'private final ITypedIngredient<?> target;\n    private final long targetAmount;')
    content = content.replace('public CraftingPlan(ItemStack target, PlanNode rootNode) {', 'public CraftingPlan(ITypedIngredient<?> target, long targetAmount, PlanNode rootNode) {')
    content = content.replace('this.target = target;', 'this.target = target;\n        this.targetAmount = targetAmount;')
    
    # Getters
    content = content.replace('public ItemStack getTarget() {', 'public ITypedIngredient<?> getTarget() {')
    
    # StepItem
    content = content.replace('public final ItemStack stack;', 'public final ITypedIngredient<?> stack;\n        public final String uniqueId;')
    content = content.replace('public StepItem(ItemStack stack, int needed, int have) {', 'public StepItem(ITypedIngredient<?> stack, String uniqueId, int needed, int have) {')
    content = content.replace('this.stack = stack;', 'this.stack = stack;\n            this.uniqueId = uniqueId;')
    
    # PlanNode fields
    content = content.replace('private final ItemStack output;', 'private final ITypedIngredient<?> output;\n        private final long amount;')
    
    # Constructors of PlanNode
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {')
    content = content.replace('this(output, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, null, null);', 'this(output, amount, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, null, null);')
    
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {')
    content = content.replace('this(output, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, rawRecipe, recipeCategory);', 'this(output, amount, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, rawRecipe, recipeCategory);')
    
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ItemStack[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ITypedIngredient<?>[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {')
    content = content.replace('this.output = output;', 'this.output = output;\n            this.amount = amount;')
    
    # Getters
    content = content.replace('public ItemStack getOutput() {', 'public ITypedIngredient<?> getOutput() {\n            return output;\n        }\n\n        public long getAmount() {')
    
    # tagOptions
    content = content.replace('private final ItemStack[] tagOptions;', 'private final ITypedIngredient<?>[] tagOptions;')
    content = content.replace('public ItemStack[] getTagOptions() {', 'public ITypedIngredient<?>[] getTagOptions() {')
    
    with open('src/main/java/com/craftanyway/planning/CraftingPlan.java', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_crafting_plan()
