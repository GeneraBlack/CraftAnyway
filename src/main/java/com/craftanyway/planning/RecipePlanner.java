package com.craftanyway.planning;


import com.craftanyway.jei.CraftAnywayJeiPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.IRecipeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.NonNullList;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

public class RecipePlanner {
    
    private static CraftingPlan currentPlan = null;
    private static List<CraftingPlan> alternativePlans = new ArrayList<>();
    public static Map<String, String> userPreferences = new java.util.HashMap<>();
    public static Map<String, String> tagPreferences = new java.util.HashMap<>();

    public static class RecipeOption {
        public final String recipeId;
        public final String name;
        public final int cost;
        public final List<ItemStack> previewItems;
        public RecipeOption(String recipeId, String name, int cost, List<ItemStack> previewItems) {
            this.recipeId = recipeId;
            this.name = name;
            this.cost = cost;
            this.previewItems = previewItems;
        }
    }

    public static void plan(ItemStack target) {
        if (target == null || target.isEmpty()) return;
        
        alternativePlans.clear();
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        Minecraft mc = Minecraft.getInstance();
        Inventory inv = mc.player != null ? mc.player.getInventory() : null;
        
        if (jeiRuntime != null) {
            // Find ALL recipes for the root item across all JEI categories
            List<CraftingPlan.PlanNode> rootNodes = buildNodesForTarget(target, new HashSet<>(), true, inv);
            
            if (rootNodes.isEmpty()) {
                int rawCost = getCostWithInventory(target, inv);
                CraftingPlan.PlanNode rootNode = new CraftingPlan.PlanNode(target, "None", false, null, target.getCount(), new ArrayList<>(), rawCost, "craftanyway:raw");
                alternativePlans.add(new CraftingPlan(target, rootNode));
            } else {
                for (CraftingPlan.PlanNode node : rootNodes) {
                    alternativePlans.add(new CraftingPlan(target, node));
                }
            }
        }
        
        if (!alternativePlans.isEmpty()) {
            currentPlan = alternativePlans.get(0);
        }
    }
    
    public static CraftingPlan getCurrentPlan() {
        return currentPlan;
    }
    
    public static void setCurrentPlan(CraftingPlan plan) {
        currentPlan = plan;
    }
    
    public static List<CraftingPlan> getAlternativePlans() {
        return alternativePlans;
    }

    public static List<RecipeOption> getAvailableOptions(ItemStack target) {
        List<RecipeOption> options = new ArrayList<>();
        
        // Always offer "Raw" as an option
        Minecraft mc = Minecraft.getInstance();
        Inventory inv = mc.player != null ? mc.player.getInventory() : null;
        int rawCost = getCostWithInventory(target, inv);
        options.add(new RecipeOption("craftanyway:raw", "Raw", rawCost, new ArrayList<>()));
        
        // Query JEI directly for all recipe categories that produce this item
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime == null) return options;
        
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        var ingredientManager = jeiRuntime.getIngredientManager();
        
        // Build focuses for this item (same normalization as buildNodesForTarget)
        List<IFocus<ItemStack>> focuses = new ArrayList<>();
        for (ItemStack stack : ingredientManager.getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
            if (stack.getItem() == target.getItem()) {
                var typedOpt = ingredientManager.createTypedIngredient(stack);
                if (typedOpt.isPresent()) {
                    focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
                }
            }
        }
        if (focuses.isEmpty()) {
            var typedOpt = ingredientManager.createTypedIngredient(new ItemStack(target.getItem()));
            if (typedOpt.isPresent()) {
                focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
            }
        }
        if (focuses.isEmpty()) return options;
        
        // Get all categories that produce this item
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList();
        
        for (IRecipeCategory<?> category : categories) {
            String catPath = category.getRecipeType().getUid().getPath();
            if (catPath.equals("information")) continue;
            
            List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).limitFocus(focuses).get().toList();
            if (recipes.isEmpty()) continue;
            
