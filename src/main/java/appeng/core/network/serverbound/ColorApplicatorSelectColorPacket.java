
package appeng.core.network.serverbound;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import appeng.api.util.AEColor;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.items.tools.powered.ColorApplicatorItem;

/**
 * Switches the color of any held color applicator to the desired color.
 */
public final class ColorApplicatorSelectColorPacket implements ServerboundPacket {
    private final AEColor color;

    public ColorApplicatorSelectColorPacket(@Nullable AEColor color) {
        this.color = color;
    }

    @Nullable public AEColor color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorApplicatorSelectColorPacket)) return false;
        ColorApplicatorSelectColorPacket that = (ColorApplicatorSelectColorPacket) o;
        return java.util.Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(color);
    }

    @Override
    public String toString() {
        return "ColorApplicatorSelectColorPacket[" + "color=" + color + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ColorApplicatorSelectColorPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    ColorApplicatorSelectColorPacket::write,
                    ColorApplicatorSelectColorPacket::decode);

    public static final Type<ColorApplicatorSelectColorPacket> TYPE = CustomAppEngPayload
            .createType("color_applicator_select_color");

    @Override
    public Type<ColorApplicatorSelectColorPacket> type() {
        return TYPE;
    }

    public static ColorApplicatorSelectColorPacket decode(RegistryFriendlyByteBuf stream) {
        AEColor color = null;
        if (stream.readBoolean()) {
            color = stream.readEnum(AEColor.class);
        }
        return new ColorApplicatorSelectColorPacket(color);
    }

    public void write(RegistryFriendlyByteBuf data) {
        if (color != null) {
            data.writeBoolean(true);
            data.writeEnum(color);
        } else {
            data.writeBoolean(false);
        }
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        switchColor(player.getHeldItemMainhand(), color);
        switchColor(player.getHeldItemOffhand(), color);
    }

    private static void switchColor(ItemStack stack, AEColor color) {
        if (!stack.isEmpty() && stack.getItem() instanceof ColorApplicatorItem colorApplicator) {
            colorApplicator.setActiveColor(stack, color);
        }
    }
}
