
import java.lang.reflect.*;

public class DumpAPI {
    public static void main(String[] args) throws Exception {
        dump("net.minecraft.client.gui.screens.Screen");
        dump("net.minecraft.world.entity.player.Player");
        dump("net.minecraft.client.player.LocalPlayer");
        dump("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");
        dump("net.minecraft.client.Minecraft");
    }
    static void dump(String name) {
        System.out.println("--- " + name + " ---");
        try {
            Class<?> c = Class.forName(name);
            for(Method m : c.getMethods()) {
                if (m.getName().toLowerCase().contains("message") || 
                    m.getName().toLowerCase().contains("chat") || 
                    m.getName().toLowerCase().contains("render") ||
                    m.getName().toLowerCase().contains("click")) {
                    
                    System.out.print(m.getName() + "(");
                    Class<?>[] pTypes = m.getParameterTypes();
                    for(int i=0;i<pTypes.length;i++) {
                        System.out.print(pTypes[i].getSimpleName() + (i<pTypes.length-1?", ":""));
                    }
                    System.out.println(")");
                }
            }
        } catch(Exception e) {
            System.out.println("Not found");
        }
    }
}
