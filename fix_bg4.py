import os
with open('build.gradle', 'r') as f:
    lines = f.readlines()
idx = len(lines)
for i, l in enumerate(lines):
    if l.startswith("tasks.register('dumpCat')") or l.startswith("tasks.register('dumpCat2')") or l.startswith("tasks.register('dumpCat3')"):
        idx = min(idx, i)
with open('build.gradle', 'w') as f:
    f.writelines(lines[:idx])
