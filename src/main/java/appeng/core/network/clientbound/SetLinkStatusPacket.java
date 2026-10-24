package appeng.core.network.clientbound;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.storage.ILinkStatus;
import appeng.api.storage.LinkStatus;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.guisync.LinkStatusAwareMenu;

public final class SetLinkStatusPacket implements ClientboundPacket {
    private final ILinkStatus linkStatus;

    public SetLinkStatusPacket(ILinkStatus linkStatus) {
        this.linkStatus = linkStatus;
    }

    public ILinkStatus linkStatus() {
        return linkStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetLinkStatusPacket)) return false;
        SetLinkStatusPacket that = (SetLinkStatusPacket) o;
        return java.util.Objects.equals(linkStatus, that.linkStatus);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(linkStatus);
    }

    @Override
    public String toString() {
        return "SetLinkStatusPacket[" + "linkStatus=" + linkStatus + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SetLinkStatusPacket> STREAM_CODEC = StreamCodec.ofMember(
            SetLinkStatusPacket::write,
            SetLinkStatusPacket::decode);

    public static final Type<SetLinkStatusPacket> TYPE = CustomAppEngPayload.createType("set_link_status");

    @Override
    public Type<SetLinkStatusPacket> type() {
        return TYPE;
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(linkStatus.connected());
        buffer.writeNullable(linkStatus.statusDescription(), RegistryFriendlyByteBuf::writeComponent);
    }

    public static SetLinkStatusPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SetLinkStatusPacket(new LinkStatus(
                buffer.readBoolean(),
                buffer.readNullable(RegistryFriendlyByteBuf::readComponent)));
    }

    @Override
    public void handleOnClient(EntityPlayer player) {
        if (player.openContainer instanceof LinkStatusAwareMenu linkStatusAwareMenu) {
            linkStatusAwareMenu.setLinkStatus(linkStatus);
        }
    }
}
