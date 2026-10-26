package appeng.recipes.game;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class StorageCellUpgradeRecipeSerializer implements RecipeSerializer<StorageCellUpgradeRecipe> {
    public static final StorageCellUpgradeRecipeSerializer INSTANCE = new StorageCellUpgradeRecipeSerializer();

    @Override
    public MapCodec<StorageCellUpgradeRecipe> codec() {
        return StorageCellUpgradeRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, StorageCellUpgradeRecipe> streamCodec() {
        return StorageCellUpgradeRecipe.STREAM_CODEC;
    }
}
