package appeng.recipes.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import appeng.api.util.Registries;
import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;
import appeng.recipes.RecipeSerializer;
import appeng.util.CraftingInput;

/**
 * Allows swapping out the storage component of a cell without having to empty it first.
 */
public class StorageCellUpgradeRecipe extends CustomRecipe {
    public static final ResourceLocation SERIALIZER_ID = AppEng.makeId("storage_cell_upgrade");

    private final Item inputCell;
    private final Item inputComponent;
    private final Item resultCell;
    private final Item resultComponent;

    public StorageCellUpgradeRecipe(Item inputCell, Item inputComponent, Item resultCell, Item resultComponent) {
        this.inputCell = inputCell;
        this.inputComponent = inputComponent;
        this.resultCell = resultCell;
        this.resultComponent = resultComponent;
    }

    public static final MapCodec<StorageCellUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            AECodecs.ITEM.fieldOf("input_cell")
                    .forGetter(StorageCellUpgradeRecipe::getInputCell),
            AECodecs.ITEM.fieldOf("input_component")
                    .forGetter(StorageCellUpgradeRecipe::getInputComponent),
            AECodecs.ITEM.fieldOf("result_cell")
                    .forGetter(StorageCellUpgradeRecipe::getResultCell),
            AECodecs.ITEM.fieldOf("result_component")
                    .forGetter(StorageCellUpgradeRecipe::getResultComponent))
            .apply(builder, StorageCellUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCellUpgradeRecipe> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.ITEM, StorageCellUpgradeRecipe::getInputCell,
                    ByteBufCodecs.ITEM, StorageCellUpgradeRecipe::getInputComponent,
                    ByteBufCodecs.ITEM, StorageCellUpgradeRecipe::getResultCell,
                    ByteBufCodecs.ITEM, StorageCellUpgradeRecipe::getResultComponent,
                    StorageCellUpgradeRecipe::new);

    public Item getInputCell() {
        return inputCell;
    }

    public Item getInputComponent() {
        return inputComponent;
    }

    public Item getResultCell() {
        return resultCell;
    }

    public Item getResultComponent() {
        return resultComponent;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(inputCell), Ingredient.fromItem(inputComponent));
    }

    @Override
    public boolean matches(CraftingInput container, World level) {
        var cellsFound = 0;
        var componentsFound = 0;

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == inputCell) {
                    // Also bail out if somehow someone managed to stack cells (since we replace it with a component)
                    cellsFound += stack.getCount();
                } else if (stack.getItem() == inputComponent) {
                    componentsFound++;
                } else {
                    return false;
                }

                if (cellsFound > 1 || componentsFound > 1) {
                    return false;
                }
            }
        }

        return cellsFound == 1 && componentsFound == 1;
    }

    @Override
    public ItemStack getResultItem() {
        return new ItemStack(resultCell);
    }

    @Override
    public ItemStack assemble(CraftingInput container) {
        ItemStack foundCell = ItemStack.EMPTY;
        var componentsFound = 0;

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == inputCell) {
                    if (stack.getCount() > 1 || !foundCell.isEmpty()) {
                        return ItemStack.EMPTY; // More than one cell found
                    }
                    foundCell = stack;
                } else if (stack.getItem() == inputComponent) {
                    if (++componentsFound > 1) {
                        return ItemStack.EMPTY; // More than one component found
                    }
                } else {
                    return ItemStack.EMPTY; // Other item found
                }
            }
        }

        if (foundCell.isEmpty() || componentsFound == 0) {
            return ItemStack.EMPTY;
        } else {
            // The new cell keeps the data of the old one (content, upgrades, ...)
            var result = new ItemStack(resultCell, 1);
            if (foundCell.hasTagCompound()) {
                result.setTagCompound(foundCell.getTagCompound().copy());
            }
            return result;
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < remainder.size(); ++i) {
            var stack = input.getItem(i);
            if (stack.getItem() == inputCell) {
                // We replace the cell with the component since it is unstackable and forced to be in match
                remainder.set(i, new ItemStack(resultComponent));
            } else {
                remainder.set(i, ForgeHooks.getContainerItem(stack));
            }
        }

        return remainder;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StorageCellUpgradeRecipeSerializer.INSTANCE;
    }

}
