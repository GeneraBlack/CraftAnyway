import os

def refactor_imports():
    src_dir = 'src/main/java/com/craftanyway'
    for root, dirs, files in os.walk(src_dir):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f)
                with open(path, 'r', encoding='utf-8') as file:
                    content = file.read()

                content = content.replace('net.minecraftforge.fml', 'net.neoforged.fml')
                content = content.replace('net.minecraftforge.eventbus.api', 'net.neoforged.bus.api')
                content = content.replace('net.minecraftforge.client.event', 'net.neoforged.neoforge.client.event')
                content = content.replace('net.minecraftforge.event', 'net.neoforged.neoforge.event')
                content = content.replace('net.minecraftforge.common.MinecraftForge', 'net.neoforged.neoforge.common.NeoForge')
                content = content.replace('MinecraftForge.EVENT_BUS', 'NeoForge.EVENT_BUS')
                content = content.replace('net.minecraftforge.client.settings.KeyConflictContext', 'net.neoforged.neoforge.client.settings.KeyConflictContext')
                
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(content)

if __name__ == "__main__":
    refactor_imports()
