package appeng.recipes.game;

import com.mojang.serialization.MapCodec;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import appeng.api.upgrades.IUpgradeableItem;
import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.util.Platform;

/**
 * Allows adding upgrades to upgradable items.
 */
public class AddItemUpgradeRecipe extends CustomRecipe {
    public static final AddItemUpgradeRecipe INSTANCE = new AddItemUpgradeRecipe();
    public static final ResourceLocation SERIALIZER_ID = AppEng.makeId("add_item_upgrade");
    private static final NonNullList<Ingredient> INGREDIENTS = NonNullList.create();

    private AddItemUpgradeRecipe() {
        super(CraftingBookCategory.MISC);
    }

    public static final MapCodec<AddItemUpgradeRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, AddItemUpgradeRecipe> STREAM_CODEC = StreamCodec
            .unit(INSTANCE);

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return INGREDIENTS;
    }

    private static ItemStack attemptUpgrade(CraftingInput input) {
        if (input.ingredientCount() < 2) {
            return ItemStack.EMPTY;
        }

        // Find the upgradable item
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.getItem() instanceof IUpgradeableItem upgradableItem) {
                var upgraded = stack.copy();
                var upgrades = upgradableItem.getUpgrades(upgraded);

                for (int j = 0; j < input.size(); j++) {
                    if (j == i) {
                        continue;
                    }
                    var upgrade = input.getItem(j);
                    if (upgrade.isEmpty()) {
                        continue;
                    }
                    // Since we do not like unexpectedly adding all upgrades at once,
                    // we copy with count=1
                    upgrade = Platform.copyWithCount(upgrade, 1);
                    // If we can't insert one of the items, break
                    if (!upgrades.addItems(upgrade).isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }

                return upgraded;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput container, World level) {
        return !attemptUpgrade(container).isEmpty();
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput container) {
        return attemptUpgrade(container);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AddItemUpgradeRecipeSerializer.INSTANCE;
    }

}
