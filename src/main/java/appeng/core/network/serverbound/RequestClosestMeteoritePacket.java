
package appeng.core.network.serverbound;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;

import appeng.core.network.PacketDistributor;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.clientbound.CompassResponsePacket;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.server.services.compass.ServerCompassService;

public final class RequestClosestMeteoritePacket implements ServerboundPacket {
    private final ChunkPos pos;

    public RequestClosestMeteoritePacket(ChunkPos pos) {
        this.pos = pos;
    }

    public ChunkPos pos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestClosestMeteoritePacket)) return false;
        RequestClosestMeteoritePacket that = (RequestClosestMeteoritePacket) o;
        return java.util.Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pos);
    }

    @Override
    public String toString() {
        return "RequestClosestMeteoritePacket[" + "pos=" + pos + "]";
    }

    private static final Logger LOG = LogManager.getLogger(RequestClosestMeteoritePacket.class);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestClosestMeteoritePacket> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.CHUNK_POS, RequestClosestMeteoritePacket::pos,
                    RequestClosestMeteoritePacket::new);

    public static final Type<RequestClosestMeteoritePacket> TYPE = CustomAppEngPayload.createType("compass_request");

    @Override
    public Type<RequestClosestMeteoritePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        var result = ServerCompassService.getClosestMeteorite(player.getServerWorld(), pos);
        LOG.trace("{} requested closest meteorite for {} in {} -> {}", player, pos, player.getServerWorld(), result);
        PacketDistributor.sendToPlayer(player, new CompassResponsePacket(pos, result));
    }
}
