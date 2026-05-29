import os

filepath = 'build.gradle'
with open(filepath, 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if line.startswith("tasks.register('dumpCat')"):
        skip = True
    if not skip:
        new_lines.append(line)
    if skip and line.strip() == "}":
        # check if it's the end of the task, might be multiple }
        pass # Actually wait, it's easier to just find the string "dumpCat" and remove everything after it
