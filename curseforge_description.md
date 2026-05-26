# CraftAnyway - Dynamic Crafting Planner & Auto-Crafter

![CraftAnyway Logo](logo.png)

Tired of manually clicking through complex recipes, calculating raw materials in your head, and trying to remember exactly how many planks, sticks, or gears you need for that end-game machine?

**CraftAnyway** is the ultimate client-side companion mod for Minecraft 1.21.1 (NeoForge). It seamlessly integrates with **Just Enough Items (JEI)** to bring you a fully interactive visual planning tree, exact inventory-aware quantity calculations, an explorer's HUD shopping list, and an autonomous state-machine-driven auto-crafter!

---

## Key Features

### 🌲 Interactive Visual Planning Tree (`Plan Screen`)
- Hover over any item in your **JEI panel** and press **P** to instantly calculate a complete crafting tree.
- A beautiful, full-screen panning and zooming GUI opens, displaying a visual node tree. The target item sits at the top, branching down into every sub-ingredient via sleek connecting lines.
- Request multiple outputs using the `[+]` and `[-]` quantity scaling buttons in the top left, and watch the entire tree dynamically scale!
- Select between multiple valid crafting paths (e.g. different wood types or alternative recipe recipes) side-by-side.

### ⚖️ Exact Dynamic Mass Calculations & Virtual Leftovers
- **Virtual Inventory Simulator:** Unlike other calculators, CraftAnyway dynamically evaluates your actual inventory as it traverses the tree.
- **Yield Leftovers Simulation:** Recipes that produce multiple items (like Planks from Logs) are accurately modeled. Leftover items from a sub-craft (e.g., crafting 4 Planks when you only need 2, leaving 2 leftovers) are virtually placed back into your inventory to satisfy other sibling recipe slots!
- **Dynamic Child Scaling:** Child requirements scale down dynamically based on possessed intermediate components. If you need 8 Planks and have 6 Planks, the mod knows you only need to craft 2 Planks, accurately scaling the child requirement down to 1 Log (instead of 2!).

### 📝 Smart Step-by-Step Breakdown
- Replaces confusing "Alternatives" lists with an intuitive **Step-by-Step To-Do List** organized from raw materials (bottom-up) to final assembly.
- Each step intelligently groups the ingredients you need to craft at that stage of the tech tree.

### 🧠 Intelligent Inventory-Aware Tag Resolution
- When a recipe accepts "any" item from a tag (e.g. Any Planks), the planner recursively scans all possibilities.
- It dynamically subtracts costs based on your **actual inventory**, automatically selecting the variant you already have the raw materials for (e.g. choosing Spruce Planks over Oak Planks because you have Spruce Logs).

### 🎯 Interactive Variant Picker
- "Any" tag items chosen by the planner are indicated with a yellow `*` in the visual tree.
- Click on the node to open the **Variant Picker Menu**, allowing you to manually override the planner's choice (e.g. forcing Oak Planks).
- The planner instantly recalculates the entire tech tree and saves your variant preference!

### 🛒 Persistent HUD & Chest Shopping List
- Displays your Crafting Steps on your main HUD.
- Automatically docks next to Chests and containers.
- Numbers turn green dynamically the moment you gather enough of a specific item, keeping you perfectly on track!

### ⚡ Smart Packet-Safe Auto-Crafting
- Open a Crafting Table and press **Craft Plan** to let the mod automatically assemble your items.
- **Dynamic Execution Scheduling:** The auto-crafter reads your actual inventory slots and schedules only the exact missing crafts needed, perfectly skipping sub-crafts if you already possess intermediate components.
- **Safe Click Sequences:** Uses a highly robust click loop (Left-click to grab a stack, Right-click to place exactly 1 item in the grid, Left-click to return the stack) to prevent dumping entire stacks into a slot.
- **Leftover Sweeper:** At the end of each craft, the executor automatically sweeps the grid and shift-clicks any leftovers (like empty buckets) back into your inventory.
- **Smart Pause & Resume:** If a recipe requires a non-crafting step (e.g., Smelting or Crushing), the mod safely pauses, informs you via chat what to produce, and resumes right where you left off when you click the button again.

---

## How to Use

1. **Plan a Craft:** Open your inventory or recipe book, hover over any item in the JEI panel, and press **P**.
2. **Review the Tree:** Drag with your mouse to pan around, scroll to zoom, adjust the target quantity with `[+]` or `[-]`, and click **Select Path** on your preferred crafting route.
3. **Gather Materials:** Explore or search chests while tracking your missing raw materials and simplified intermediates on your HUD shopping list.
4. **Auto-Craft:** Open a Crafting Table and click the **Craft Plan** button in the top left. Watch as the mod dynamically handles all sub-crafts, placing items precisely and clearing the grid for you!

---

## Installation & Requirements

- **Minecraft Version:** 1.21.1
- **Mod Loader:** NeoForge (21.1.218 or newer)
- **Dependency:** **Just Enough Items (JEI)** (19.0.0 or newer)
- **Client-Side Only:** This mod is entirely client-side; you do not need to install it on the server to use it on multiplayer worlds!
