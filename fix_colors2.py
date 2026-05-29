import os
import re

filepath = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('0xFFFFFFFFFF', '0xFFFFFFFF')
content = content.replace('0xFFFFFFAA', '0xFFFFFFAA') # if it was messed up
content = content.replace('0xFFAAAAAA', '0xFFAAAAAA') 
# Actually let's just make sure it's exactly 0xFFFFFFFF
content = re.sub(r'0xFFF+AA', '0xFFFFFFAA', content)

with open(filepath, 'w') as f:
    f.write(content)
