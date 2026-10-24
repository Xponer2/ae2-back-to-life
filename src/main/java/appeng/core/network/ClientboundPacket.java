package appeng.core.network;

import net.minecraft.entity.player.EntityPlayer;

/**
 * A packet sent from the server to the client. {@link #handleOnClient} runs on the client thread.
 */
public interface ClientboundPacket extends CustomAppEngPayload {
    default void handleOnClient(EntityPlayer player) {
        throw new AbstractMethodError("Unimplemented method on " + getClass());
    }
}
