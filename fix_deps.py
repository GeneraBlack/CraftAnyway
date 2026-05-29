import os

with open('gradle.properties', 'r') as f:
    content = f.read()
content = content.replace('mekanism_version=1.21.1-10.7.19.85\n', '')
with open('gradle.properties', 'w') as f:
    f.write(content)

with open('build.gradle', 'r') as f:
    content = f.read()
content = content.replace('compileOnly "mekanism:Mekanism:-:api"', '')
content = content.replace('runtimeOnly "mekanism:Mekanism:-"', '')
with open('build.gradle', 'w') as f:
    f.write(content)
