import os
import glob
import subprocess

classes = glob.glob(os.path.expanduser('~/.gradle/caches/**/output/**/MouseButtonEvent.class'), recursive=True)
if classes:
    newest = max(classes, key=os.path.getmtime)
    print(f"Newest MouseButtonEvent: {newest}")
    subprocess.run(['javap', newest])

classes2 = glob.glob(os.path.expanduser('~/.gradle/caches/**/output/**/Screen.class'), recursive=True)
if classes2:
    newest2 = max(classes2, key=os.path.getmtime)
    print(f"Newest Screen: {newest2}")
    subprocess.run(['javap', newest2])

classes3 = glob.glob(os.path.expanduser('~/.gradle/caches/**/output/**/KeyEvent.class'), recursive=True)
if classes3:
    newest3 = max(classes3, key=os.path.getmtime)
    print(f"Newest KeyEvent: {newest3}")
    subprocess.run(['javap', newest3])

classes4 = glob.glob(os.path.expanduser('~/.gradle/caches/**/output/**/KeyMapping.class'), recursive=True)
if classes4:
    newest4 = max(classes4, key=os.path.getmtime)
    print(f"Newest KeyMapping: {newest4}")
    subprocess.run(['javap', newest4])
