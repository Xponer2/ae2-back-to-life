package appeng.menu.guisync;

import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Implement on classes to signal they can be synchronized to the client using {@link GuiSync}. For this to work fully,
 * the class also needs to have a public constructor that takes a {@link RegistryFriendlyByteBuf} argument.
 */
public interface PacketWritable {
    void writeToPacket(RegistryFriendlyByteBuf data);
}
