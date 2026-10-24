
package appeng.core.network.clientbound;

import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.hooks.CompassManager;

public final class CompassResponsePacket implements ClientboundPacket {
    private final ChunkPos requestedPos;
    private final Optional<BlockPos> closestMeteorite;

    public CompassResponsePacket(ChunkPos requestedPos, Optional<BlockPos> closestMeteorite) {
        this.requestedPos = requestedPos;
        this.closestMeteorite = closestMeteorite;
    }

    public ChunkPos requestedPos() {
        return requestedPos;
    }

    public Optional<BlockPos> closestMeteorite() {
        return closestMeteorite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompassResponsePacket)) return false;
        CompassResponsePacket that = (CompassResponsePacket) o;
        return java.util.Objects.equals(requestedPos, that.requestedPos)
                && java.util.Objects.equals(closestMeteorite, that.closestMeteorite);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(requestedPos, closestMeteorite);
    }

    @Override
    public String toString() {
        return "CompassResponsePacket[" + "requestedPos=" + requestedPos + ", " + "closestMeteorite=" + closestMeteorite + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CompassResponsePacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.CHUNK_POS, CompassResponsePacket::requestedPos,
                    ByteBufCodecs.optional(ByteBufCodecs.BLOCK_POS), CompassResponsePacket::closestMeteorite,
                    CompassResponsePacket::new);

    public static final Type<CompassResponsePacket> TYPE = CustomAppEngPayload.createType("compass_response");

    @Override
    public Type<CompassResponsePacket> type() {
        return TYPE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        CompassManager.INSTANCE.postResult(requestedPos, closestMeteorite.orElse(null));
    }
}
