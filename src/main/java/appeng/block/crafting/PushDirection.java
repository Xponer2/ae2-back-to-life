package appeng.block.crafting;

import appeng.core.network.codec.ByteBufCodecs;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.EnumFacing;

import appeng.api.util.StringRepresentable;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Extends {@link EnumFacing} with an 'all' key.
 */
public enum PushDirection implements StringRepresentable {
    DOWN(EnumFacing.DOWN),
    UP(EnumFacing.UP),
    NORTH(EnumFacing.NORTH),
    SOUTH(EnumFacing.SOUTH),
    WEST(EnumFacing.WEST),
    EAST(EnumFacing.EAST),
    ALL;

    public static final Codec<PushDirection> CODEC = StringRepresentable.fromEnum(PushDirection::values);

    public static final StreamCodec<RegistryFriendlyByteBuf, PushDirection> STREAM_CODEC = ByteBufCodecs
            .enumCodec(PushDirection.class);

    @Nullable
    private final EnumFacing direction;

    PushDirection(EnumFacing direction) {
        this.direction = direction;
    }

    PushDirection() {
        this.direction = null;
    }

    @Nullable
    public EnumFacing getDirection() {
        return direction;
    }

    @Override
    public String getSerializedName() {
        return direction != null ? direction.getSerializedName() : "all";
    }

    public static PushDirection fromDirection(@Nullable EnumFacing direction) {
        return direction != null ? values()[direction.ordinal()] : ALL;
    }
}
