package com.craftanyway.planning;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class RecipeWeights {
    // Defines the default cost of obtaining an item as a raw material.
    public static final int DEFAULT_RAW_COST = 50;

    // Hardcoded raw material weights. Items here are significantly cheaper to get raw than to craft.
    private static final Map<String, Integer> RAW_WEIGHTS = new HashMap<>();

    static {
        // Base resources that should almost always be treated as raw
        addWeight("minecraft:cobblestone", 1);
        addWeight("minecraft:stone", 1);
        addWeight("minecraft:dirt", 1);
        addWeight("minecraft:gravel", 1);
        addWeight("minecraft:sand", 1);
        addWeight("minecraft:glass", 1);
        addWeight("minecraft:iron_ingot", 1);
        addWeight("minecraft:gold_ingot", 1);
        addWeight("minecraft:copper_ingot", 1);
        addWeight("minecraft:diamond", 1);
        addWeight("minecraft:emerald", 1);
        addWeight("minecraft:redstone", 1);
        addWeight("minecraft:lapis_lazuli", 1);
        addWeight("minecraft:coal", 1);
        addWeight("minecraft:charcoal", 1);
        addWeight("minecraft:quartz", 1);
        addWeight("minecraft:netherrack", 1);
        addWeight("minecraft:end_stone", 1);
        addWeight("minecraft:obsidian", 1);
        addWeight("minecraft:clay_ball", 1);
        addWeight("minecraft:flint", 1);
        addWeight("minecraft:string", 1);
        addWeight("minecraft:leather", 1);
        addWeight("minecraft:feather", 1);
        addWeight("minecraft:gunpowder", 1);
        addWeight("minecraft:bone", 1);
        addWeight("minecraft:wheat", 1);
        addWeight("minecraft:sugar_cane", 1);
        addWeight("minecraft:slime_ball", 1);
        addWeight("minecraft:blaze_rod", 1);
        addWeight("minecraft:ender_pearl", 1);
    }
    
    private static void addWeight(String id, int weight) {
        RAW_WEIGHTS.put(id, weight);
        // Add variant without namespace to be safe depending on how item.toString() resolves in current mappings
        if (id.startsWith("minecraft:")) {
            RAW_WEIGHTS.put(id.replace("minecraft:", ""), weight);
        }
    }

    public static int getRawCost(Item item) {
        String id = item.toString();
        return RAW_WEIGHTS.getOrDefault(id, DEFAULT_RAW_COST);
    }
}
