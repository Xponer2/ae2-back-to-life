package appeng.recipes.handlers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;
import appeng.util.AECodecs;
import appeng.recipes.RecipeInput;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.RecipeType;
import appeng.recipes.AERecipe;
import appeng.recipes.ingredient.IngredientCodecs;

public class ChargerRecipe implements AERecipe<RecipeInput> {
    @Deprecated
    public static final ResourceLocation TYPE_ID = AppEng.makeId("charger");
    @Deprecated
    public static final RecipeType<ChargerRecipe> TYPE = AERecipeTypes.CHARGER;

    public final Ingredient ingredient;
    public final NonNullList<Ingredient> ingredients;
    public final ItemStack result;

    public static final MapCodec<ChargerRecipe> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            IngredientCodecs.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ChargerRecipe::getIngredient),
                            AECodecs.ITEM_STACK.fieldOf("result").forGetter(cr -> cr.result))
                    .apply(builder, ChargerRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargerRecipe> STREAM_CODEC = StreamCodec.composite(
            IngredientCodecs.STREAM_CODEC,
            ChargerRecipe::getIngredient,
            ByteBufCodecs.ITEM_STACK,
            ChargerRecipe::getResultItem,
            ChargerRecipe::new);

    public ChargerRecipe(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
        this.ingredients = NonNullList.from(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean matches(RecipeInput container, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput container) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChargerRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
