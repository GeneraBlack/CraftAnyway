import os
with open('build.gradle', 'r') as f:
    content = f.read()

content = content.replace("println '--- MouseButtonEvent Methods ---'", "def mev = ucl.loadClass('net.minecraft.client.input.MouseButtonEvent')\n            println '--- MouseButtonEvent Methods ---'\n            mev.getDeclaredMethods().each { m -> println m.name + ' : ' + m.getParameterTypes().collect { it.name } }\n            def kev = ucl.loadClass('net.minecraft.client.input.KeyEvent')\n            println '--- KeyEvent Methods ---'\n            kev.getDeclaredMethods().each { m -> println m.name + ' : ' + m.getParameterTypes().collect { it.name } }")
with open('build.gradle', 'w') as f:
    f.write(content)
