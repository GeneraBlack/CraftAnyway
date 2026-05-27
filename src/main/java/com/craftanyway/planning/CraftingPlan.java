package com.craftanyway.planning;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import mezz.jei.api.ingredients.ITypedIngredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class CraftingPlan {
    private final ITypedIngredient<?> target;
    private final long targetAmount;
    private final PlanNode rootNode;

    public CraftingPlan(ITypedIngredient<?> target, long targetAmount, PlanNode rootNode) {
        this.target = target;
        this.targetAmount = targetAmount;
        this.rootNode = rootNode;
    }

    public ITypedIngredient<?> getTarget() {
        return target;
    }
    
    public long getTargetAmount() {
        return targetAmount;
    }

    public PlanNode getRootNode() {
        return rootNode;
    }

    public static class StepItem {
        public final ITypedIngredient<?> ingredient;
        public final String uniqueId;
        public final long needed;
        public final long have;
        
        public StepItem(ITypedIngredient<?> ingredient, String uniqueId, long needed, long have) {
            this.ingredient = ingredient;
            this.uniqueId = uniqueId;
            this.needed = needed;
            this.have = have;
        }
    }

    public static class CraftingStep {
        public final int stepNumber;
        public final Map<String, StepItem> items = new HashMap<>();
        
        public CraftingStep(int stepNumber) {
            this.stepNumber = stepNumber;
        }
    }

    public static class PlanResult {
        public final List<CraftingStep> steps;
        
        public PlanResult(List<CraftingStep> steps) {
            this.steps = steps;
        }
    }

    public PlanResult calculateRequirements(Inventory inv) {
        Map<Integer, CraftingStep> stepMap = new HashMap<>();
        
        Map<String, Long> remainingInv = new HashMap<>();
        if (inv != null) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    String descId = stack.getItem().getDescriptionId(); // Simple unique ID for inventory items
                    remainingInv.put(descId, remainingInv.getOrDefault(descId, 0L) + stack.getCount());
                }
            }
        }
        
        if (rootNode != null) {
            aggregateSteps(rootNode, rootNode.getAmount(), 0, remainingInv, stepMap, inv);
        }
        
        List<CraftingStep> stepList = new ArrayList<>();
        int maxDepth = stepMap.keySet().stream().max(Integer::compareTo).orElse(0);
        
        int currentStepNum = 1;
        for (int d = maxDepth; d > 0; d--) {
            CraftingStep s = stepMap.get(d);
            if (s != null && !s.items.isEmpty()) {
                CraftingStep realStep = new CraftingStep(currentStepNum++);
                realStep.items.putAll(s.items);
                stepList.add(realStep);
            }
        }
        
        return new PlanResult(stepList);
    }

    private void aggregateSteps(PlanNode node, long neededAmount, int depth, Map<String, Long> remainingInv, 
                           Map<Integer, CraftingStep> stepMap, Inventory inv) {
        if (node == null || "Ignore".equals(node.getCategoryName())) return;

        ITypedIngredient<?> output = node.getOutput();
        String key = node.getUniqueId();

        long remainingNeeded = neededAmount;
        
        // For vanilla items we can match by descriptionId if uniqueId contains it
        String invMatchKey = key;
        if (output.getIngredient() instanceof ItemStack stack) {
            invMatchKey = stack.getItem().getDescriptionId();
        }

        long haveAvailable = remainingInv.getOrDefault(invMatchKey, 0L);
        long used = Math.min(neededAmount, haveAvailable);
        remainingInv.put(invMatchKey, haveAvailable - used);
        remainingNeeded = neededAmount - used;

        if (depth > 0 && neededAmount > 0) {
            CraftingStep step = stepMap.computeIfAbsent(depth, CraftingStep::new);
            
            long actualHave = 0;
            if (inv != null && output.getIngredient() instanceof ItemStack stack) {
                actualHave = countItemInInventory(inv, stack.getItem());
            }
            
            if (step.items.containsKey(key)) {
                StepItem existing = step.items.get(key);
                step.items.put(key, new StepItem(output, key, existing.needed + neededAmount, actualHave));
            } else {
                step.items.put(key, new StepItem(output, key, neededAmount, actualHave));
            }
        }

        if (remainingNeeded <= 0 || node.isLeaf()) {
            return;
        }

        long recipeYield = node.getRecipeYield();
        if (recipeYield <= 0) recipeYield = 1;

        long craftsNeeded = (long) Math.ceil((double) remainingNeeded / recipeYield);
        long produced = craftsNeeded * recipeYield;
        long leftover = produced - remainingNeeded;

        if (leftover > 0) {
            remainingInv.put(invMatchKey, remainingInv.getOrDefault(invMatchKey, 0L) + leftover);
        }

        for (PlanNode child : node.getChildren()) {
            long originalCraftsNeeded = Math.max(1, node.getCraftsNeeded());
            long childAmountPerCraft = (long) Math.ceil((double) child.getAmount() / originalCraftsNeeded);
            aggregateSteps(child, childAmountPerCraft * craftsNeeded, depth + 1, remainingInv, stepMap, inv);
        }
    }

    private int countItemInInventory(Inventory inv, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static class PlanNode {
        private final ITypedIngredient<?> output;
        private final long amount;
        private final String uniqueId;
        private final String categoryName;
        private final boolean isCraftingTable;
        private final Recipe<?> recipe;
        private final int craftsNeeded;
        private final long recipeYield;
        private final List<PlanNode> children;
        private final int cost;
        private final String recipeId;
        private final ITypedIngredient<?>[] tagOptions;
        private final String tagSignature;
        private final Object rawRecipe;
        private final Object recipeCategory;

        public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId) {
            this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, recipeYield, children, cost, recipeId, null, null, null, null);
        }

        public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId, Object rawRecipe, Object recipeCategory) {
            this(output, amount, uniqueId, categoryName, isCraftingTable, recipe, craftsNeeded, recipeYield, children, cost, recipeId, null, null, rawRecipe, recipeCategory);
        }

        public PlanNode(ITypedIngredient<?> output, long amount, String uniqueId, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, long recipeYield, List<PlanNode> children, int cost, String recipeId, ITypedIngredient<?>[] tagOptions, String tagSignature, Object rawRecipe, Object recipeCategory) {
            this.output = output;
            this.amount = amount;
            this.uniqueId = uniqueId;
            this.categoryName = categoryName;
            this.isCraftingTable = isCraftingTable;
            this.recipe = recipe;
            this.craftsNeeded = craftsNeeded;
            this.recipeYield = recipeYield;
            this.children = children;
            this.cost = cost;
            this.recipeId = recipeId;
            this.tagOptions = tagOptions;
            this.tagSignature = tagSignature;
            this.rawRecipe = rawRecipe;
            this.recipeCategory = recipeCategory;
        }

        public Object getRawRecipe() {
            return rawRecipe;
        }

        public Object getRecipeCategory() {
            return recipeCategory;
        }

        public ITypedIngredient<?>[] getTagOptions() {
            return tagOptions;
        }

        public String getTagSignature() {
            return tagSignature;
        }

        public String getRecipeId() {
            return recipeId;
        }

        public int getCost() {
            return cost;
        }

        public ITypedIngredient<?> getOutput() {
            return output;
        }
        
        public long getAmount() {
            return amount;
        }
        
        public String getUniqueId() {
            return uniqueId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public boolean isCraftingTable() {
            return isCraftingTable;
        }

        public Recipe<?> getRecipe() {
            return recipe;
        }

        public int getCraftsNeeded() {
            return craftsNeeded;
        }
        
        public long getRecipeYield() {
            return recipeYield;
        }

        public List<PlanNode> getChildren() {
            return children;
        }
        
        public boolean isLeaf() {
            return children.isEmpty();
        }
    }
}
