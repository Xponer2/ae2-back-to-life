package appeng.recipes.game;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

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

/**
 * Allows adding upgrades to upgradable items.
 */
public class RemoveItemUpgradeRecipe extends CustomRecipe {
    public static final RemoveItemUpgradeRecipe INSTANCE = new RemoveItemUpgradeRecipe();

    public static final ResourceLocation SERIALIZER_ID = AppEng.makeId("remove_item_upgrade");
    private static final NonNullList<Ingredient> INGREDIENTS = NonNullList.create();

    private RemoveItemUpgradeRecipe() {
        super(CraftingBookCategory.MISC);
    }

    public static final MapCodec<RemoveItemUpgradeRecipe> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveItemUpgradeRecipe> STREAM_CODEC = StreamCodec
            .unit(INSTANCE);

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return INGREDIENTS;
    }

    final static class RemovalResult {
        private final ItemStack upgradableItem;
        private final ItemStack upgrade;

        public RemovalResult(ItemStack upgradableItem, ItemStack upgrade) {
            this.upgradableItem = upgradableItem;
            this.upgrade = upgrade;
        }

        public ItemStack upgradableItem() {
            return upgradableItem;
        }

        public ItemStack upgrade() {
            return upgrade;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RemovalResult)) return false;
            RemovalResult that = (RemovalResult) o;
            return java.util.Objects.equals(upgradableItem, that.upgradableItem)
                    && java.util.Objects.equals(upgrade, that.upgrade);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(upgradableItem, upgrade);
        }

        @Override
        public String toString() {
            return "RemovalResult[" + "upgradableItem=" + upgradableItem + ", " + "upgrade=" + upgrade + "]";
        }

    }

    @Nullable
    private static RemovalResult attemptRemoval(CraftingInput input) {
        if (input.size() != 1) {
            return null;
        }

        var item = input.getItem(0);
        if (!(item.getItem() instanceof IUpgradeableItem upgradableItem)) {
            return null;
        }

        var upgradable = item.copy();
        var upgrades = upgradableItem.getUpgrades(upgradable);
        for (int i = 0; i < upgrades.size(); i++) {
            var upgrade = upgrades.extractItem(i, 1, false);
            if (!upgrade.isEmpty()) {
                return new RemovalResult(upgradable, upgrade);
            }
        }

        return null;
    }

    @Override
    public boolean matches(CraftingInput input, World level) {
        return attemptRemoval(input) != null;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    /**
     * Assemble returns the extracted upgrade.
     */
    @Override
    public ItemStack assemble(CraftingInput input) {
        var result = attemptRemoval(input);
        return result != null ? result.upgrade() : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = attemptRemoval(input);
        if (result == null || input.size() != 1) {
            return super.getRemainingItems(input);
        }

        return NonNullList.of(ItemStack.EMPTY, result.upgradableItem());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RemoveItemUpgradeRecipeSerializer.INSTANCE;
    }

}
