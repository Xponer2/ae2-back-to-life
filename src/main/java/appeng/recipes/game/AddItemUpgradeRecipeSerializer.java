package appeng.recipes.game;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class AddItemUpgradeRecipeSerializer implements RecipeSerializer<AddItemUpgradeRecipe> {
    public static final AddItemUpgradeRecipeSerializer INSTANCE = new AddItemUpgradeRecipeSerializer();

    @Override
    public MapCodec<AddItemUpgradeRecipe> codec() {
        return AddItemUpgradeRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AddItemUpgradeRecipe> streamCodec() {
        return AddItemUpgradeRecipe.STREAM_CODEC;
    }
}
