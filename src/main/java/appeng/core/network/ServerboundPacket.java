package appeng.core.network;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * A packet sent from the client to the server. {@link #handleOnServer} runs on the server thread.
 */
public interface ServerboundPacket extends CustomAppEngPayload {
    void handleOnServer(EntityPlayerMP player);
}
