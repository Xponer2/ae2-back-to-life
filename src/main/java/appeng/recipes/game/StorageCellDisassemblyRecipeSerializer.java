package appeng.recipes.game;

import com.mojang.serialization.MapCodec;


import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.RecipeSerializer;

public class StorageCellDisassemblyRecipeSerializer implements RecipeSerializer<StorageCellDisassemblyRecipe> {
    public static final StorageCellDisassemblyRecipeSerializer INSTANCE = new StorageCellDisassemblyRecipeSerializer();

    private StorageCellDisassemblyRecipeSerializer() {
    }

    @Override
    public MapCodec<StorageCellDisassemblyRecipe> codec() {
        return StorageCellDisassemblyRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, StorageCellDisassemblyRecipe> streamCodec() {
        return StorageCellDisassemblyRecipe.STREAM_CODEC;
    }
}
