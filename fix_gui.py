import os

with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'r') as f:
    content = f.read()

# remove duplicate getUniqueId
bad_method = '''    @SuppressWarnings("unchecked")
    public static <T> String getUniqueId(ITypedIngredient<T> typedIng) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientHelper<T> helper = jeiRuntime.getIngredientManager().getIngredientHelper(typedIng.getType());
            return helper.getUniqueId(typedIng.getIngredient(), UidContext.Ingredient);
        }
        return "";
    }'''
content = content.replace(bad_method, '')

with open('src/main/java/com/craftanyway/planning/RecipePlanner.java', 'w') as f:
    f.write(content)

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r') as f:
    content = f.read()

content = content.replace('guiGraphics.pose().translate(panX, panY);', 'guiGraphics.pose().translate((float) panX, (float) panY);')
content = content.replace('guiGraphics.pose().scale((float) zoom, (float) zoom, 1f);', 'guiGraphics.pose().scale((float) zoom, (float) zoom);')
content = content.replace('guiGraphics.pose().scale(0.75f, 0.75f, 1f);', 'guiGraphics.pose().scale(0.75f, 0.75f);')

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w') as f:
    f.write(content)

