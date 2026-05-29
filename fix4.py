import os

def fix_screen():
    ps_path = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
    with open(ps_path, 'r', encoding='utf-8') as f:
        ps = f.read()
    
    ps = ps.replace('public void renderBackground(GuiGraphics guiGraphics) {', 'public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {')
    ps = ps.replace('public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {', 'public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {')
    
    with open(ps_path, 'w', encoding='utf-8') as f:
        f.write(ps)

if __name__ == "__main__":
    fix_screen()
