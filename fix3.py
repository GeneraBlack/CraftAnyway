import os

def fix_errors():
    # 1. Fix CraftExecutor.java
    ce_path = 'src/main/java/com/craftanyway/execution/CraftExecutor.java'
    with open(ce_path, 'r', encoding='utf-8') as f:
        ce = f.read()
    ce = ce.replace('import net.neoforged.neoforge.event.TickEvent.ClientTickEvent;', 'import net.neoforged.neoforge.client.event.ClientTickEvent;')
    ce = ce.replace('import net.neoforged.neoforge.event.TickEvent.Phase;', '')
    ce = ce.replace('public static void onClientTick(ClientTickEvent event) {', 'public static void onClientTick(ClientTickEvent.Post event) {')
    ce = ce.replace('if (event.phase != Phase.END) return;', '')
    with open(ce_path, 'w', encoding='utf-8') as f:
        f.write(ce)

    # 2. Fix RecipePlanner.java (RecipeHolder & getId)
    rp_path = 'src/main/java/com/craftanyway/planning/RecipePlanner.java'
    with open(rp_path, 'r', encoding='utf-8') as f:
        rp = f.read()
    rp = rp.replace('if (recipeObj instanceof Recipe<?> r) {', 'if (recipeObj instanceof net.minecraft.world.item.crafting.RecipeHolder<?> r) {')
    rp = rp.replace('recipeId = r.getId().toString();', 'recipeId = r.id().toString();')
    
    # 3. JEI API getUniqueId deprecation:
    rp = rp.replace('return helper.getUniqueId(typedIng.getIngredient(), UidContext.Ingredient);', 'return helper.getUniqueId(typedIng.getIngredient(), mezz.jei.api.ingredients.subtypes.UidContext.Ingredient);')
    with open(rp_path, 'w', encoding='utf-8') as f:
        f.write(rp)

    # 4. Fix CraftAnywayJeiPlugin.java (ResourceLocation constructor)
    jei_path = 'src/main/java/com/craftanyway/jei/CraftAnywayJeiPlugin.java'
    with open(jei_path, 'r', encoding='utf-8') as f:
        jei = f.read()
    jei = jei.replace('new ResourceLocation(CraftAnyway.MODID, "jei_plugin")', 'ResourceLocation.fromNamespaceAndPath(CraftAnyway.MODID, "jei_plugin")')
    with open(jei_path, 'w', encoding='utf-8') as f:
        f.write(jei)

    # 5. Fix PlanScreen.java overrides
    # PlanScreen line 83 and 521: render method changed in 1.21.1?
    # Actually, let's remove @Override from render() and keyPressed() to avoid compilation error, or see what it is.
    ps_path = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
    with open(ps_path, 'r', encoding='utf-8') as f:
        ps = f.read()
    
    # Minecraft 1.21.1 render method: public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    # The signature in 1.20.1 was public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    # So why does it say "does not override"? 
    # Ah! In 1.21.1, the render method might be public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) but something else changed. Or it's public void renderBackground(...)?
    
    with open(ps_path, 'w', encoding='utf-8') as f:
        f.write(ps)

    # 6. Fix Keybinds.java (createTypedIngredient)
    kb_path = 'src/main/java/com/craftanyway/client/Keybinds.java'
    with open(kb_path, 'r', encoding='utf-8') as f:
        kb = f.read()
    # It says createTypedIngredient(V) is deprecated. We can ignore the warning or fix it.
    # The warning is: var opt = com.craftanyway.jei.CraftAnywayJeiPlugin.getJeiRuntime().getIngredientManager().createTypedIngredient(ingredientUnderMouse);
    # To fix we pass the ingredient type:
    kb = kb.replace('createTypedIngredient(ingredientUnderMouse)', 'createTypedIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, ingredientUnderMouse)')
    with open(kb_path, 'w', encoding='utf-8') as f:
        f.write(kb)

if __name__ == "__main__":
    fix_errors()
