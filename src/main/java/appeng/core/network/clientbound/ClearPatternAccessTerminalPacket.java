
package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Clears all data from the pattern access terminal before a full reset.
 */
public final class ClearPatternAccessTerminalPacket implements ClientboundPacket {
    public ClearPatternAccessTerminalPacket() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClearPatternAccessTerminalPacket;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash();
    }

    @Override
    public String toString() {
        return "ClearPatternAccessTerminalPacket[]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ClearPatternAccessTerminalPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    ClearPatternAccessTerminalPacket::write,
                    ClearPatternAccessTerminalPacket::decode);

    public static final Type<ClearPatternAccessTerminalPacket> TYPE = CustomAppEngPayload
            .createType("clear_pattern_access_terminal");

    @Override
    public Type<ClearPatternAccessTerminalPacket> type() {
        return TYPE;
    }

    public static ClearPatternAccessTerminalPacket decode(RegistryFriendlyByteBuf data) {
        return new ClearPatternAccessTerminalPacket();
    }

    public void write(RegistryFriendlyByteBuf data) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (Minecraft.getMinecraft().currentScreen instanceof PatternAccessTermScreen<?> patternAccessTerminal) {
            patternAccessTerminal.clear();
        }
    }
}
