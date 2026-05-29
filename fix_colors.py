import os
import re

filepath = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('0xFFFFFF', '0xFFFFFFFF')
content = content.replace('0xAAAAAA', '0xFFAAAAAA')
content = content.replace('0xFFFFAA', '0xFFFFFFAA')
content = content.replace('16777215', '0xFFFFFFFF')
# But wait, 0xFFFFFFFF could be replaced again if I do it blindly? No, 0xFFFFFF is 6 Fs, 0xFFFFFFFF is 8 Fs.
# If I replace '0xFFFFFF' it will also match inside '0xFFFFFFFF'!
# Let's use regex.
content = re.sub(r'\b0xFFFFFF\b', '0xFFFFFFFF', content)
content = re.sub(r'\b0xAAAAAA\b', '0xFFAAAAAA', content)
content = re.sub(r'\b0xFFFFAA\b', '0xFFFFFFAA', content)
content = re.sub(r'\b16777215\b', '0xFFFFFFFF', content)

with open(filepath, 'w') as f:
    f.write(content)
