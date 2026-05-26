package com.craftanyway.planning;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class CraftingPlan {
    private final ItemStack target;
    private final PlanNode rootNode;

    public CraftingPlan(ItemStack target, PlanNode rootNode) {
        this.target = target;
        this.rootNode = rootNode;
    }

    public ItemStack getTarget() {
        return target;
    }

    public PlanNode getRootNode() {
        return rootNode;
    }

    public static class StepItem {
        public final ItemStack stack;
        public final int needed;
        public final int have;
        
        public StepItem(ItemStack stack, int needed, int have) {
            this.stack = stack;
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
        
        Map<Item, Integer> remainingInv = new HashMap<>();
        if (inv != null) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    remainingInv.put(stack.getItem(), remainingInv.getOrDefault(stack.getItem(), 0) + stack.getCount());
                }
            }
        }
        
        if (rootNode != null) {
            aggregateSteps(rootNode, rootNode.getOutput().getCount(), 0, remainingInv, stepMap, inv);
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

    private void aggregateSteps(PlanNode node, int neededAmount, int depth, Map<Item, Integer> remainingInv, 
                           Map<Integer, CraftingStep> stepMap, Inventory inv) {
        if (node == null) return;

        ItemStack output = node.getOutput();
        Item item = output.getItem();

        int remainingNeeded = neededAmount;
        
        int haveAvailable = remainingInv.getOrDefault(item, 0);
        int used = Math.min(neededAmount, haveAvailable);
        remainingInv.put(item, haveAvailable - used);
        remainingNeeded = neededAmount - used;

        if (depth > 0 && neededAmount > 0) {
            CraftingStep step = stepMap.computeIfAbsent(depth, CraftingStep::new);
            String key = item.getDescriptionId();
            
            int actualHave = inv != null ? countItemInInventory(inv, item) : 0;
            
            if (step.items.containsKey(key)) {
                StepItem existing = step.items.get(key);
                step.items.put(key, new StepItem(output, existing.needed + neededAmount, actualHave));
            } else {
                step.items.put(key, new StepItem(output, neededAmount, actualHave));
            }
        }

        if (remainingNeeded <= 0 || node.isLeaf()) {
            return;
        }

        int recipeYield = 1;
        if (node.getRecipe() != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                try {
                    ItemStack result = node.getRecipe().getResultItem(mc.level.registryAccess());
                    if (!result.isEmpty()) {
                        recipeYield = result.getCount();
                    }
                } catch (Exception e) {}
            }
        }
        if (recipeYield <= 0) recipeYield = 1;

        int craftsNeeded = (int) Math.ceil((double) remainingNeeded / recipeYield);
        int produced = craftsNeeded * recipeYield;
        int leftover = produced - remainingNeeded;

        if (leftover > 0) {
            remainingInv.put(item, remainingInv.getOrDefault(item, 0) + leftover);
        }

        for (PlanNode child : node.getChildren()) {
            aggregateSteps(child, craftsNeeded, depth + 1, remainingInv, stepMap, inv);
        }
    }

    private int countItemInInventory(Inventory inv, Item item) {
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
        private final ItemStack output;
        private final String categoryName;
        private final boolean isCraftingTable;
        private final Recipe<?> recipe;
        private final int craftsNeeded;
        private final List<PlanNode> children;
        private final int cost;
        private final String recipeId;
        private final ItemStack[] tagOptions;
        private final String tagSignature;

        public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId) {
            this(output, categoryName, isCraftingTable, recipe, craftsNeeded, children, cost, recipeId, null, null);
        }

        public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children, int cost, String recipeId, ItemStack[] tagOptions, String tagSignature) {
            this.output = output;
            this.categoryName = categoryName;
            this.isCraftingTable = isCraftingTable;
            this.recipe = recipe;
            this.craftsNeeded = craftsNeeded;
            this.children = children;
            this.cost = cost;
            this.recipeId = recipeId;
            this.tagOptions = tagOptions;
            this.tagSignature = tagSignature;
        }

        public ItemStack[] getTagOptions() {
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

        public ItemStack getOutput() {
            return output;
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

        public List<PlanNode> getChildren() {
            return children;
        }
        
        public boolean isLeaf() {
            return children == null || children.isEmpty();
        }
    }
}
