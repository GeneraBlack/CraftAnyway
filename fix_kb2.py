import os

filepath = 'src/main/java/com/craftanyway/client/Keybinds.java'
with open(filepath, 'r') as f:
    content = f.read()

old_kb = '''    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.craftanyway"
    );'''
new_kb = '''    public static final KeyMapping PLAN_KEY = new KeyMapping(
            "key.craftanyway.plan",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            KeyMapping.CATEGORY_MISC
    );'''
content = content.replace(old_kb, new_kb)

with open(filepath, 'w') as f:
    f.write(content)