            String categoryName = category.getTitle().getString();
            
            // Standard and custom modded recipes
            for (Object recipeObj : recipes) {
                try {
                    Recipe<?> recipe = extractRecipeFromObject(recipeObj);
                    if (recipe != null) {
                        String recipeId = extractRecipeIdFromObject(recipeObj, recipe);
                        int ingredientCount = 0;
                        List<ItemStack> previewItems = new ArrayList<>();
                        for (Ingredient ing : extractIngredientsFromRecipe(recipe)) {
                            if (!ing.isEmpty()) {
                                ingredientCount++;
                                ItemStack[] items = ing.getItems();
                                if (items.length > 0) {
                                    previewItems.add(items[0]);
                                }
                            }
                        }
                        int cost = ingredientCount * target.getCount();
                        options.add(new RecipeOption(recipeId, categoryName, cost, previewItems));
                    } else {
                        // Non-standard JEI recipe wrapper fallback
                        String recipeId = "jei:" + category.getRecipeType().getUid().toString();
                        options.add(new RecipeOption(recipeId, categoryName, 0, new ArrayList<>()));
                    }
                } catch (Throwable t) {
                    // Fail-safe: if reflection or modded extraction throws ANY error, fall back to simple category entry
                    String recipeId = "jei:" + category.getRecipeType().getUid().toString();
                    options.add(new RecipeOption(recipeId, categoryName, 0, new ArrayList<>()));
                }
            }
        }
        
        return options;
    }

    public static List<RecipeOption> getVariantOptions(CraftingPlan.PlanNode node) {
        List<RecipeOption> options = new ArrayList<>();
        if (node.getTagOptions() != null) {
            for (ItemStack opt : node.getTagOptions()) {
                List<ItemStack> preview = new ArrayList<>();
                preview.add(opt);
                options.add(new RecipeOption(opt.getItem().toString(), opt.getHoverName().getString(), 0, preview));
            }
        }
        return options;
    }

    private static List<CraftingPlan.PlanNode> buildNodesForTarget(ItemStack target, Set<String> visited, boolean isRoot, Inventory inv) {
        List<CraftingPlan.PlanNode> nodes = new ArrayList<>();
        String itemId = target.getItem().toString();
        if (visited.contains(itemId)) {
            return nodes; // Prevent circular
        }
        
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime == null) return nodes;

        int rawCost = getCostWithInventory(target, inv);
        CraftingPlan.PlanNode rawNode = new CraftingPlan.PlanNode(target, "Raw", false, null, target.getCount(), new ArrayList<>(), rawCost, "craftanyway:raw");
        nodes.add(rawNode);

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        
        // Normalize the item stack to match JEI's registered ingredient EXACTLY
        // Collect ALL variants of the item (some mods/recipes might output variants with different data components)
        var ingredientManager = jeiRuntime.getIngredientManager();
        List<IFocus<ItemStack>> focuses = new ArrayList<>();
        
        for (ItemStack stack : ingredientManager.getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
            if (stack.getItem() == target.getItem()) {
                var typedOpt = ingredientManager.createTypedIngredient(stack);
                if (typedOpt.isPresent()) {
                    focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
                }
            }
        }
        
        // Default fallback if JEI index doesn't have it
        if (focuses.isEmpty()) {
            var typedOpt = ingredientManager.createTypedIngredient(new ItemStack(target.getItem()));
            if (typedOpt.isPresent()) {
                focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
            }
        }
        
        if (focuses.isEmpty()) return nodes;
        
        // Pass all focuses at once - JEI handles OR-logic internally
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList();
        
        for (IRecipeCategory<?> category : categories) {
            if (category.getRecipeType().getUid().getPath().equals("information")) {
                continue;
            }

            List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).limitFocus(focuses).get().toList();
            
            for (Object recipeObj : recipes) {
                try {
                    Recipe<?> recipe = extractRecipeFromObject(recipeObj);
                    if (recipe != null) {
                        // Check if this recipe contains any ingredient that is already in visited (circular dependency)
                        boolean hasCycle = false;
                        List<Ingredient> ingredients = extractIngredientsFromRecipe(recipe);
                        for (Ingredient ingredient : ingredients) {
                            if (ingredient.isEmpty()) continue;
                            ItemStack[] items = ingredient.getItems();
                            if (items.length > 0) {
                                boolean allVisited = true;
                                for (ItemStack opt : items) {
                                    if (!visited.contains(opt.getItem().toString())) {
                                        allVisited = false;
                                        break;
                                    }
                                }
                                if (allVisited) {
                                    hasCycle = true;
                                    break;
                                }
                            }
                        }
                        if (hasCycle) {
                            continue; // Skip this recipe to prevent circular loops
                        }
                        
                        visited.add(itemId);
                        
                        List<CraftingPlan.PlanNode> children = new ArrayList<>();
                        boolean isCrafting = category.getRecipeType().getUid().getPath().equals("crafting");
                        
                        int requestedAmount = target.getCount();
                        int recipeYield = 1;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null) {
                            ItemStack result = extractResultFromRecipe(recipe, mc.level.registryAccess());
                            if (!result.isEmpty()) recipeYield = result.getCount();
                        }
                        int craftsNeeded = (int) Math.ceil((double) requestedAmount / recipeYield);
                        
                        int totalCost = craftsNeeded; // 1 cost per craft operation
                        
                        java.util.Map<String, ItemStack> groupedIngredients = new java.util.HashMap<>();
                        java.util.Map<String, ItemStack[]> groupedOptions = new java.util.HashMap<>();
     
                        for (Ingredient ingredient : ingredients) {
                            if (ingredient.isEmpty()) continue;
                            
                            ItemStack[] items = ingredient.getItems();
                            if (items.length > 0) {
                                String tagSignature = null;
                                if (items.length > 1) {
                                    List<String> ids = new ArrayList<>();
                                    for (ItemStack stack : items) ids.add(stack.getItem().toString());
                                    java.util.Collections.sort(ids);
                                    tagSignature = String.join(",", ids);
                                }
                                
                                String preferredVariant = tagSignature != null ? tagPreferences.get(tagSignature) : null;
                                
                                ItemStack chosenOpt = items[0];
                                for (ItemStack opt : items) {
                                    if (preferredVariant != null && opt.getItem().toString().equals(preferredVariant)) {
                                        chosenOpt = opt;
                                        break;
                                    }
                                }
                                
                                String key = chosenOpt.getItem().toString();
                                if (groupedIngredients.containsKey(key)) {
                                    groupedIngredients.get(key).grow(1);
                                } else {
                                    ItemStack copy = chosenOpt.copy();
                                    copy.setCount(1);
                                    groupedIngredients.put(key, copy);
                                    groupedOptions.put(key, items);
                                }
                            }
                        }
     
                        for (java.util.Map.Entry<String, ItemStack> entry : groupedIngredients.entrySet()) {
                            ItemStack groupedTarget = entry.getValue();
                            ItemStack[] items = groupedOptions.get(entry.getKey());
                            
                            groupedTarget.setCount(groupedTarget.getCount() * craftsNeeded);
                            
                            List<CraftingPlan.PlanNode> subNodes = buildNodesForTarget(groupedTarget, visited, false, inv);
                            CraftingPlan.PlanNode candidate;
                            if (!subNodes.isEmpty()) {
                                candidate = subNodes.get(0);
                            } else {
                                int fallbackCost = getCostWithInventory(groupedTarget, inv);
                                candidate = new CraftingPlan.PlanNode(groupedTarget, "Raw", false, null, groupedTarget.getCount(), new ArrayList<>(), fallbackCost, "craftanyway:raw");
                            }
                            
                            if (items.length > 1) {
                                String tagSignature = null;
                                List<String> ids = new ArrayList<>();
                                for (ItemStack stack : items) ids.add(stack.getItem().toString());
                                java.util.Collections.sort(ids);
                                tagSignature = String.join(",", ids);
                                candidate = new CraftingPlan.PlanNode(candidate.getOutput(), candidate.getCategoryName(), candidate.isCraftingTable(), candidate.getRecipe(), candidate.getCraftsNeeded(), candidate.getChildren(), candidate.getCost(), candidate.getRecipeId(), items, tagSignature);
                            }
                            
                            children.add(candidate);
                            totalCost += candidate.getCost();
                        }
                        
                        visited.remove(itemId);
                        String recipeId = extractRecipeIdFromObject(recipeObj, recipe);
                        nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), isCrafting, recipe, craftsNeeded, children, totalCost, recipeId));
                    } else {
                        // Non-standard recipe (e.g. JEI custom wrapper), treat as leaf with category name
                        int craftsNeeded = target.getCount();
                        int fallbackCost = getCostWithInventory(target, inv);
                        String recipeId = "jei:" + category.getRecipeType().getUid().toString();
                        nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), false, null, craftsNeeded, new ArrayList<>(), fallbackCost, recipeId));
                    }
                } catch (Throwable t) {
                    // Fail-safe: if reflection or modded extraction throws ANY error, fall back to simple category node
                    int craftsNeeded = target.getCount();
                    int fallbackCost = getCostWithInventory(target, inv);
                    String recipeId = "jei:" + category.getRecipeType().getUid().toString();
                    nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), false, null, craftsNeeded, new ArrayList<>(), fallbackCost, recipeId));
                }
            }
        }
        
        if (!isRoot && !nodes.isEmpty()) {
            CraftingPlan.PlanNode chosen = nodes.get(0);
            String pref = userPreferences.get(itemId);
            
            if (pref != null) {
                for (CraftingPlan.PlanNode n : nodes) {
                    if (n.getRecipeId().equals(pref)) {
                        chosen = n;
                        break;
                    }
                }
            } else {
                for (CraftingPlan.PlanNode n : nodes) {
                    if (n.getCost() < chosen.getCost()) {
                        chosen = n;
                    }
                }
            }
            nodes.clear();
            nodes.add(chosen);
        } else if (isRoot && nodes.size() > 1) {
            nodes.sort((a, b) -> Integer.compare(a.getCost(), b.getCost()));
            List<CraftingPlan.PlanNode> filtered = new ArrayList<>();
            for (CraftingPlan.PlanNode n : nodes) {
                if (n == rawNode && nodes.size() > 1 && n.getCost() > nodes.get(0).getCost()) {
                    continue; // Hide the useless raw alternative if a better crafting path exists
                }
                
                // Avoid adding exact duplicate structures (e.g. two identical recipes from different categories)
                boolean isDuplicate = false;
                for (CraftingPlan.PlanNode existing : filtered) {
                    if (existing.getCategoryName().equals(n.getCategoryName()) && existing.getCost() == n.getCost()) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    filtered.add(n);
                }
                
                if (filtered.size() >= 3) break; // Keep at most 3 alternatives for root
            }
            nodes = filtered;
        }
        
        return nodes;
    }
    
    private static int getCostWithInventory(ItemStack target, Inventory inv) {
        int have = inv != null ? countItem(inv, target.getItem()) : 0;
        int missing = Math.max(0, target.getCount() - have);
        return RecipeWeights.getRawCost(target.getItem()) * missing;
    }
    
    private static int countItem(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static Recipe<?> extractRecipeFromObject(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Recipe<?> r) return r;
        if (obj instanceof RecipeHolder<?> holder) return holder.value();
        
        try {
            // Scan fields of obj
            Class<?> clazz = obj.getClass();
            while (clazz != null && clazz != Object.class) {
                String name = clazz.getName();
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("net.minecraft.") || name.startsWith("mezz.jei.")) {
                    break;
                }
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        if (value instanceof Recipe<?> r) {
                            return r;
                        }
                        if (value instanceof RecipeHolder<?> holder) {
                            return holder.value();
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }

    private static String extractRecipeIdFromObject(Object obj, Recipe<?> recipe) {
        if (obj instanceof RecipeHolder<?> holder) {
            return holder.id().toString();
        }
        
        try {
            // Scan fields of obj for RecipeHolder
            Class<?> clazz = obj.getClass();
            while (clazz != null && clazz != Object.class) {
                String name = clazz.getName();
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("net.minecraft.") || name.startsWith("mezz.jei.")) {
                    break;
                }
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        if (value instanceof RecipeHolder<?> holder) {
                            return holder.id().toString();
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            // Ignore
        }
        
        // Fallback to class name + hash code if we only have a raw Recipe
        if (recipe != null) {
            return "recipe:" + recipe.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(recipe));
        }
        return "unknown";
    }

    private static List<Ingredient> extractIngredientsFromRecipe(Recipe<?> recipe) {
        try {
            NonNullList<Ingredient> standard = recipe.getIngredients();
            if (standard != null && !standard.isEmpty()) {
                return standard;
            }
        } catch (Throwable t) {
            // Ignore standard extraction errors
        }
        
        // Reflection fallback for custom modded recipes (like OritechRecipe)
        List<Ingredient> extracted = new ArrayList<>();
        try {
            Class<?> clazz = recipe.getClass();
            while (clazz != null && clazz != Object.class) {
                String name = clazz.getName();
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("net.minecraft.") || name.startsWith("mezz.jei.")) {
                    break;
                }
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(recipe);
                        if (value == null) continue;
                        
                        if (value instanceof List<?> list) {
                            for (Object element : list) {
                                if (element instanceof Ingredient ing) {
                                    extracted.add(ing);
                                } else if (element instanceof ItemStack stack) {
                                    if (!stack.isEmpty()) {
                                        extracted.add(Ingredient.of(stack));
                                    }
                                }
                            }
                            if (!extracted.isEmpty()) return extracted;
                        } else if (value instanceof Ingredient[] arr) {
                            for (Ingredient ing : arr) {
                                if (ing != null && !ing.isEmpty()) {
                                    extracted.add(ing);
                                }
                            }
                            if (!extracted.isEmpty()) return extracted;
                        } else if (value instanceof ItemStack[] arr) {
                            for (ItemStack stack : arr) {
                                if (stack != null && !stack.isEmpty()) {
                                    extracted.add(Ingredient.of(stack));
                                }
                            }
                            if (!extracted.isEmpty()) return extracted;
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            // Ignore
        }
        return extracted;
    }

    private static ItemStack extractResultFromRecipe(Recipe<?> recipe, net.minecraft.core.RegistryAccess registryAccess) {
        try {
            ItemStack standard = recipe.getResultItem(registryAccess);
            if (standard != null && !standard.isEmpty()) {
                return standard;
            }
        } catch (Throwable t) {
            // Ignore standard extraction errors
        }
        
        // Reflection fallback for custom modded recipes (like OritechRecipe)
        try {
            Class<?> clazz = recipe.getClass();
            while (clazz != null && clazz != Object.class) {
                String name = clazz.getName();
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("net.minecraft.") || name.startsWith("mezz.jei.")) {
                    break;
                }
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(recipe);
                        if (value == null) continue;
                        
                        if (value instanceof List<?> list) {
                            for (Object element : list) {
                                if (element instanceof ItemStack stack) {
                                    if (!stack.isEmpty()) {
                                        return stack;
                                    }
                                }
                            }
                        } else if (value instanceof ItemStack stack) {
                            if (!stack.isEmpty()) {
                                return stack;
                            }
                        } else if (value instanceof ItemStack[] arr) {
                            for (ItemStack stack : arr) {
                                if (stack != null && !stack.isEmpty()) {
                                    return stack;
                                }
                            }
                        }
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            // Ignore
        }
        
        return ItemStack.EMPTY;
    }
}
