package com.craftanyway.jei;

import com.craftanyway.CraftAnyway;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@JeiPlugin
public class CraftAnywayJeiPlugin implements IModPlugin {

    private static IJeiRuntime jeiRuntime;

    @Override
    public @NotNull Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(CraftAnyway.MODID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static IJeiRuntime getJeiRuntime() {
        return jeiRuntime;
    }

    public static ItemStack getIngredientUnderMouse() {
        if (jeiRuntime != null) {
            Optional<ITypedIngredient<?>> ingredientUnderMouse = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse();
            if (ingredientUnderMouse.isPresent()) {
                Object ingredient = ingredientUnderMouse.get().getIngredient();
                if (ingredient instanceof ItemStack itemStack) {
                    return itemStack;
                }
            }
        }
        return null;
    }
}
