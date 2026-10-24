package appeng.items.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.math.BlockPos;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.spatial.SpatialStoragePlot;
import appeng.util.AECodecs;

/**
 * @param id   The {@linkplain SpatialStoragePlot#getId() plot id}.
 * @param size This is only stored in the itemstack to display in the tooltip on the client-side.
 */
public final class SpatialPlotInfo {
    private final int id;
    private final BlockPos size;

    public SpatialPlotInfo(int id, BlockPos size) {
        this.id = id;
        this.size = size;
    }

    public int id() {
        return id;
    }

    public BlockPos size() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpatialPlotInfo)) return false;
        SpatialPlotInfo that = (SpatialPlotInfo) o;
        return id == that.id
                && java.util.Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, size);
    }

    @Override
    public String toString() {
        return "SpatialPlotInfo[" + "id=" + id + ", " + "size=" + size + "]";
    }

    public static final Codec<SpatialPlotInfo> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("id").forGetter(SpatialPlotInfo::id),
            AECodecs.BLOCK_POS.fieldOf("size").forGetter(SpatialPlotInfo::size)).apply(builder, SpatialPlotInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpatialPlotInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SpatialPlotInfo::id,
            ByteBufCodecs.BLOCK_POS,
            SpatialPlotInfo::size,
            SpatialPlotInfo::new);
}
