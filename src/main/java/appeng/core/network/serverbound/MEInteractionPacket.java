
package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.IMEInteractionHandler;

/**
 * Packet sent by clients to interact with an ME inventory such as an item terminal.
 */
public final class MEInteractionPacket implements ServerboundPacket {
    private final int containerId;
    private final long serial;
    private final InventoryAction action;

    public MEInteractionPacket(int containerId, long serial, InventoryAction action) {
        this.containerId = containerId;
        this.serial = serial;
        this.action = action;
    }

    public int containerId() {
        return containerId;
    }

    public long serial() {
        return serial;
    }

    public InventoryAction action() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MEInteractionPacket)) return false;
        MEInteractionPacket that = (MEInteractionPacket) o;
        return containerId == that.containerId
                && serial == that.serial
                && java.util.Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, serial, action);
    }

    @Override
    public String toString() {
        return "MEInteractionPacket[" + "containerId=" + containerId + ", " + "serial=" + serial + ", " + "action=" + action + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MEInteractionPacket> STREAM_CODEC = StreamCodec.ofMember(
            MEInteractionPacket::write,
            MEInteractionPacket::decode);

    public static final Type<MEInteractionPacket> TYPE = CustomAppEngPayload.createType("me_interaction");

    @Override
    public Type<MEInteractionPacket> type() {
        return TYPE;
    }

    public static MEInteractionPacket decode(RegistryFriendlyByteBuf buffer) {
        var containerId = buffer.readInt();
        var serial = buffer.readVarLong();
        var action = buffer.readEnum(InventoryAction.class);
        return new MEInteractionPacket(containerId, serial, action);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeInt(containerId);
        data.writeVarLong(serial);
        data.writeEnum(action);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof IMEInteractionHandler handler) {
            // The open screen has changed since the client sent the packet
            if (player.openContainer.windowId != containerId) {
                return;
            }

            handler.handleInteraction(serial, action);
        }
    }
}
