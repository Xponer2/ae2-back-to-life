package appeng.recipes.game;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.crafting.RecipeSerializer;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

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
