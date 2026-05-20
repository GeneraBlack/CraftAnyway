# CraftAnyway - Dynamic Crafting Planner & Auto-Crafter

![CraftAnyway Logo](logo.png)

[![NeoForge](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://neoforged.net/)
[![Loader](https://img.shields.io/badge/Loader-NeoForge-orange.svg)](https://neoforged.net/)
[![JEI](https://img.shields.io/badge/JEI-Required-blue.svg)](https://maven.blamejared.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**CraftAnyway** is the ultimate client-side companion mod for Minecraft 1.21.1 (NeoForge). It integrates directly with **Just Enough Items (JEI)** to provide an interactive panning/zooming visual tech tree, exact inventory-aware mass calculations, a persistent HUD shopping list, and an automated state-machine-driven auto-crafter.

---

## 🚀 Key Features

### 🌲 Interactive Visual Planning Tree (`Plan Screen`)
- Hover over any item in the **JEI panel** and press **P** to open a visual node tree.
- Pan around with your mouse, scroll to zoom, and scale target quantities dynamically using the top-left `[+]` and `[-]` controls.
- Compare multiple crafting pathways side-by-side and lock in your route!

### ⚖️ Exact Dynamic Mass Calculations & Yield Leftovers
- **Top-Down Virtual Inventory Consumption:** Automatically consumes matching inventory items during plan analysis.
- **Leftover Yield Tracking:** If a sub-craft yields 4 items (like Planks) but you only need 2, the remaining 2 are tracked as virtual "leftovers" to satisfy sibling nodes.
- **Accurate Child Scaling:** Child requirements scale down dynamically based on possessed intermediate components (e.g., having 6/8 Planks scales the Log requirement to 1 Log instead of 2!).

### 💎 Premium "Alternatives" HUD & GCD Fraction Simplification
- Dynamically tracks intermediate components you partially possess.
- Displays counts as simplified fractions using Greatest Common Divisor (GCD) math (e.g. possessing 6 of 8 required Planks renders as `3/4 (6/8) Oak Planks`!).

### 🛒 Persistent HUD & Chest Shopping List
- Displays remaining missing raw materials on your main HUD.
- Automatically docks next to Chests and containers.
- Automatically hides items as soon as they are fully satisfied.

### ⚡ Smart Packet-Safe Auto-Crafting
- Click **Craft Plan** inside a Crafting Table to let the state machine automatically place items.
- **Dynamic Scheduling**: Reads container slots at start time and only schedules the exact sub-craft steps needed to create missing components.
- **Safe Click Loop**: Uses a bulletproof Left-click (grab) -> Right-click (place 1) -> Left-click (return) sequence to prevent stack dumps.
- **Leftover Sweeper**: Automatically shift-clicks recipe leftovers (like empty buckets) back to your inventory.
- **Smart Pausing**: Pauses on non-crafting steps (like Smelting) and resumes when clicked again.

---

## 🛠️ Installation & Requirements

- **Minecraft:** `1.21.1`
- **Mod Loader:** **NeoForge** (tested on `21.1.218`)
- **Required Mod:** **Just Enough Items (JEI)** (tested on `19.18.8.213`)
- **Client-Side Only:** This mod does not need to be installed on servers!

---

## 💻 Developer Setup

To build and compile the mod locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/GeneraBlack/CraftAnyway.git
   cd CraftAnyway
   ```
2. Build the project:
   ```bash
   ./gradlew build
   ```
3. Run the Minecraft client:
   ```bash
   ./gradlew runClient
   ```

The compiled jar will be generated inside `build/libs/`.
