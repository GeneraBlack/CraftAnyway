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
        mezz.jei.api.runtime.IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        
        // Build focuses for this item using the exact stack and generic fallback
        List<IFocus<ItemStack>> focuses = new ArrayList<>();
        var typedOpt = ingredientManager.createTypedIngredient(target);
        if (typedOpt.isPresent()) {
            focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
        }
        
        if (target.hasTag()) {
            ItemStack defaultStack = target.getItem().getDefaultInstance();
            var defaultOpt = ingredientManager.createTypedIngredient(defaultStack);
            if (defaultOpt.isPresent()) {
                focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, defaultOpt.get()));
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
            
            // For standard recipes, add one option per unique recipe
            for (Object recipeObj : recipes) {
                try {
                    Recipe<?> recipe = extractRecipeFromObject(recipeObj);
                    String recipeId = (recipe != null && recipe.getId() != null) ? recipe.getId().toString() : ("jei:" + category.getRecipeType().getUid().toString());
                    
                    int ingredientCount = 0;
                    List<ItemStack> previewItems = new ArrayList<>();
                    for (Ingredient ing : extractIngredientsFromObject(recipeObj)) {
                        if (!ing.isEmpty()) {
                            ingredientCount++;
                            ItemStack[] items = ing.getItems();
                            if (items.length > 0) {
                                previewItems.add(items[0]);
                            }
                        }
                    }
                    int cost = ingredientCount > 0 ? ingredientCount * target.getCount() : 0;
                    options.add(new RecipeOption(recipeId, categoryName, cost, previewItems));
                } catch (Throwable t) {
                    // Fail-safe
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
        
        // Build focuses for this item using the exact stack and generic fallback
        var ingredientManager = jeiRuntime.getIngredientManager();
        List<IFocus<ItemStack>> focuses = new ArrayList<>();
        var typedOpt = ingredientManager.createTypedIngredient(target);
        if (typedOpt.isPresent()) {
            focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get()));
        }
        
        if (target.hasTag()) {
            ItemStack defaultStack = target.getItem().getDefaultInstance();
            var defaultOpt = ingredientManager.createTypedIngredient(defaultStack);
            if (defaultOpt.isPresent()) {
                focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, defaultOpt.get()));
            }
        }
        
        if (focuses.isEmpty()) return nodes;
        
        // Pass all focuses at once
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList();
        
        for (IRecipeCategory<?> category : categories) {
            if (category.getRecipeType().getUid().getPath().equals("information")) {
                continue;
            }

            List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).limitFocus(focuses).get().toList();
            
            for (Object recipeObj : recipes) {
                try {
                    Recipe<?> recipe = extractRecipeFromObject(recipeObj);
                    List<Ingredient> ingredients = extractIngredientsFromObject(recipeObj);
                    String recipeId = (recipe != null && recipe.getId() != null) ? recipe.getId().toString() : ("jei:" + category.getRecipeType().getUid().toString());

                    // Check if this recipe contains any ingredient that is already in visited (circular dependency)
                    boolean hasCycle = false;
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
                        nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), isCrafting, recipe, craftsNeeded, children, totalCost, recipeId));
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

    private static List<Ingredient> extractIngredientsFromObject(Object obj) {
        if (obj instanceof Recipe<?> recipe) {
            try {
                NonNullList<Ingredient> standard = recipe.getIngredients();
                if (standard != null && !standard.isEmpty()) {
                    return standard;
                }
            } catch (Throwable t) {
                // Ignore standard extraction errors
            }
        }
        
        List<Ingredient> extracted = new ArrayList<>();
        Set<Object> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        scanObjectForIngredients(obj, extracted, visited, 0);
        return extracted;
    }

    private static void scanObjectForIngredients(Object obj, List<Ingredient> extracted, Set<Object> visited, int depth) {
        if (obj == null || depth > 3 || !visited.add(obj)) return;
        
        if (obj instanceof Ingredient) {
            Ingredient ing = (Ingredient) obj;
            if (!ing.isEmpty()) extracted.add(ing);
            return;
        } else if (obj instanceof ItemStack) {
            ItemStack stack = (ItemStack) obj;
            if (!stack.isEmpty()) extracted.add(Ingredient.of(stack));
            return;
        } else if (obj instanceof java.util.Collection) {
            java.util.Collection<?> coll = (java.util.Collection<?>) obj;
            for (Object element : coll) {
                scanObjectForIngredients(element, extracted, visited, depth + 1);
            }
            return;
        } else if (obj.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                scanObjectForIngredients(java.lang.reflect.Array.get(obj, i), extracted, visited, depth + 1);
            }
            return;
        }
        
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            String name = clazz.getName();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("net.minecraft.") || name.startsWith("mezz.jei.")) {
                break;
            }
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    scanObjectForIngredients(value, extracted, visited, depth + 1);
                } catch (Throwable t) {
                    // Ignore
                }
            }
            clazz = clazz.getSuperclass();
        }
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
