import os

with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList()) {\\n            RecipeType<?> type = category.getRecipeType();\n            IRecipeCategory<?> category = recipeManager.createRecipeCategoryLookup().limitTypes(List.of(type)).get().findFirst().orElse(null);\n            if (category == null) continue;', 'for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList()) {\n            RecipeType<?> type = category.getRecipeType();')

with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
    f.write(c)
