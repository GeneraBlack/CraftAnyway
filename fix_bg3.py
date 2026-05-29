import os
with open('build.gradle', 'r') as f:
    lines = f.readlines()
idx = len(lines)
for i, l in enumerate(lines):
    if l.startswith("tasks.register('dumpScreenMethods')") or l.startswith("tasks.register('dumpMoreMethods')") or l.startswith("tasks.register('dumpFixedMethods')"):
        idx = min(idx, i)
with open('build.gradle', 'w') as f:
    f.writelines(lines[:idx])
