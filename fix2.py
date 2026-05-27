import os

def fix2():
    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r', encoding='utf-8') as f:
        c = f.read()

    c = c.replace('for (RecipeType<?> type : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).getRecipeTypes().toList()) {', 'for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList()) {\\n            RecipeType<?> type = category.getRecipeType();')
    c = c.replace('IRecipeCategory<?> category = recipeManager.createRecipeCategoryLookup().limitTypes(List.of(type)).get().findFirst().orElse(null);\\n            if (category == null) continue;', '// category is already available')

    with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w', encoding='utf-8') as f:
        f.write(c)

if __name__ == "__main__":
    fix2()
