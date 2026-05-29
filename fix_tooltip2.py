import os

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r') as f:
    content = f.read()

content = content.replace('guiGraphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);', 'guiGraphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);')

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w') as f:
    f.write(content)

