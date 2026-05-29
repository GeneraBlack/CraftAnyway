import os
import glob
import subprocess

classes = glob.glob(os.path.expanduser('~/.gradle/caches/**/MouseButtonEvent.class'), recursive=True)
if not classes:
    print("Could not find MouseButtonEvent.class")
else:
    print(f"Found: {classes[0]}")
    subprocess.run(['javap', classes[0]])
    
classes2 = glob.glob(os.path.expanduser('~/.gradle/caches/**/KeyMapping.class'), recursive=True)
if classes2:
    subprocess.run(['javap', classes2[0]])
    
classes3 = glob.glob(os.path.expanduser('~/.gradle/caches/**/Screen.class'), recursive=True)
if classes3:
    subprocess.run(['javap', classes3[0]])
