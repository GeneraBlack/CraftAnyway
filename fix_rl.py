import os
import glob

for filepath in glob.glob('src/main/java/**/*.java', recursive=True):
    with open(filepath, 'r') as f:
        content = f.read()
    
    new_content = content.replace('net.minecraft.resources.ResourceLocation', 'net.minecraft.resources.Identifier')
    new_content = new_content.replace('ResourceLocation.', 'Identifier.')
    new_content = new_content.replace('ResourceLocation ', 'Identifier ')
    new_content = new_content.replace('ResourceLocation>', 'Identifier>')
    new_content = new_content.replace('(ResourceLocation', '(Identifier')
    
    if content != new_content:
        with open(filepath, 'w') as f:
            f.write(new_content)
