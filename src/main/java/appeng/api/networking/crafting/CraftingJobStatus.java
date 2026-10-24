package appeng.api.networking.crafting;

import appeng.api.stacks.GenericStack;

public final class CraftingJobStatus {
    private final GenericStack crafting;
    private final long totalItems;
    private final long progress;
    private final long elapsedTimeNanos;

    public CraftingJobStatus(GenericStack crafting, long totalItems, long progress, long elapsedTimeNanos) {
        this.crafting = crafting;
        this.totalItems = totalItems;
        this.progress = progress;
        this.elapsedTimeNanos = elapsedTimeNanos;
    }

    public GenericStack crafting() {
        return crafting;
    }

    public long totalItems() {
        return totalItems;
    }

    public long progress() {
        return progress;
    }

    public long elapsedTimeNanos() {
        return elapsedTimeNanos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftingJobStatus)) return false;
        CraftingJobStatus that = (CraftingJobStatus) o;
        return java.util.Objects.equals(crafting, that.crafting)
                && totalItems == that.totalItems
                && progress == that.progress
                && elapsedTimeNanos == that.elapsedTimeNanos;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(crafting, totalItems, progress, elapsedTimeNanos);
    }

    @Override
    public String toString() {
        return "CraftingJobStatus[" + "crafting=" + crafting + ", " + "totalItems=" + totalItems + ", " + "progress=" + progress + ", " + "elapsedTimeNanos=" + elapsedTimeNanos + "]";
    }

}
