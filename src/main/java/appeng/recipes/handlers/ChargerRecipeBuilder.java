package appeng.recipes.handlers;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import appeng.api.util.ItemLike;
import appeng.api.util.TagKey;

public class ChargerRecipeBuilder {

    public static void charge(RecipeOutput consumer, ResourceLocation id, ItemLike input, ItemLike output) {
        consumer.accept(id, new ChargerRecipe(Ingredient.of(input), output.asItem().getDefaultInstance()), null);
    }

    public static void charge(RecipeOutput consumer, ResourceLocation id, TagKey<Item> input,
            ItemLike output) {
        consumer.accept(id, new ChargerRecipe(Ingredient.of(input), output.asItem().getDefaultInstance()), null);
    }

    public static void charge(RecipeOutput consumer, ResourceLocation id, Ingredient input,
            ItemLike output) {
        consumer.accept(id, new ChargerRecipe(input, output.asItem().getDefaultInstance()), null);
    }
}
