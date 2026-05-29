import os
import re

filepath = 'src/main/java/com/craftanyway/client/gui/ShoppingListOverlay.java'
with open(filepath, 'r') as f:
    content = f.read()

content = re.sub(r'\b0xFFFFFF\b', '0xFFFFFFFF', content)
content = re.sub(r'\b0xAAAAAA\b', '0xFFAAAAAA', content)
content = re.sub(r'\b0xFFFFAA\b', '0xFFFFFFAA', content)
content = re.sub(r'\b0x55FF55\b', '0xFF55FF55', content)

with open(filepath, 'w') as f:
    f.write(content)
