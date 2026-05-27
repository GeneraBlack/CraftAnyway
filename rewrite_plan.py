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
    content = content.replace('private final ItemStack output;', 'private final ITypedIngredient<?> output;\n        private final long amount;\n        private final String uniqueId;')
    
    # Constructors of PlanNode
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {')
    content = content.replace('this(output, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, null, null);', 'this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, null, null);')
    
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {')
    content = content.replace('this(output, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, rawRecipe, recipeCategory);', 'this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null, rawRecipe, recipeCategory);')
    
    content = content.replace('public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ItemStack[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {', 'public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ITypedIngredient<?>[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {')
    content = content.replace('this.output = output;', 'this.output = output;\n            this.amount = amount;\n            this.uniqueId = uniqueId;')
    
    # tagOptions
    content = content.replace('private final ItemStack[] tagOptions;', 'private final ITypedIngredient<?>[] tagOptions;')
    content = content.replace('public ItemStack[] getTagOptions() {', 'public ITypedIngredient<?>[] getTagOptions() {')

    # Getters
    content = content.replace('public ItemStack getOutput() {', 'public ITypedIngredient<?> getOutput() {\n            return output;\n        }\n\n        public long getAmount() {\n            return amount;\n        }\n\n        public String getUniqueId() {\n            return uniqueId;\n        }\n\n        // Dummy method for old references')

    # aggregateSteps mapping
    content = content.replace('Map<Item, Integer> remainingInv', 'Map<String, Integer> remainingInv')
    content = content.replace('Item item = output.getItem();', 'String key = node.getUniqueId();')
    content = content.replace('remainingInv.getOrDefault(item, 0)', 'remainingInv.getOrDefault(key, 0)')
    content = content.replace('remainingInv.put(item, haveAvailable - used)', 'remainingInv.put(key, haveAvailable - used)')
    content = content.replace('String key = item.getDescriptionId();', '// key already unique id')
    content = content.replace('countItemInInventory(inv, item)', '0 // FIXME: check inventory for ITypedIngredient')
    
    content = content.replace('remainingInv.put(stack.getItem(), remainingInv.getOrDefault(stack.getItem(), 0) + stack.getCount());', '// FIXME: initialize inventory tracking')

    content = content.replace('step.items.put(key, new StepItem(output, existing.needed + neededAmount, actualHave));', 'step.items.put(key, new StepItem(output, key, existing.needed + neededAmount, actualHave));')
    content = content.replace('step.items.put(key, new StepItem(output, neededAmount, actualHave));', 'step.items.put(key, new StepItem(output, key, neededAmount, actualHave));')

    content = content.replace('aggregateSteps(child, child.getOutput().getCount(),', 'aggregateSteps(child, (int)child.getAmount(),')
    content = content.replace('aggregateSteps(rootNode, rootNode.getOutput().getCount(),', 'aggregateSteps(rootNode, (int)rootNode.getAmount(),')

    with open('src/main/java/com/craftanyway/planning/CraftingPlan.java', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_crafting_plan()
