package appeng.integration.modules.itemlists;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import appeng.api.stacks.AEItemKey;
import appeng.core.network.PacketDistributor;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;

public final class CraftingHelper {
    private static final Comparator<GridInventoryEntry> ENTRY_COMPARATOR = Comparator
            .comparing(GridInventoryEntry::getStoredAmount);

    private CraftingHelper() {
    }

    /**
     * 1.12.2: JEI only gives the ingredients of the recipe, not the recipe itself, so the grid is filled from the
     * template items (the server uses them when there is no recipe id).
     *
     * @param ingredients the 3x3 crafting grid, empty ingredients for empty slots
     */
    public static void performTransfer(CraftingTermMenu menu, @Nullable ResourceLocation recipeId,
            List<Ingredient> ingredients, boolean craftMissing) {

        // We send the items in the recipe in any case to serve as a fallback in case the recipe is transient
        var templateItems = findGoodTemplateItems(ingredients, menu);

        ServerboundPacket message = new FillCraftingGridFromRecipePacket(recipeId, templateItems, craftMissing);
        PacketDistributor.sendToServer(message);
    }

    private static NonNullList<ItemStack> findGoodTemplateItems(List<Ingredient> ingredients, MEStorageMenu menu) {
        var ingredientPriorities = EncodingHelper.getIngredientPriorities(menu, ENTRY_COMPARATOR);

        var templateItems = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < ingredients.size() && i < templateItems.size(); i++) {
            var ingredient = ingredients.get(i);
            if (ingredient.getMatchingStacks().length > 0) {
                // Try to find the best item. In case the ingredient is a tag, it might contain versions the
                // player doesn't actually have
                var stack = ingredientPriorities.entrySet()
                        .stream()
                        .filter(e -> e.getKey() instanceof AEItemKey itemKey && itemKey.matches(ingredient))
                        .max(Comparator.comparingInt(Map.Entry::getValue))
                        .map(e -> ((AEItemKey) e.getKey()).toStack())
                        .orElse(ingredient.getMatchingStacks()[0]);
                templateItems.set(i, stack);
            }
        }
        return templateItems;
    }
}
