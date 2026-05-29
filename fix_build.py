import os

with open('build.gradle', 'r') as f:
    content = f.read()

content = content.replace("def icClazz = ucl.loadClass('net.minecraft.client.KeyMapping\\\')", "def icClazz = ucl.loadClass('net.minecraft.client.KeyMapping\')")
with open('build.gradle', 'w') as f:
    f.write(content)
