import os

filepath = 'src/main/java/com/craftanyway/client/Keybinds.java'
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("import net.minecraft.client.KeyMapping;", "import net.minecraft.client.KeyMapping;\nimport net.minecraft.client.input.KeyEvent;")

old_keybind = '''    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.craftanyway"
    );'''
new_keybind = '''    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.craftanyway" // wait, if it's an enum/record maybe it fails here. we'll test. wait I'll try without KeyConflictContext
    );'''
# Actually wait, the old constructor took String. The new one takes KeyMapping.Category. Wait, let me just try 
et.neoforged.neoforge.client.settings.KeyMappingManager?
# I'll try changing it to:
new_keybind2 = '''    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.craftanyway"
    );'''
# wait, KeyMapping(String, Type, int, String) was also supported in 1.21.8. Let's see if it works here.

old_km1 = '''InputConstants.getKey(event.getKey(), event.getScanCode())'''
new_km1 = '''InputConstants.getKey(event)'''
content = content.replace(old_km1, new_km1)

old_km2 = '''InputConstants.getKey(event.getKeyCode(), event.getScanCode())'''
new_km2 = '''InputConstants.getKey(event)''' # Wait, event in clientTick might be a different event! Oh wait, wait wait!
content = content.replace(old_km2, new_km2)

with open(filepath, 'w') as f:
    f.write(content)
