package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.api.stacks.AEKeyType;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.interfaces.KeyTypeSelectionMenu;

public final class SelectKeyTypePacket implements ServerboundPacket {
    private final AEKeyType keyType;
    private final boolean enabled;

    public SelectKeyTypePacket(AEKeyType keyType, boolean enabled) {
        this.keyType = keyType;
        this.enabled = enabled;
    }

    public AEKeyType keyType() {
        return keyType;
    }

    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectKeyTypePacket)) return false;
        SelectKeyTypePacket that = (SelectKeyTypePacket) o;
        return java.util.Objects.equals(keyType, that.keyType)
                && enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(keyType, enabled);
    }

    @Override
    public String toString() {
        return "SelectKeyTypePacket[" + "keyType=" + keyType + ", " + "enabled=" + enabled + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectKeyTypePacket> STREAM_CODEC = StreamCodec.ofMember(
            SelectKeyTypePacket::write,
            SelectKeyTypePacket::decode);

    public static final Type<SelectKeyTypePacket> TYPE = CustomAppEngPayload.createType("select_key_type");

    @Override
    public Type<SelectKeyTypePacket> type() {
        return TYPE;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(keyType.getRawId());
        buf.writeBoolean(enabled);
    }

    public static SelectKeyTypePacket decode(RegistryFriendlyByteBuf buf) {
        return new SelectKeyTypePacket(AEKeyType.fromRawId(buf.readVarInt()), buf.readBoolean());
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof KeyTypeSelectionMenu menu) {
            menu.getServerKeyTypeSelection().setEnabled(keyType, enabled);
        }
    }
}
