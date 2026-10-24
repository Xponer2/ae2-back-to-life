package appeng.recipes.game;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.crafting.RecipeSerializer;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

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
