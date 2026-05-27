package com.craftanyway.execution;

import com.craftanyway.planning.CraftingPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = com.craftanyway.CraftAnyway.MODID, bus = EventBusSubscriber.Bus.FORGE)
public class CraftExecutor {

    private static boolean isExecuting = false;
    private static CraftingScreen currentScreen = null;
    private static List<CraftingPlan.PlanNode> nodesToCraft = new ArrayList<>();
    private static int currentTickDelay = 0;
    private static final int TICK_DELAY = 2; // Delay between clicks
    
    private static int state = 0; 
    // 0 = idle/next recipe, 1 = moving items to grid, 2 = clicking output, 3 = clearing grid
    
    private static CraftingPlan.PlanNode currentNode = null;
    private static int currentIngredientIndex = 0;

    public static void startExecution(CraftingPlan plan, CraftingScreen screen) {
        if (isExecuting) return;
        currentScreen = screen;
        nodesToCraft.clear();
        
        // Initialize virtual inventory with player's actual container slots
        java.util.Map<net.minecraft.world.item.Item, Integer> remainingInv = new java.util.HashMap<>();
        for (int i = 10; i < 46; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (!stack.isEmpty()) {
                remainingInv.put(stack.getItem(), remainingInv.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }
        
        if (plan.getRootNode() != null) {
            flattenPlan(plan.getRootNode(), (int)plan.getRootNode().getAmount(), remainingInv, nodesToCraft, screen);
        }
        
        if (!nodesToCraft.isEmpty()) {
            isExecuting = true;
            state = 0;
            currentTickDelay = 0;
            currentNode = null;
        }
    }

    private static void flattenPlan(CraftingPlan.PlanNode node, int neededAmount, java.util.Map<net.minecraft.world.item.Item, Integer> remainingInv, 
                                   List<CraftingPlan.PlanNode> list, CraftingScreen screen) {
        if (node == null) return;

        Object outIng = node.getOutput().getIngredient();
        if (!(outIng instanceof ItemStack)) return;
        ItemStack output = (ItemStack) outIng;
        net.minecraft.world.item.Item item = output.getItem();

        // 1. Consume virtual inventory for intermediate items (non-leaf)
        int remainingNeeded = neededAmount;
        if (!node.isLeaf()) {
            int haveAvailable = remainingInv.getOrDefault(item, 0);
            int used = Math.min(neededAmount, haveAvailable);
            remainingInv.put(item, haveAvailable - used);
            remainingNeeded = neededAmount - used;
        }

        // 2. If fully satisfied by inventory, stop traversing
        if (remainingNeeded <= 0) {
            return;
        }

        // 3. If leaf node (raw material), no crafts to queue
        if (node.isLeaf()) {
            return;
        }

        // 4. Calculate yield and leftovers for intermediate craft
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

        // 5. Recurse on children FIRST (bottom-up dependency order)
        for (CraftingPlan.PlanNode child : node.getChildren()) {
            flattenPlan(child, craftsNeeded, remainingInv, list, screen);
        }

        // 6. Queue the crafts for this intermediate node
        for (int i = 0; i < craftsNeeded; i++) {
            list.add(node);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        if (!isExecuting) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != currentScreen) {
            isExecuting = false; // Screen closed
            return;
        }
        
        if (currentTickDelay > 0) {
            currentTickDelay--;
            return;
        }
        
        if (state == 0) {
            if (nodesToCraft.isEmpty()) {
                isExecuting = false;
                if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Crafting complete!"), false);
                return;
            }
            currentNode = nodesToCraft.remove(0);
            currentIngredientIndex = 0;
            state = 1;
            currentTickDelay = TICK_DELAY;
        } else if (state == 1) {
            // Move items to grid
            if (currentNode.isCraftingTable() && currentNode.getRecipe() instanceof CraftingRecipe recipe) {
                List<Ingredient> ingredients = recipe.getIngredients();
                
                if (currentIngredientIndex < ingredients.size()) {
                    Ingredient ing = ingredients.get(currentIngredientIndex);
                    if (!ing.isEmpty()) {
                        // Find item in inventory
                        int invSlot = findItemInInventory(ing, currentScreen);
                        if (invSlot != -1) {
                            // Calculate proper grid slot index (1 to 9) based on shaped/shapeless recipe
                            int gridSlot = currentIngredientIndex + 1; // Fallback to shapeless index
                            if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                                int w = shaped.getWidth();
                                int col = currentIngredientIndex % w;
                                int row = currentIngredientIndex / w;
                                gridSlot = (row * 3 + col) + 1;
                            }
                            
                            // Safe placement sequence: Left-click stack -> Right-click slot -> Left-click stack back
                            clickSlot(invSlot, 0, ClickType.PICKUP);
                            clickSlot(gridSlot, 1, ClickType.PICKUP);
                            clickSlot(invSlot, 0, ClickType.PICKUP);
                        } else {
                            // Missing item, abort
                            isExecuting = false;
                            if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Missing items for craft! Aborting."), false);
                            return;
                        }
                    }
                    currentIngredientIndex++;
                    currentTickDelay = TICK_DELAY;
                } else {
                    state = 2; // Move to click output
                    currentTickDelay = TICK_DELAY;
                }
            } else {
                // Non-crafting step, pause execution
                isExecuting = false;
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Paused! Please manually produce: " 
                            + ((ItemStack)currentNode.getOutput().getIngredient()).getHoverName().getString() 
                            + " via " + currentNode.getCategoryName() 
                            + ". Press 'Craft Plan' to resume once you have it."), 
                        false
                    );
                }
                return;
            }
        } else if (state == 2) {
            // Click output slot
            clickSlot(0, 0, ClickType.QUICK_MOVE); // Shift click output
            state = 3;
            currentTickDelay = TICK_DELAY;
        } else if (state == 3) {
            // Clear grid of any remaining items (like empty buckets or leftover items)
            for (int i = 1; i <= 9; i++) {
                if (!currentScreen.getMenu().getSlot(i).getItem().isEmpty()) {
                    clickSlot(i, 0, ClickType.QUICK_MOVE);
                }
            }
            state = 0;
            currentTickDelay = TICK_DELAY;
        }
    }
    
    private static void clickSlot(int slot, int button, ClickType clickType) {
        if (currentScreen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleInventoryMouseClick(
                currentScreen.getMenu().containerId, 
                slot, 
                button, 
                clickType, 
                mc.player
            );
        }
    }
    
    private static int findItemInInventory(Ingredient ingredient, CraftingScreen screen) {
        // Inventory slots are 10 to 45
        // We look at the slots in the container
        for (int i = 10; i < 46; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return i;
            }
        }
        return -1;
    }
}
