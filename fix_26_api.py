import os

files = [
    'src/main/java/com/craftanyway/client/gui/PlanScreen.java',
    'src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java',
    'src/main/java/com/craftanyway/client/gui/CraftingScreenOverlay.java',
    'src/main/java/com/craftanyway/execution/CraftExecutor.java',
    'src/main/java/com/craftanyway/client/Keybinds.java'
]

for file in files:
    if not os.path.exists(file): continue
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. GuiGraphics -> GuiGraphicsExtractor
    content = content.replace('import net.minecraft.client.gui.GuiGraphics;', 'import net.minecraft.client.gui.GuiGraphicsExtractor;')
    content = content.replace('GuiGraphics guiGraphics', 'GuiGraphicsExtractor guiGraphics')
    
    # 2. render -> extractRenderState
    content = content.replace('public void render(GuiGraphicsExtractor', 'public void extractRenderState(GuiGraphicsExtractor')
    
    # 3. renderBackground (remove it and just do nothing or call super if needed. Actually we'll just rename it to something else and call it from extractRenderState)
    content = content.replace('public void renderBackground(', 'private void customRenderBackground(')
    content = content.replace('this.renderBackground(', 'this.customRenderBackground(')
    content = content.replace('super.renderBackground', '// super.renderBackground')

    # 4. displayClientMessage -> sendSystemMessage
    content = content.replace('displayClientMessage(', 'sendSystemMessage(')

    # 5. ClickType -> ContainerInput
    content = content.replace('import net.minecraft.world.inventory.ClickType;', 'import net.minecraft.world.inventory.ContainerInput;')
    content = content.replace('ClickType', 'ContainerInput')

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)
print('Done!')
