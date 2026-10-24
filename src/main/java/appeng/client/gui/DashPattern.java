package appeng.client.gui;

public final class DashPattern {
    private final float width;
    private final float onLength;
    private final float offLength;
    private final int color;
    private final float animationCycleMs;

    public DashPattern(float width, float onLength, float offLength, int color, float animationCycleMs) {
        this.width = width;
        this.onLength = onLength;
        this.offLength = offLength;
        this.color = color;
        this.animationCycleMs = animationCycleMs;
    }

    public float width() {
        return width;
    }

    public float onLength() {
        return onLength;
    }

    public float offLength() {
        return offLength;
    }

    public int color() {
        return color;
    }

    public float animationCycleMs() {
        return animationCycleMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DashPattern)) return false;
        DashPattern that = (DashPattern) o;
        return Float.compare(width, that.width) == 0
                && Float.compare(onLength, that.onLength) == 0
                && Float.compare(offLength, that.offLength) == 0
                && color == that.color
                && Float.compare(animationCycleMs, that.animationCycleMs) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(width, onLength, offLength, color, animationCycleMs);
    }

    @Override
    public String toString() {
        return "DashPattern[" + "width=" + width + ", " + "onLength=" + onLength + ", " + "offLength=" + offLength + ", " + "color=" + color + ", " + "animationCycleMs=" + animationCycleMs + "]";
    }

    float length() {
        return onLength + offLength;
    }
}
