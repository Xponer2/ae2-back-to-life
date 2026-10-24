package appeng.blockentity.misc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import appeng.util.RecipeHolder;

import appeng.recipes.AERecipeTypes;
import appeng.recipes.handlers.ChargerRecipe;

public class ChargerRecipes {

    public static Iterable<RecipeHolder<ChargerRecipe>> getRecipes(World level) {
        return level.getRecipeManager().byType(AERecipeTypes.CHARGER);
    }

    @Nullable
    public static ChargerRecipe findRecipe(World level, ItemStack input) {
        for (var recipe : getRecipes(level)) {
            if (recipe.value().ingredient.test(input)) {
                return recipe.value();
            }
        }

        return null;
    }

    public static boolean allowInsert(World level, ItemStack stack) {
        return findRecipe(level, stack) != null;
    }

    public static boolean allowExtract(World level, ItemStack stack) {
        return findRecipe(level, stack) == null;
    }

}
