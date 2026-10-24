
package appeng.core.network.clientbound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.AEConfig;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public final class LightningPacket implements ClientboundPacket {
    private final double x;
    private final double y;
    private final double z;

    public LightningPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LightningPacket)) return false;
        LightningPacket that = (LightningPacket) o;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "LightningPacket[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, LightningPacket> STREAM_CODEC = StreamCodec.ofMember(
            LightningPacket::write,
            LightningPacket::decode);

    public static final Type<LightningPacket> TYPE = CustomAppEngPayload.createType("lightning");

    @Override
    public Type<LightningPacket> type() {
        return TYPE;
    }

    public static LightningPacket decode(RegistryFriendlyByteBuf stream) {
        var x = stream.readFloat();
        var y = stream.readFloat();
        var z = stream.readFloat();
        return new LightningPacket(x, y, z);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeFloat((float) x);
        data.writeFloat((float) y);
        data.writeFloat((float) z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        try {
            if (AEConfig.instance().isEnableEffects()) {
                AEParticles.add(ParticleTypes.LIGHTNING, this.x, this.y, this.z, 0.0f, 0.0f,
                        0.0f);
            }
        } catch (Exception ignored) {
        }
    }
}
