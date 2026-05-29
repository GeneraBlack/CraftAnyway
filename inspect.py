import os
import glob
import subprocess

# Find the minecraft client jar
jars = glob.glob(os.path.expanduser('~/.gradle/caches/**/minecraft-1.21.11-client.jar'), recursive=True)
if not jars:
    print("Could not find minecraft jar")
else:
    jar = jars[0]
    print(f"Found jar: {jar}")
    # run javap on Screen
    subprocess.run(['javap', '-cp', jar, 'net.minecraft.client.gui.screens.Screen'])
    subprocess.run(['javap', '-cp', jar, 'net.minecraft.client.gui.components.events.ContainerEventHandler'])
    subprocess.run(['javap', '-cp', jar, 'com.mojang.blaze3d.platform.InputConstants'])
    subprocess.run(['javap', '-cp', jar, 'net.minecraft.client.KeyMapping'])
    subprocess.run(['javap', '-cp', jar, 'net.minecraft.client.input.MouseButtonEvent'])
    subprocess.run(['javap', '-cp', jar, 'net.minecraft.client.input.KeyEvent'])
