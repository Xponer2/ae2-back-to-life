
package appeng.core.network.clientbound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public final class MockExplosionPacket implements ClientboundPacket {
    private final double x;
    private final double y;
    private final double z;

    public MockExplosionPacket(double x, double y, double z) {
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
        if (!(o instanceof MockExplosionPacket)) return false;
        MockExplosionPacket that = (MockExplosionPacket) o;
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
        return "MockExplosionPacket[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MockExplosionPacket> STREAM_CODEC = StreamCodec.ofMember(
            MockExplosionPacket::write,
            MockExplosionPacket::decode);

    public static final Type<MockExplosionPacket> TYPE = CustomAppEngPayload.createType("mock_explosion");

    @Override
    public Type<MockExplosionPacket> type() {
        return TYPE;
    }

    public static MockExplosionPacket decode(RegistryFriendlyByteBuf data) {
        var x = data.readDouble();
        var y = data.readDouble();
        var z = data.readDouble();
        return new MockExplosionPacket(x, y, z);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        final World level = player.world;
        level.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
    }
}
