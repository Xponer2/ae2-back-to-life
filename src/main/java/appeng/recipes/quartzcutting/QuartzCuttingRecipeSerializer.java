package appeng.recipes.quartzcutting;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class QuartzCuttingRecipeSerializer implements RecipeSerializer<QuartzCuttingRecipe> {

    public static final QuartzCuttingRecipeSerializer INSTANCE = new QuartzCuttingRecipeSerializer();

    public QuartzCuttingRecipeSerializer() {
    }

    public MapCodec<QuartzCuttingRecipe> codec() {
        return QuartzCuttingRecipe.CODEC;
    }

    public StreamCodec<RegistryFriendlyByteBuf, QuartzCuttingRecipe> streamCodec() {
        return QuartzCuttingRecipe.STREAM_CODEC;
    }
}
