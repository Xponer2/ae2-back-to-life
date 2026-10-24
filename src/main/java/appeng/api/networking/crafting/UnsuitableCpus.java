package appeng.api.networking.crafting;

/**
 * Details about unsuitable crafting CPUs unavailable for a job. Detail for
 * {@link CraftingSubmitErrorCode#NO_SUITABLE_CPU_FOUND}.
 */
public final class UnsuitableCpus {
    private final int offline;
    private final int busy;
    private final int tooSmall;
    private final int excluded;

    public UnsuitableCpus(int offline, int busy, int tooSmall, int excluded) {
        this.offline = offline;
        this.busy = busy;
        this.tooSmall = tooSmall;
        this.excluded = excluded;
    }

    public int offline() {
        return offline;
    }

    public int busy() {
        return busy;
    }

    public int tooSmall() {
        return tooSmall;
    }

    public int excluded() {
        return excluded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnsuitableCpus)) return false;
        UnsuitableCpus that = (UnsuitableCpus) o;
        return offline == that.offline
                && busy == that.busy
                && tooSmall == that.tooSmall
                && excluded == that.excluded;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offline, busy, tooSmall, excluded);
    }

    @Override
    public String toString() {
        return "UnsuitableCpus[" + "offline=" + offline + ", " + "busy=" + busy + ", " + "tooSmall=" + tooSmall + ", " + "excluded=" + excluded + "]";
    }

}
