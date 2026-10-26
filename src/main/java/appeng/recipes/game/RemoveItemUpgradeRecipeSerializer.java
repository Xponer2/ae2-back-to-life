package appeng.recipes.game;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class RemoveItemUpgradeRecipeSerializer implements RecipeSerializer<RemoveItemUpgradeRecipe> {
    public static final RemoveItemUpgradeRecipeSerializer INSTANCE = new RemoveItemUpgradeRecipeSerializer();

    @Override
    public MapCodec<RemoveItemUpgradeRecipe> codec() {
        return RemoveItemUpgradeRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, RemoveItemUpgradeRecipe> streamCodec() {
        return RemoveItemUpgradeRecipe.STREAM_CODEC;
    }
}
