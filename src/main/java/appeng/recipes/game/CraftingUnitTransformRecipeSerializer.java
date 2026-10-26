package appeng.recipes.game;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class CraftingUnitTransformRecipeSerializer implements RecipeSerializer<CraftingUnitTransformRecipe> {
    public static final CraftingUnitTransformRecipeSerializer INSTANCE = new CraftingUnitTransformRecipeSerializer();

    private CraftingUnitTransformRecipeSerializer() {
    }

    @Override
    public MapCodec<CraftingUnitTransformRecipe> codec() {
        return CraftingUnitTransformRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CraftingUnitTransformRecipe> streamCodec() {
        return CraftingUnitTransformRecipe.STREAM_CODEC;
    }
}
