import os

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r') as f:
    content = f.read()

content = content.replace('guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);', 'guiGraphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);')

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w') as f:
    f.write(content)

