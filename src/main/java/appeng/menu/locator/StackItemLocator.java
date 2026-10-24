package appeng.menu.locator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

/**
 * Provides a static ItemStack, used for accessing a menu host without actually opening the menu.
 */
final class StackItemLocator implements ItemMenuHostLocator {
    private final ItemStack stack;

    public StackItemLocator(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StackItemLocator)) return false;
        StackItemLocator that = (StackItemLocator) o;
        return java.util.Objects.equals(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(stack);
    }

    @Override
    public String toString() {
        return "StackItemLocator[" + "stack=" + stack + "]";
    }

    @Override
    public ItemStack locateItem(EntityPlayer player) {
        return stack;
    }

    @Override
    public @Nullable RayTraceResult hitResult() {
        return null;
    }
}
