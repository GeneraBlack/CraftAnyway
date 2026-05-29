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

    # drawString(font, text, x, y, color)
    # guiGraphics.drawString(this.font, "Step-by-Step Breakdown:", 10, 50, 0xFFFFFFFF);
    # => guiGraphics.textRenderer().accept(10, 50, net.minecraft.network.chat.Component.literal("Step-by-Step Breakdown:").withStyle(s -> s.withColor(0xFFFFFFFF)));
    
    # We'll use regex to match guiGraphics.drawString(..., text, x, y, color)
    def repl_draw(m):
        font, text, x, y, color = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
        # Check if text is already a string literal or variable
        if text.startswith('"'):
            comp = f'net.minecraft.network.chat.Component.literal({text})'
        elif text == 'have + "/" + needed' or text == '"Step " + step.stepNumber + ":"':
            comp = f'net.minecraft.network.chat.Component.literal({text})'
        else:
            comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
            
        return f'guiGraphics.textRenderer().accept({x}, {y}, {comp}.withStyle(s -> s.withColor({color})))'

    content = re.sub(r'guiGraphics\.drawString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_draw, content)
    
    # drawCenteredString
    def repl_center(m):
        font, text, x, y, color = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
        if text.startswith('"'):
            comp = f'net.minecraft.network.chat.Component.literal({text})'
        else:
            comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
        return f'guiGraphics.textRenderer().accept(net.minecraft.client.gui.TextAlignment.CENTER, {x}, {y}, {comp}.withStyle(s -> s.withColor({color})))'
        
    content = re.sub(r'guiGraphics\.drawCenteredString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_center, content)

    # drawString with dropShadow boolean
    # guiGraphics.drawString(this.font, text, x + 17 - this.font.width(text), y + 9, 0xFFFFFFFF, true);
    def repl_draw_shadow(m):
        font, text, x, y, color, shadow = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), m.group(6)
        comp = f'net.minecraft.network.chat.Component.literal(String.valueOf({text}))'
        return f'guiGraphics.textRenderer().accept({x}, {y}, {comp}.withStyle(s -> s.withColor({color})))' # Ignore shadow for now

    content = re.sub(r'guiGraphics\.drawString\([^,]+,\s*(.+?),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_draw_shadow, content)

    # renderOutline -> thin fills
    def repl_outline(m):
        x, y, w, h, color = m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)
        return f'guiGraphics.fill({x}, {y}, {x} + {w}, {y} + 1, {color});\n        guiGraphics.fill({x}, {y} + {h} - 1, {x} + {w}, {y} + {h}, {color});\n        guiGraphics.fill({x}, {y} + 1, {x} + 1, {y} + {h} - 1, {color});\n        guiGraphics.fill({x} + {w} - 1, {y} + 1, {x} + {w}, {y} + {h} - 1, {color});'

    content = re.sub(r'guiGraphics\.renderOutline\(([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,)]+)\);', repl_outline, content)

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print('Done fixing rendering!')
