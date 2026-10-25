package appeng.blockentity.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import appeng.util.CraftingInput;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

/**
 * Implement this on pattern details that support being assembled in the {@link MolecularAssemblerBlockEntity}.
 */
public interface IMolecularAssemblerSupportedPattern extends IPatternDetails {
    ItemStack assemble(CraftingInput input, World level);

    /**
     * The default is to not have remaining items.
     */
    default NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    boolean isItemValid(int slot, AEItemKey key, World level);

    boolean isSlotEnabled(int slot);

    void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor);

    @Override
    default boolean supportsPushInputsToExternalInventory() {
        // Patterns crafted in a molecular assembler are usually pointless to craft in anything else
        return false;
    }

    @FunctionalInterface
    interface CraftingGridAccessor {
        void set(int slot, ItemStack stack);
    }
}
