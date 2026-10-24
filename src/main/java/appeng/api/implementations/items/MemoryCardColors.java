package appeng.api.implementations.items;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import appeng.api.util.AEColor;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Describes the custom colors to show o n a memory card item.
 *
 * @param top1
 * @param top2
 * @param top3
 * @param top4
 * @param bottom1
 * @param bottom2
 * @param bottom3
 * @param bottom4
 */
public final class MemoryCardColors {
    private final AEColor top1;
    private final AEColor top2;
    private final AEColor top3;
    private final AEColor top4;
    private final AEColor bottom1;
    private final AEColor bottom2;
    private final AEColor bottom3;
    private final AEColor bottom4;

    public MemoryCardColors(AEColor top1, AEColor top2, AEColor top3, AEColor top4, AEColor bottom1, AEColor bottom2, AEColor bottom3, AEColor bottom4) {
        this.top1 = top1;
        this.top2 = top2;
        this.top3 = top3;
        this.top4 = top4;
        this.bottom1 = bottom1;
        this.bottom2 = bottom2;
        this.bottom3 = bottom3;
        this.bottom4 = bottom4;
    }

    public AEColor top1() {
        return top1;
    }

    public AEColor top2() {
        return top2;
    }

    public AEColor top3() {
        return top3;
    }

    public AEColor top4() {
        return top4;
    }

    public AEColor bottom1() {
        return bottom1;
    }

    public AEColor bottom2() {
        return bottom2;
    }

    public AEColor bottom3() {
        return bottom3;
    }

    public AEColor bottom4() {
        return bottom4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryCardColors)) return false;
        MemoryCardColors that = (MemoryCardColors) o;
        return java.util.Objects.equals(top1, that.top1)
                && java.util.Objects.equals(top2, that.top2)
                && java.util.Objects.equals(top3, that.top3)
                && java.util.Objects.equals(top4, that.top4)
                && java.util.Objects.equals(bottom1, that.bottom1)
                && java.util.Objects.equals(bottom2, that.bottom2)
                && java.util.Objects.equals(bottom3, that.bottom3)
                && java.util.Objects.equals(bottom4, that.bottom4);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(top1, top2, top3, top4, bottom1, bottom2, bottom3, bottom4);
    }

    @Override
    public String toString() {
        return "MemoryCardColors[" + "top1=" + top1 + ", " + "top2=" + top2 + ", " + "top3=" + top3 + ", " + "top4=" + top4 + ", " + "bottom1=" + bottom1 + ", " + "bottom2=" + bottom2 + ", " + "bottom3=" + bottom3 + ", " + "bottom4=" + bottom4 + "]";
    }

    public static final Codec<MemoryCardColors> CODEC = AEColor.CODEC.listOf().xmap(
            aeColors -> new MemoryCardColors(
                    0 < aeColors.size() ? aeColors.get(0) : AEColor.TRANSPARENT,
                    1 < aeColors.size() ? aeColors.get(1) : AEColor.TRANSPARENT,
                    2 < aeColors.size() ? aeColors.get(2) : AEColor.TRANSPARENT,
                    3 < aeColors.size() ? aeColors.get(3) : AEColor.TRANSPARENT,
                    4 < aeColors.size() ? aeColors.get(4) : AEColor.TRANSPARENT,
                    5 < aeColors.size() ? aeColors.get(5) : AEColor.TRANSPARENT,
                    6 < aeColors.size() ? aeColors.get(6) : AEColor.TRANSPARENT,
                    7 < aeColors.size() ? aeColors.get(7) : AEColor.TRANSPARENT),
            colors -> ImmutableList.of(
                    colors.top1(),
                    colors.top2(),
                    colors.top3(),
                    colors.top4(),
                    colors.bottom1(),
                    colors.bottom2(),
                    colors.bottom3(),
                    colors.bottom4()));

    public static StreamCodec<RegistryFriendlyByteBuf, MemoryCardColors> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, MemoryCardColors>() {
        @Override
        public MemoryCardColors decode(RegistryFriendlyByteBuf buffer) {
            return new MemoryCardColors(
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class),
                    buffer.readEnum(AEColor.class));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MemoryCardColors colors) {
            buffer.writeEnum(colors.top1);
            buffer.writeEnum(colors.top2);
            buffer.writeEnum(colors.top3);
            buffer.writeEnum(colors.top4);
            buffer.writeEnum(colors.bottom1);
            buffer.writeEnum(colors.bottom2);
            buffer.writeEnum(colors.bottom3);
            buffer.writeEnum(colors.bottom4);
        }
    };

    public static final MemoryCardColors DEFAULT = new MemoryCardColors(AEColor.TRANSPARENT, AEColor.TRANSPARENT,
            AEColor.TRANSPARENT, AEColor.TRANSPARENT,
            AEColor.TRANSPARENT, AEColor.TRANSPARENT, AEColor.TRANSPARENT, AEColor.TRANSPARENT);

    public AEColor get(int x, int y) {
        var index = x + y * 4;
        return switch (index) {
            case 0 -> top1;
            case 1 -> top2;
            case 2 -> top3;
            case 3 -> top4;
            case 4 -> bottom1;
            case 5 -> bottom2;
            case 6 -> bottom3;
            case 7 -> bottom4;
            default -> AEColor.TRANSPARENT;
        };
    }
}
