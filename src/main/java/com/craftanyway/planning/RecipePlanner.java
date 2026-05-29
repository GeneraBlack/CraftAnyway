package com.craftanyway.planning;

import com.craftanyway.jei.CraftAnywayJeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecipePlanner {

    private static CraftingPlan currentPlan = null;
    private static final Map<String, String> itemPreferences = new HashMap<>();

    public static void setPreference(String itemKey, String recipeId) {
        itemPreferences.put(itemKey, recipeId);
    }

    public static String getPreference(String itemKey) {
        return itemPreferences.get(itemKey);
    }

    public static class RecipeOption {
        public final String recipeId;
        public final String name;
        public final int cost;
        public final List<ITypedIngredient<?>> previewItems;

        public RecipeOption(String recipeId, String name, int cost, List<ITypedIngredient<?>> previewItems) {
            this.recipeId = recipeId;
            this.name = name;
            this.cost = cost;
            this.previewItems = previewItems;
        }
    }

    
    @SuppressWarnings("unchecked")
    public static <T> String getUniqueId(ITypedIngredient<T> typedIng) {
        var jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime != null) {
            IIngredientHelper<T> helper = jeiRuntime.getIngredientManager().getIngredientHelper(typedIng.getType());
            return helper.getUid(typedIng.getIngredient(), mezz.jei.api.ingredients.subtypes.UidContext.Recipe).toString();
        }
        return "";
    }

    


    public static void plan(ITypedIngredient<?> target, long targetAmount) {
        if (target == null) return;
        
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        Minecraft mc = Minecraft.getInstance();
        Inventory inv = mc.player != null ? mc.player.getInventory() : null;
        
        if (jeiRuntime != null) {
            List<CraftingPlan.PlanNode> rootNodes = buildNodesForTarget(target, targetAmount, inv);
            
            if (rootNodes.isEmpty()) {
                String uid = getUniqueId(target);
                CraftingPlan.PlanNode rootNode = new CraftingPlan.PlanNode(target, targetAmount, uid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, "");
                currentPlan = new CraftingPlan(target, targetAmount, rootNode);
            } else {
                currentPlan = new CraftingPlan(target, targetAmount, rootNodes.get(0));
            }
        }
    }
    
    public static CraftingPlan getCurrentPlan() {
        return currentPlan;
    }
    
    public static void setCurrentPlan(CraftingPlan plan) {
        currentPlan = plan;
    }
    
    public static List<CraftingPlan> getAlternativePlans() {
        List<CraftingPlan> list = new ArrayList<>();
        if (currentPlan != null) list.add(currentPlan);
        return list;
    }

    public static List<RecipeOption> getAvailableOptions(ITypedIngredient<?> target) {
        List<RecipeOption> options = new ArrayList<>();
        
        // Always offer "Raw" and "Ignore" as options
        options.add(new RecipeOption("craftanyway:raw", "Raw", 0, new ArrayList<>()));
        options.add(new RecipeOption("craftanyway:ignore", "Ignore", 0, new ArrayList<>()));
        
        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime == null) return options;
        
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        mezz.jei.api.runtime.IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();

        List<IFocus<Object>> focuses = new ArrayList<>();
        focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, (ITypedIngredient<Object>) target));
        IFocusGroup focusGroup = jeiRuntime.getJeiHelpers().getFocusFactory().createFocusGroup(focuses);

        for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList()) {
            String catTitle = category.getTitle().getString();
            if (catTitle.equals("Item Tags") || catTitle.equals("Block Tags")) continue;
            
            var type = category.getRecipeType();

            List<?> recipes = recipeManager.createRecipeLookup(type).limitFocus(focuses).get().toList();
            for (Object recipeObj : recipes) {
                try {
                    String recipeId = category.getRecipeType().getUid().toString();
                    if (recipeObj instanceof net.minecraft.world.item.crafting.RecipeHolder<?> r) {
                        recipeId = r.id().toString();
                    }
                    
                    int ingredientCount = 0;
                    List<ITypedIngredient<?>> previewItems = new ArrayList<>();
                    
                    Optional<mezz.jei.api.gui.IRecipeLayoutDrawable<Object>> opt = recipeManager.createRecipeLayoutDrawable(
                        (IRecipeCategory<Object>) category, recipeObj, focusGroup
                    );
                    
                    if (opt.isPresent()) {
                        for (IRecipeSlotView view : opt.get().getRecipeSlotsView().getSlotViews()) {
                            if (view.getRole() != mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT) {
                                Optional<ITypedIngredient<?>> ingOpt = view.getDisplayedIngredient();
                                if (ingOpt.isPresent()) {
                                    previewItems.add(ingOpt.get());
                                    ingredientCount++;
                                }
                            }
                        }
                    }

                    options.add(new RecipeOption(recipeId, category.getTitle().getString(), ingredientCount, previewItems));
                } catch (Exception e) {}
            }
        }
        
        return options;
    }

    private static List<CraftingPlan.PlanNode> buildNodesForTarget(ITypedIngredient<?> target, long requestedAmount, Inventory inv) {
        List<CraftingPlan.PlanNode> nodes = new ArrayList<>();
        if (target == null) return nodes;

        IJeiRuntime jeiRuntime = CraftAnywayJeiPlugin.getJeiRuntime();
        if (jeiRuntime == null) return nodes;
        
        mezz.jei.api.runtime.IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
        String targetKey = getUniqueId(target);
        String uid = targetKey;

        Map<String, String> tagPreferences = new HashMap<>();
        String pref = itemPreferences.get(targetKey);

        if ("craftanyway:raw".equals(pref)) {
            nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, "Raw", false, null, 1, 1L, new ArrayList<>(), 0, "craftanyway:raw"));
            return nodes;
        }
        
        if ("craftanyway:ignore".equals(pref)) {
            nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, "Ignore", false, null, 1, 1L, new ArrayList<>(), 0, "craftanyway:ignore"));
            return nodes;
        }

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();

        List<IFocus<Object>> focuses = new ArrayList<>();
        focuses.add(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, (ITypedIngredient<Object>) target));
        IFocusGroup focusGroup = jeiRuntime.getJeiHelpers().getFocusFactory().createFocusGroup(focuses);

        for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().limitFocus(focuses).get().toList()) {
            var type = category.getRecipeType();

            List<?> recipes = recipeManager.createRecipeLookup(type).limitFocus(focuses).get().toList();
            for (Object recipeObj : recipes) {
                String recipeId = category.getRecipeType().getUid().toString();
                Recipe<?> recipe = null;
                if (recipeObj instanceof net.minecraft.world.item.crafting.RecipeHolder<?> r) {
                    recipe = r.value();
                    recipeId = r.id().toString();
                }

                if (pref != null && !pref.equals(recipeId)) {
                    continue;
                }
                
                if (pref == null) {
                    nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, ""));
                    return nodes;
                }

                try {
                    boolean isCrafting = category.getRecipeType().getUid().getPath().equals("crafting");
                    
                    Optional<mezz.jei.api.gui.IRecipeLayoutDrawable<Object>> opt = recipeManager.createRecipeLayoutDrawable(
                        (IRecipeCategory<Object>) category, recipeObj, focusGroup
                    );
                    
                    long recipeYield = 1;
                    if (opt.isPresent()) {
                        for (mezz.jei.api.gui.ingredient.IRecipeSlotView view : opt.get().getRecipeSlotsView().getSlotViews()) {
                            if (view.getRole() == mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT) {
                                Optional<ITypedIngredient<?>> outIng = view.getDisplayedIngredient();
                                if (outIng.isPresent() && getUniqueId(outIng.get()).equals(getUniqueId(target))) {
                                    long amt = getIngredientAmount(outIng.get().getIngredient());
                                    if (amt > 0) recipeYield = amt;
                                }
                            }
                        }
                    }
                    
                    int craftsNeeded = (int) Math.ceil((double) requestedAmount / recipeYield);
                    
                    Map<String, ITypedIngredient<?>> groupedIngredients = new HashMap<>();
                    Map<String, ITypedIngredient<?>[]> groupedOptions = new HashMap<>();
                    Map<String, Long> groupedAmounts = new HashMap<>();
                    
                    if (opt.isPresent()) {
                        for (IRecipeSlotView view : opt.get().getRecipeSlotsView().getSlotViews()) {
                            if (view.getRole() == mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT) continue;
                            
                            List<ITypedIngredient<?>> typedIngs = view.getAllIngredients().collect(Collectors.toList());
                            if (typedIngs.isEmpty()) continue;
                            
                            String tagSignature = null;
                            if (typedIngs.size() > 1) {
                                List<String> ids = new ArrayList<>();
                                for (ITypedIngredient<?> ti : typedIngs) {
                                    ids.add(getUniqueId(ti));
                                }
                                java.util.Collections.sort(ids);
                                tagSignature = String.join(",", ids);
                            }
                            
                            String preferredVariant = tagSignature != null ? tagPreferences.get(tagSignature) : null;
                            
                            ITypedIngredient<?> chosenOpt = typedIngs.get(0);
                            if (preferredVariant != null) {
                                for (ITypedIngredient<?> ti : typedIngs) {
                                    if (getUniqueId(ti).equals(preferredVariant)) {
                                        chosenOpt = ti;
                                        break;
                                    }
                                }
                            }
                            
                            String key = getUniqueId(chosenOpt);
                            
                            if (groupedIngredients.containsKey(key)) {
                                groupedAmounts.put(key, groupedAmounts.get(key) + getIngredientAmount(chosenOpt.getIngredient()));
                            } else {
                                groupedIngredients.put(key, chosenOpt);
                                groupedAmounts.put(key, getIngredientAmount(chosenOpt.getIngredient()));
                                groupedOptions.put(key, typedIngs.toArray(new ITypedIngredient<?>[0]));
                            }
                        }
                    }

                    List<CraftingPlan.PlanNode> children = new ArrayList<>();
                    
                    for (Map.Entry<String, ITypedIngredient<?>> entry : groupedIngredients.entrySet()) {
                        ITypedIngredient<?> groupedTarget = entry.getValue();
                        long baseAmount = groupedAmounts.get(entry.getKey());
                        ITypedIngredient<?>[] items = groupedOptions.get(entry.getKey());
                        
                        long totalRequired = baseAmount * craftsNeeded;
                        
                        List<CraftingPlan.PlanNode> subNodes = buildNodesForTarget(groupedTarget, totalRequired, inv);
                        CraftingPlan.PlanNode candidate;
                        if (!subNodes.isEmpty()) {
                            candidate = subNodes.get(0);
                        } else {
                            String childUid = getUniqueId(groupedTarget);
                            candidate = new CraftingPlan.PlanNode(groupedTarget, totalRequired, childUid, "Select Category...", false, null, 1, 1L, new ArrayList<>(), 0, "");
                        }
                        
                        if (items.length > 1) {
                            String tagSignature = null;
                            List<String> ids = new ArrayList<>();
                            for (ITypedIngredient<?> ti : items) {
                                ids.add(getUniqueId(ti));
                            }
                            java.util.Collections.sort(ids);
                            tagSignature = String.join(",", ids);
                            candidate = new CraftingPlan.PlanNode(candidate.getOutput(), candidate.getAmount(), candidate.getUniqueId(), candidate.getCategoryName(), candidate.isCraftingTable(), candidate.getRecipe(), candidate.getCraftsNeeded(), candidate.getRecipeYield(), candidate.getChildren(), 0, candidate.getRecipeId(), items, tagSignature, candidate.getRawRecipe(), candidate.getRecipeCategory());
                        }
                        
                        children.add(candidate);
                    }

                    nodes.add(new CraftingPlan.PlanNode(target, requestedAmount, uid, category.getTitle().getString(), category.getRecipeType().getUid().getPath().equals("crafting"), recipe, craftsNeeded, recipeYield, children, 0, recipeId, recipeObj, category));
                    return nodes;

                } catch (Exception e) {}
            }
        }

        return nodes;
    }

    public static long getIngredientAmount(Object ingredient) {
        if (ingredient == null) return 0;
        try {
            Method getCount = ingredient.getClass().getMethod("getCount");
            return ((Number) getCount.invoke(ingredient)).longValue();
        } catch (Exception e) {}
        try {
            Method getAmount = ingredient.getClass().getMethod("getAmount");
            return ((Number) getAmount.invoke(ingredient)).longValue();
        } catch (Exception e) {}
        return 1;
    }
}
