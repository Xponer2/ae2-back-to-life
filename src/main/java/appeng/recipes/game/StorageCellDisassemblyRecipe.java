package appeng.recipes.game;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.RecipeType;
import appeng.recipes.RecipeManager;
import appeng.recipes.AERecipe;
import appeng.recipes.RecipeInput;

/**
 * Used to handle disassembly of the (Portable) Storage Cells.
 */
public class StorageCellDisassemblyRecipe implements AERecipe<RecipeInput> {
    public static final MapCodec<StorageCellDisassemblyRecipe> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder
            .group(
                    AECodecs.ITEM.fieldOf("cell")
                            .forGetter(StorageCellDisassemblyRecipe::getStorageCell),
                    AECodecs.ITEM_STACK.listOf().fieldOf("cell_disassembly_items")
                            .forGetter(StorageCellDisassemblyRecipe::getCellDisassemblyItems))
            .apply(builder, StorageCellDisassemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCellDisassemblyRecipe> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.ITEM,
                    StorageCellDisassemblyRecipe::getStorageCell,
                    ByteBufCodecs.ITEM_STACK.apply(ByteBufCodecs.list()),
                    StorageCellDisassemblyRecipe::getCellDisassemblyItems,
                    StorageCellDisassemblyRecipe::new);

    private final List<ItemStack> disassemblyItems;
    private final Item storageCell;

    public StorageCellDisassemblyRecipe(Item storageCell, List<ItemStack> disassemblyItems) {
        this.storageCell = storageCell;
        this.disassemblyItems = disassemblyItems;
    }

    public Item getStorageCell() {
        return this.storageCell;
    }

    public List<ItemStack> getCellDisassemblyItems() {
        return disassemblyItems.stream().map(ItemStack::copy).collect(Collectors.toList());
    }

    /**
     * @return True when any Disassembly Output is specified.
     */
    public boolean canDisassemble() {
        return !this.disassemblyItems.isEmpty();
    }

    /**
     * Used to get the disassembly result based on recipes for the given cell.
     *
     * @param cell The cell item being disassembled.
     * @return An empty list to indicate the cell cannot be disassembled. Note that stacks in the list must be copied by
     *         the caller.
     */
    public static List<ItemStack> getDisassemblyResult(World level, Item cell) {
        var recipeManager = RecipeManager.of(level);

        for (var holder : recipeManager.byType(AERecipeTypes.CELL_DISASSEMBLY)) {
            if (holder.value().storageCell == cell) {
                return holder.value().getCellDisassemblyItems();
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean matches(RecipeInput input, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StorageCellDisassemblyRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return AERecipeTypes.CELL_DISASSEMBLY;
    }
}
