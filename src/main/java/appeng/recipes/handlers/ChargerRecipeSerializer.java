package appeng.recipes.handlers;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class ChargerRecipeSerializer implements RecipeSerializer<ChargerRecipe> {
    public static final ChargerRecipeSerializer INSTANCE = new ChargerRecipeSerializer();

    @Override
    public MapCodec<ChargerRecipe> codec() {
        return ChargerRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChargerRecipe> streamCodec() {
        return ChargerRecipe.STREAM_CODEC;
    }
}
