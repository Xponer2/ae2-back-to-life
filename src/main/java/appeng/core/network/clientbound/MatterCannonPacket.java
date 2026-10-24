
package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.ParticleTypes;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public final class MatterCannonPacket implements ClientboundPacket {
    private final double x;
    private final double y;
    private final double z;
    private final double dx;
    private final double dy;
    private final double dz;
    private final byte len;

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double dx() {
        return dx;
    }

    public double dy() {
        return dy;
    }

    public double dz() {
        return dz;
    }

    public byte len() {
        return len;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatterCannonPacket)) return false;
        MatterCannonPacket that = (MatterCannonPacket) o;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0
                && Double.compare(dx, that.dx) == 0
                && Double.compare(dy, that.dy) == 0
                && Double.compare(dz, that.dz) == 0
                && len == that.len;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z, dx, dy, dz, len);
    }

    @Override
    public String toString() {
        return "MatterCannonPacket[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + ", " + "dx=" + dx + ", " + "dy=" + dy + ", " + "dz=" + dz + ", " + "len=" + len + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterCannonPacket> STREAM_CODEC = StreamCodec.ofMember(
            MatterCannonPacket::write,
            MatterCannonPacket::decode);

    public static final Type<MatterCannonPacket> TYPE = CustomAppEngPayload.createType("matter_cannon");

    @Override
    public Type<MatterCannonPacket> type() {
        return TYPE;
    }

    public MatterCannonPacket(double x, double y, double z, double dx, double dy, double dz, byte len) {
        var dl = dx * dx + dy * dy + dz * dz;
        var dlz = (float) Math.sqrt(dl);

        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx / dlz;
        this.dy = dy / dlz;
        this.dz = dz / dlz;
        this.len = len;
    }

    public static MatterCannonPacket decode(RegistryFriendlyByteBuf stream) {
        var x = stream.readFloat();
        var y = stream.readFloat();
        var z = stream.readFloat();
        var dx = stream.readFloat();
        var dy = stream.readFloat();
        var dz = stream.readFloat();
        var len = stream.readByte();
        return new MatterCannonPacket(x, y, z, dx, dy, dz, len);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeFloat((float) x);
        data.writeFloat((float) y);
        data.writeFloat((float) z);
        data.writeFloat((float) this.dx);
        data.writeFloat((float) this.dy);
        data.writeFloat((float) this.dz);
        data.writeByte(len);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        try {
            for (int a = 1; a < this.len; a++) {
                AEParticles.create(ParticleTypes.MATTER_CANNON, this.x + this.dx * a,
                        this.y + this.dy * a, this.z + this.dz * a, 0, 0, 0);
            }
        } catch (Exception ignored) {
        }
    }
}
