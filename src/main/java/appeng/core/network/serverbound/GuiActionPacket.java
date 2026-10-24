
package appeng.core.network.serverbound;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.AEBaseMenu;

/**
 * This packet is used for triggering generic menu-specific GUI actions.
 */
public final class GuiActionPacket implements ServerboundPacket {
    private final int containerId;
    private final String actionName;
    private final String jsonPayload;

    public GuiActionPacket(int containerId, String actionName, @Nullable String jsonPayload) {
        this.containerId = containerId;
        this.actionName = actionName;
        this.jsonPayload = jsonPayload;
    }

    public int containerId() {
        return containerId;
    }

    public String actionName() {
        return actionName;
    }

    @Nullable public String jsonPayload() {
        return jsonPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuiActionPacket)) return false;
        GuiActionPacket that = (GuiActionPacket) o;
        return containerId == that.containerId
                && java.util.Objects.equals(actionName, that.actionName)
                && java.util.Objects.equals(jsonPayload, that.jsonPayload);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, actionName, jsonPayload);
    }

    @Override
    public String toString() {
        return "GuiActionPacket[" + "containerId=" + containerId + ", " + "actionName=" + actionName + ", " + "jsonPayload=" + jsonPayload + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, GuiActionPacket> STREAM_CODEC = StreamCodec.ofMember(
            GuiActionPacket::write,
            GuiActionPacket::decode);

    public static final Type<GuiActionPacket> TYPE = CustomAppEngPayload.createType("gui_action");

    @Override
    public Type<GuiActionPacket> type() {
        return TYPE;
    }

    public static GuiActionPacket decode(RegistryFriendlyByteBuf data) {
        var containerId = data.readVarInt();
        var actionName = data.readUtf();
        var jsonPayload = data.readOptional(RegistryFriendlyByteBuf::readUtf).orElse(null);
        return new GuiActionPacket(containerId, actionName, jsonPayload);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeVarInt(containerId);
        data.writeUtf(actionName);
        data.writeOptional(Optional.ofNullable(jsonPayload), RegistryFriendlyByteBuf::writeUtf);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        Container c = player.openContainer;
        if (c instanceof AEBaseMenu baseMenu && c.windowId == this.containerId) {
            baseMenu.receiveClientAction(actionName, jsonPayload);
        }
    }

}
