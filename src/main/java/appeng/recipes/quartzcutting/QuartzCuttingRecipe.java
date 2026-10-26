package appeng.recipes.quartzcutting;

import java.util.ArrayList;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.RecipeMatcher;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.datagen.providers.tags.ConventionTags;
import appeng.util.AECodecs;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.game.CustomRecipe;
import appeng.util.CraftingInput;
import appeng.recipes.ingredient.IngredientCodecs;

/**
 * A shapeless crafting recipe that damages the quartz knife instead of using it up.
 */
public class QuartzCuttingRecipe extends CustomRecipe {
    private static final java.util.Random RANDOM = new java.util.Random();
    static final int MAX_HEIGHT = 3;
    static final int MAX_WIDTH = 3;
    public static final MapCodec<QuartzCuttingRecipe> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder.group(
            AECodecs.ITEM_STACK.fieldOf("result").forGetter(QuartzCuttingRecipe::getResult),
            IngredientCodecs.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((r) -> {
                var ingredients = r.toArray(new Ingredient[0]);
                if (ingredients.length == 0) {
                    return DataResult.error("No ingredients for quartz cutting recipe");
                } else {
                    return ingredients.length > MAX_HEIGHT * MAX_WIDTH
                            ? DataResult.<NonNullList<Ingredient>>error(
                                    "Too many ingredients for quartz cutting recipe. The maximum is: "
                                            + MAX_HEIGHT * MAX_WIDTH)
                            : DataResult.success(NonNullList.from(Ingredient.EMPTY, ingredients));
                }
            }, DataResult::success).forGetter(QuartzCuttingRecipe::getIngredients))
            .apply(builder, QuartzCuttingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuartzCuttingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.ITEM_STACK, QuartzCuttingRecipe::getResult,
            StreamCodec.of(
                    (buffer, value) -> {
                        buffer.writeVarInt(value.size());
                        for (var ingredient : value) {
                            IngredientCodecs.STREAM_CODEC.encode(buffer, ingredient);
                        }
                    },
                    buffer -> {
                        int count = buffer.readVarInt();
                        NonNullList<Ingredient> ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
                        ingredients.replaceAll(ignored -> IngredientCodecs.STREAM_CODEC.decode(buffer));
                        return ingredients;
                    }),
            QuartzCuttingRecipe::getIngredients,
            QuartzCuttingRecipe::new);

    final ItemStack result;
    final NonNullList<Ingredient> ingredients;

    public QuartzCuttingRecipe(ItemStack result, NonNullList<Ingredient> ingredients) {
        this.result = result;
        this.ingredients = ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return QuartzCuttingRecipeSerializer.INSTANCE;
    }

    @Override
    public ItemStack getResultItem() {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    /**
     * Shown in the recipe book like a normal shapeless recipe.
     */
    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        var nonEmptyItems = new ArrayList<ItemStack>(input.ingredientCount());
        for (var item : input.items()) {
            if (!item.isEmpty()) {
                nonEmptyItems.add(item);
            }
        }
        return RecipeMatcher.findMatches(nonEmptyItems, this.ingredients) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    private ItemStack getResult() {
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        boolean damagedKnife = false;

        for (int i = 0; i < remainingItems.size(); i++) {
            ItemStack item = input.getItem(i);

            if (!damagedKnife && ConventionTags.QUARTZ_KNIFE.contains(item)) {
                damagedKnife = true;
                var result = item.copy();

                // Unbreaking of the crafting player applies, the knife breaks when it is used up
                var player = ForgeHooks.getCraftingPlayer();
                var broken = result.attemptDamageItem(1, RANDOM,
                        player instanceof EntityPlayerMP serverPlayer ? serverPlayer : null);
                remainingItems.set(i, broken ? ItemStack.EMPTY : result);
            } else {
                remainingItems.set(i, ForgeHooks.getContainerItem(item));
            }
        }

        return remainingItems;
    }
}
