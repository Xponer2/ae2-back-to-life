package appeng.client.gui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.inventory.Slot;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.Rect2i;

/**
 * A stack that is rendered including its absolute on-screen bounds.
 */
public final class StackWithBounds {
    private final GenericStack stack;
    private final Rect2i bounds;

    public StackWithBounds(GenericStack stack, Rect2i bounds) {
        this.stack = stack;
        this.bounds = bounds;
    }

    public GenericStack stack() {
        return stack;
    }

    public Rect2i bounds() {
        return bounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StackWithBounds)) return false;
        StackWithBounds that = (StackWithBounds) o;
        return java.util.Objects.equals(stack, that.stack)
                && java.util.Objects.equals(bounds, that.bounds);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(stack, bounds);
    }

    @Override
    public String toString() {
        return "StackWithBounds[" + "stack=" + stack + ", " + "bounds=" + bounds + "]";
    }

    @Nullable
    public static StackWithBounds fromSlot(AEBaseScreen<?> screen, Slot slot) {
        var item = slot.getStack();
        var stack = GenericStack.unwrapItemStack(item);
        if (stack != null) {
            return new StackWithBounds(
                    stack,
                    new Rect2i(screen.getGuiLeft() + slot.xPos, screen.getGuiTop() + slot.yPos, 16, 16));
        }
        return null;
    }
}
