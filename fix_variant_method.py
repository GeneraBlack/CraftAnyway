import os
filepath = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
with open(filepath, 'r') as f:
    content = f.read()

method = '''
    private void openVariantDropdown(CraftingPlan.PlanNode node, int x, int y) {
        this.activeDropdownNode = node;
        this.isVariantDropdown = true;
        this.isCategoryDropdown = false;
        this.dropdownX = x;
        this.dropdownY = y;
        
        this.currentDropdownOptions.clear();
        if (node.getTagOptions() != null) {
            for (mezz.jei.api.ingredients.ITypedIngredient<?> ti : node.getTagOptions()) {
                this.currentDropdownOptions.add(new com.craftanyway.planning.RecipePlanner.RecipeOption(com.craftanyway.planning.RecipePlanner.getUniqueId(ti), getIngredientName(ti), 0, new java.util.ArrayList<>()));
            }
        }
    }
'''

content = content.replace('private void openDropdown(CraftingPlan.PlanNode node, boolean isCategory, int x, int y) {', method + '\n    private void openDropdown(CraftingPlan.PlanNode node, boolean isCategory, int x, int y) {')

with open(filepath, 'w') as f:
    f.write(content)
