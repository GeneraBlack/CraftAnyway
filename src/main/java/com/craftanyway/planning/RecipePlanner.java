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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RecipePlanner {
    
    private static CraftingPlan currentPlan = null;
    private static List<CraftingPlan> alternativePlans = new ArrayList<>();

    public static void plan(ItemStack target) {
        if (target == null || target.isEmpty()) return;
        
        alternativePlans.clear();
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        
        if (jeiRuntime != null) {
            // Find ALL recipes for the root item across all JEI categories
            List<CraftingPlan.PlanNode> rootNodes = buildNodesForTarget(target, new HashSet<>(), true);
            
            if (rootNodes.isEmpty()) {
                CraftingPlan.PlanNode rootNode = new CraftingPlan.PlanNode(target, "None", false, null, 1, new ArrayList<>());
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

    private static List<CraftingPlan.PlanNode> buildNodesForTarget(ItemStack target, Set<String> visited, boolean isRoot) {
        List<CraftingPlan.PlanNode> nodes = new ArrayList<>();
        String itemId = target.getItem().toString();
        if (visited.contains(itemId)) {
            return nodes; // Prevent circular
        }
        
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime == null) return nodes;

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        
        // Find categories that produce this item
        var typedTargetOpt = jeiRuntime.getIngredientManager().createTypedIngredient(target);
        if (typedTargetOpt.isEmpty()) return nodes;
        
        IFocus<ItemStack> focus = jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, typedTargetOpt.get());
        List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().limitFocus(List.of(focus)).get().toList();
        
        for (IRecipeCategory<?> category : categories) {
            List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).limitFocus(List.of(focus)).get().toList();
            
            for (Object recipeObj : recipes) {
                // If it's a vanilla RecipeHolder, we can extract ingredients
                if (recipeObj instanceof RecipeHolder<?> holder && holder.value() instanceof Recipe<?> recipe) {
                    // Check if this recipe contains any ingredient that is already in visited (circular dependency)
                    boolean hasCycle = false;
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        if (ingredient.isEmpty()) continue;
                        ItemStack[] items = ingredient.getItems();
                        if (items.length > 0) {
                            ItemStack bestItem = selectBestIngredient(items);
                            if (visited.contains(bestItem.getItem().toString())) {
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
                        try {
                            ItemStack result = recipe.getResultItem(mc.level.registryAccess());
                            if (!result.isEmpty()) recipeYield = result.getCount();
                        } catch (Exception e) {
                            // fallback
                        }
                    }
                    int craftsNeeded = (int) Math.ceil((double) requestedAmount / recipeYield);
                    
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        if (ingredient.isEmpty()) continue;
                        
                        ItemStack[] items = ingredient.getItems();
                        if (items.length > 0) {
                            ItemStack bestItem = selectBestIngredient(items);
                            ItemStack subTarget = bestItem.copy();
                            
                            // 1 ingredient in the list usually means 1 item required per craft.
                            // Multiply by craftsNeeded.
                            subTarget.setCount(craftsNeeded);
                            
                            // Recursively find recipes for this sub-target
                            List<CraftingPlan.PlanNode> subNodes = buildNodesForTarget(subTarget, visited, false);
                            if (!subNodes.isEmpty()) {
                                children.add(subNodes.get(0));
                            } else {
                                // Leaf node (raw material)
                                children.add(new CraftingPlan.PlanNode(subTarget, "Raw", false, null, craftsNeeded, new ArrayList<>()));
                            }
                        }
                    }
                    
                    visited.remove(itemId);
                    nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), isCrafting, recipe, craftsNeeded, children));
                    
                    if (!isRoot) {
                        // For sub-components, just return the first valid recipe node to keep tree manageable
                        return nodes;
                    }
                } else {
                    // Non-standard recipe (e.g. JEI custom wrapper), treat as leaf with category name
                    int craftsNeeded = target.getCount();
                    nodes.add(new CraftingPlan.PlanNode(target, category.getTitle().getString(), false, null, craftsNeeded, new ArrayList<>()));
                    if (!isRoot) return nodes;
                }
            }
        }
        
        return nodes;
    }
    
    private static ItemStack selectBestIngredient(ItemStack[] options) {
        if (options.length == 1) return options[0];
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            ItemStack best = options[0];
            int maxCount = -1;
            
            for (ItemStack opt : options) {
                int count = countItem(inv, opt.getItem());
                if (count > maxCount) {
                    maxCount = count;
                    best = opt;
                }
            }
            return best;
        }
        return options[0];
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
}
