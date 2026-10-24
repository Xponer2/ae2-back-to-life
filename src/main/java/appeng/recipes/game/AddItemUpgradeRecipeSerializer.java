package appeng.recipes.game;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.crafting.RecipeSerializer;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

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
