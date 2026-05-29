import os

filepath = 'src/main/java/com/craftanyway/client/gui/PlanScreen.java'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("import net.minecraft.client.gui.screens.Screen;", "import net.minecraft.client.gui.screens.Screen;\nimport net.minecraft.client.input.MouseButtonEvent;\nimport net.minecraft.client.input.KeyEvent;")

# mouseClicked
old_mc = '''    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;'''
new_mc = '''    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isPre) {
        if (super.mouseClicked(event, isPre)) return true;
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        int button = event.button();'''
content = content.replace(old_mc, new_mc)

# keyPressed
old_kp = '''    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.craftanyway.client.Keybinds.PLAN_KEY.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))) {'''
new_kp = '''    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (com.craftanyway.client.Keybinds.PLAN_KEY.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(event))) {'''
content = content.replace(old_kp, new_kp)
# Wait! Another one:
old_kp2 = '''        return super.keyPressed(keyCode, scanCode, modifiers);'''
new_kp2 = '''        return super.keyPressed(event);'''
content = content.replace(old_kp2, new_kp2)

# mouseReleased
old_mr = '''    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }'''
new_mr = '''    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }'''
content = content.replace(old_mr, new_mr)

# mouseDragged
old_md = '''    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            scrollOffset = Math.max(0, scrollOffset - (dragY / 10.0));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }'''
new_md = '''    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging) {
            scrollOffset = Math.max(0, scrollOffset - (dragY / 10.0));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }'''
content = content.replace(old_md, new_md)

with open(filepath, 'w') as f:
    f.write(content)

