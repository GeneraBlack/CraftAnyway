import os
import glob
import subprocess

classes = glob.glob(os.path.expanduser('~/.gradle/caches/**/1.21.11*/**/KeyMapping.class'), recursive=True)
if not classes:
    print("Could not find KeyMapping.class for 1.21.11")
else:
    for c in classes:
        if 'output' in c:
            print(f"Found: {c}")
            subprocess.run(['javap', c])
            break

classes2 = glob.glob(os.path.expanduser('~/.gradle/caches/**/1.21.11*/**/KeyMapping\.class'), recursive=True)
if classes2:
    for c in classes2:
        if 'output' in c:
            print(f"Found Category: {c}")
            subprocess.run(['javap', c])
            break
