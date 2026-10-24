package appeng.crafting.execution;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;

/**
 * Represents a single "unit" of input for a slot in a crafting/processing pattern. How many units of input are required
 * for a pattern is returned by {@link IPatternDetails.IInput#getMultiplier()}.
 */
public final class InputTemplate {
    private final AEKey key;
    private final long amount;

    public InputTemplate(AEKey key, long amount) {
        this.key = key;
        this.amount = amount;
    }

    public AEKey key() {
        return key;
    }

    public long amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputTemplate)) return false;
        InputTemplate that = (InputTemplate) o;
        return java.util.Objects.equals(key, that.key)
                && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, amount);
    }

    @Override
    public String toString() {
        return "InputTemplate[" + "key=" + key + ", " + "amount=" + amount + "]";
    }

}
