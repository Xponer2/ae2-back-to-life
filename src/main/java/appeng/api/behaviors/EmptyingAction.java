package appeng.api.behaviors;

import net.minecraft.util.text.ITextComponent;

import appeng.api.stacks.AEKey;

/**
 * Describes the action of emptying an item into the storage network.
 */
public final class EmptyingAction {
    private final ITextComponent description;
    private final AEKey what;
    private final long maxAmount;

    public EmptyingAction(ITextComponent description, AEKey what, long maxAmount) {
        this.description = description;
        this.what = what;
        this.maxAmount = maxAmount;
    }

    public ITextComponent description() {
        return description;
    }

    public AEKey what() {
        return what;
    }

    public long maxAmount() {
        return maxAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmptyingAction)) return false;
        EmptyingAction that = (EmptyingAction) o;
        return java.util.Objects.equals(description, that.description)
                && java.util.Objects.equals(what, that.what)
                && maxAmount == that.maxAmount;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(description, what, maxAmount);
    }

    @Override
    public String toString() {
        return "EmptyingAction[" + "description=" + description + ", " + "what=" + what + ", " + "maxAmount=" + maxAmount + "]";
    }

}
