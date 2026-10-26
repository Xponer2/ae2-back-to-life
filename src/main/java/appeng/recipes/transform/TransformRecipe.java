package appeng.recipes.transform;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.blockentity.qnb.QuantumBridgeBlockEntity;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;
import appeng.util.AECodecs;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.RecipeType;
import appeng.recipes.AERecipe;
import appeng.recipes.ingredient.IngredientCodecs;

public final class TransformRecipe implements AERecipe<TransformRecipeInput> {
    @Deprecated
    public static final ResourceLocation TYPE_ID = AppEng.makeId("transform");
    @Deprecated
    public static final RecipeType<TransformRecipe> TYPE = AERecipeTypes.TRANSFORM;

    public static final MapCodec<TransformRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> {
        return builder.group(
                IngredientCodecs.CODEC_NONEMPTY
                        .listOf()
                        .fieldOf("ingredients")
                        .flatXmap(ingredients -> {
                            return DataResult
                                    .success(NonNullList.from(Ingredient.EMPTY, ingredients.toArray(new Ingredient[0])));
                        }, DataResult::success)
                        .forGetter(r -> r.ingredients),
                AECodecs.ITEM_STACK.fieldOf("result").forGetter(r -> r.output),
                TransformCircumstance.CODEC
                        .optionalFieldOf("circumstance",
                                TransformCircumstance.fluid(TransformCircumstance.WATER_TAG))
                        .forGetter(t -> t.circumstance))
                .apply(builder, TransformRecipe::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformRecipe> STREAM_CODEC = StreamCodec.composite(
            IngredientCodecs.STREAM_CODEC.apply(ByteBufCodecs.collection(size -> NonNullList.create())),
            TransformRecipe::getIngredients,
            ByteBufCodecs.ITEM_STACK,
            TransformRecipe::getResultItem,
            TransformCircumstance.STREAM_CODEC,
            TransformRecipe::getCircumstance,
            TransformRecipe::new);

    public final NonNullList<Ingredient> ingredients;
    public final ItemStack output;
    public final TransformCircumstance circumstance;

    public TransformRecipe(NonNullList<Ingredient> ingredients, ItemStack output,
            TransformCircumstance circumstance) {
        this.ingredients = ingredients;
        this.output = output;
        this.circumstance = circumstance;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public TransformCircumstance getCircumstance() {
        return circumstance;
    }

    @Override
    public boolean matches(TransformRecipeInput container, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(TransformRecipeInput container) {
        ItemStack result = getResultItem().copy();
        if (AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(result) && result.getCount() > 1) {
            QuantumBridgeBlockEntity.assignFrequency(result);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TransformRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
