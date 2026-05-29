import os

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'r') as f:
    content = f.read()

# Add nextStratum() after background fills to ensure they don't cover text/items
content = content.replace('guiGraphics.fill(0, 0, this.width, this.height, 0xFF2B2B2B);', 'guiGraphics.fill(0, 0, this.width, this.height, 0xFF2B2B2B);\n        guiGraphics.nextStratum();')

content = content.replace('guiGraphics.fill(sidebarWidth, 0, sidebarWidth + 2, this.height, 0xFF111111); // separator', 'guiGraphics.fill(sidebarWidth, 0, sidebarWidth + 2, this.height, 0xFF111111); // separator\n        guiGraphics.nextStratum();')

content = content.replace('guiGraphics.fill(recX, dropY, recX + recWidth, dropY + 12, 0xFF555555);\n        guiGraphics.pose().popMatrix();', 'guiGraphics.fill(recX, dropY, recX + recWidth, dropY + 12, 0xFF555555);\n        guiGraphics.pose().popMatrix();\n        guiGraphics.nextStratum();')

content = content.replace('guiGraphics.renderOutline(drawX - 5, nextY - 5, drawable.getRect().getWidth() + 10, drawable.getRect().getHeight() + 10, 0xFF555555);\n            guiGraphics.pose().popMatrix();', 'guiGraphics.renderOutline(drawX - 5, nextY - 5, drawable.getRect().getWidth() + 10, drawable.getRect().getHeight() + 10, 0xFF555555);\n            guiGraphics.pose().popMatrix();\n            guiGraphics.nextStratum();')

with open('src/main/java/com/craftanyway/client/gui/PlanScreen.java', 'w') as f:
    f.write(content)
