package com.craftanyway.planning;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static class PlanResult {
        public final Map<String, ItemStack> rawMaterials;
        public final Map<String, AlternativeItem> alternatives;

        public PlanResult(Map<String, ItemStack> rawMaterials, Map<String, AlternativeItem> alternatives) {
            this.rawMaterials = rawMaterials;
            this.alternatives = alternatives;
        }
    }

    public PlanResult calculateRequirements(Inventory inv) {
        Map<String, ItemStack> rawMaterials = new HashMap<>();
        Map<String, AlternativeItem> alternatives = new HashMap<>();
        
        // Initialize virtual inventory with player's inventory counts
        Map<Item, Integer> remainingInv = new HashMap<>();
        if (inv != null) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    remainingInv.put(stack.getItem(), remainingInv.getOrDefault(stack.getItem(), 0) + stack.getCount());
                }
            }
        }
        
        // Dynamic top-down aggregation
        if (rootNode != null) {
            aggregate(rootNode, rootNode.getOutput().getCount(), remainingInv, rawMaterials, alternatives, inv);
        }
        
        return new PlanResult(rawMaterials, alternatives);
    }

    private void aggregate(PlanNode node, int neededAmount, Map<Item, Integer> remainingInv, 
                           Map<String, ItemStack> rawMaterials, Map<String, AlternativeItem> alternatives, Inventory inv) {
        if (node == null) return;

        ItemStack output = node.getOutput();
        Item item = output.getItem();

        // 1. Consume virtual inventory for intermediate items (non-leaf)
        int remainingNeeded = neededAmount;
        if (!node.isLeaf()) {
            int haveAvailable = remainingInv.getOrDefault(item, 0);
            int used = Math.min(neededAmount, haveAvailable);
            remainingInv.put(item, haveAvailable - used);
            remainingNeeded = neededAmount - used;

            // 2. Track alternative if player has a partial count of this intermediate component
            if (inv != null) {
                int actualHave = countItemInInventory(inv, item);
                if (actualHave > 0 && actualHave < neededAmount) {
                    String key = item.getDescriptionId();
                    if (!alternatives.containsKey(key)) {
                        ItemStack copy = output.copy();
                        copy.setCount(actualHave);
                        alternatives.put(key, new AlternativeItem(copy, actualHave, neededAmount));
                    } else {
                        AlternativeItem existing = alternatives.get(key);
                        int newNeeded = existing.getNeeded() + neededAmount;
                        ItemStack copy = output.copy();
                        copy.setCount(actualHave);
                        alternatives.put(key, new AlternativeItem(copy, actualHave, newNeeded));
                    }
                }
            }
        }

        // 3. If fully satisfied, stop recursing
        if (remainingNeeded <= 0) {
            return;
        }

        // 4. If leaf node, add remaining requirements to raw materials
        if (node.isLeaf()) {
            String key = item.getDescriptionId();
            if (rawMaterials.containsKey(key)) {
                rawMaterials.get(key).grow(remainingNeeded);
            } else {
                ItemStack copy = output.copy();
                copy.setCount(remainingNeeded);
                rawMaterials.put(key, copy);
            }
        } else {
            // 5. Craft intermediate item: calculate yield, leftovers, and child requirements
            int recipeYield = 1;
            if (node.getRecipe() != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    try {
                        ItemStack result = node.getRecipe().getResultItem(mc.level.registryAccess());
                        if (!result.isEmpty()) {
                            recipeYield = result.getCount();
                        }
                    } catch (Exception e) {
                        // fallback
                    }
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
                aggregate(child, craftsNeeded, remainingInv, rawMaterials, alternatives, inv);
            }
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

    public static class AlternativeItem {
        private final ItemStack stack;
        private final int have;
        private final int needed;
        
        public AlternativeItem(ItemStack stack, int have, int needed) {
            this.stack = stack;
            this.have = have;
            this.needed = needed;
        }
        
        public ItemStack getStack() {
            return stack;
        }
        
        public int getHave() {
            return have;
        }
        
        public int getNeeded() {
            return needed;
        }
    }


    public static class PlanNode {
        private final ItemStack output;
        private final String categoryName;
        private final boolean isCraftingTable;
        private final Recipe<?> recipe;
        private final int craftsNeeded;
        private final List<PlanNode> children;

        public PlanNode(ItemStack output, String categoryName, boolean isCraftingTable, Recipe<?> recipe, int craftsNeeded, List<PlanNode> children) {
            this.output = output;
            this.categoryName = categoryName;
            this.isCraftingTable = isCraftingTable;
            this.recipe = recipe;
            this.craftsNeeded = craftsNeeded;
            this.children = children;
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
