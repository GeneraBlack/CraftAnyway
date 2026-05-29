import os
import re

files = [
    'src/main/java/com/craftanyway/client/gui/PlanScreen.java',
    'src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java',
    'src/main/java/com/craftanyway/execution/CraftExecutor.java'
]

for file in files:
    if not os.path.exists(file): continue
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # handleInventoryMouseClick -> handleContainerInput
    content = content.replace('handleInventoryMouseClick(', 'handleContainerInput(')

    def repl_draw(m):
        text, x, y, color = m.group(1), m.group(2), m.group(3), m.group(4)
        if text.startswith('"') or text == 'have + "/" + needed' or text == '"Step " + step.stepNumber + ":"':
            comp = f'net.minecraft.network.chat.Component.literal({text})'
        else:
            comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
        return f'guiGraphics.textRenderer().accept({x}, {y}, {comp}.withStyle(s -> s.withColor({color})))'

    content = re.sub(r'guiGraphics\.drawString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_draw, content)
    
    def repl_center(m):
        text, x, y, color = m.group(1), m.group(2), m.group(3), m.group(4)
        if text.startswith('"'):
            comp = f'net.minecraft.network.chat.Component.literal({text})'
        else:
            comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
        return f'guiGraphics.textRenderer().accept(net.minecraft.client.gui.TextAlignment.CENTER, {x}, {y}, {comp}.withStyle(s -> s.withColor({color})))'
        
    content = re.sub(r'guiGraphics\.drawCenteredString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_center, content)

    def repl_draw_shadow(m):
        text, x, y, color, shadow = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
        comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
        return f'guiGraphics.textRenderer().accept({x}, {y}, {comp}.withStyle(s -> s.withColor({color})))'

    content = re.sub(r'guiGraphics\.drawString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_draw_shadow, content)

    def repl_outline(m):
        x, y, w, h, color = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
        return f'guiGraphics.fill({x}, {y}, {x} + {w}, {y} + 1, {color});\n        guiGraphics.fill({x}, {y} + {h} - 1, {x} + {w}, {y} + {h}, {color});\n        guiGraphics.fill({x}, {y} + 1, {x} + 1, {y} + {h} - 1, {color});\n        guiGraphics.fill({x} + {w} - 1, {y} + 1, {x} + {w}, {y} + {h} - 1, {color});'

    content = re.sub(r'guiGraphics\.renderOutline\(([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_outline, content)

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print('Done fixing rendering!')
