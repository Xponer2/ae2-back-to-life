package appeng.recipes.transform;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public final class TransformRecipeInput implements RecipeInput {
    private final List<ItemStack> items;

    public TransformRecipeInput(List<ItemStack> items) {
        this.items = items;
    }

    public List<ItemStack> items() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransformRecipeInput)) return false;
        TransformRecipeInput that = (TransformRecipeInput) o;
        return java.util.Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(items);
    }

    @Override
    public String toString() {
        return "TransformRecipeInput[" + "items=" + items + "]";
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
