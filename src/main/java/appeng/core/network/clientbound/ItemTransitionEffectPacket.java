
package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.EnergyParticleData;
import appeng.core.AppEngClient;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Plays a transition particle effect into the supplied direction. Used primarily by annihilation planes.
 */
public final class ItemTransitionEffectPacket implements ClientboundPacket {
    private final double x;
    private final double y;
    private final double z;
    private final EnumFacing d;

    public ItemTransitionEffectPacket(double x, double y, double z, EnumFacing d) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.d = d;
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

    public EnumFacing d() {
        return d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemTransitionEffectPacket)) return false;
        ItemTransitionEffectPacket that = (ItemTransitionEffectPacket) o;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0
                && java.util.Objects.equals(d, that.d);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z, d);
    }

    @Override
    public String toString() {
        return "ItemTransitionEffectPacket[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + ", " + "d=" + d + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemTransitionEffectPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    ItemTransitionEffectPacket::write,
                    ItemTransitionEffectPacket::decode);

    public static final Type<ItemTransitionEffectPacket> TYPE = CustomAppEngPayload
            .createType("item_transition_effect");

    @Override
    public Type<ItemTransitionEffectPacket> type() {
        return TYPE;
    }

    public static ItemTransitionEffectPacket decode(RegistryFriendlyByteBuf stream) {
        var x = stream.readFloat();
        var y = stream.readFloat();
        var z = stream.readFloat();
        var d = stream.readEnum(EnumFacing.class);
        return new ItemTransitionEffectPacket(x, y, z, d);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeFloat((float) x);
        data.writeFloat((float) y);
        data.writeFloat((float) z);
        data.writeEnum(d);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        EnergyParticleData data = new EnergyParticleData(true, this.d);
        for (int zz = 0; zz < 8; zz++) {
            if (AppEngClient.instance().shouldAddParticles(player.world.rand)) {
                // Distribute the spawn point around the item's position
                double x = this.x + player.world.rand.nextFloat() * 0.5 - 0.25;
                double y = this.y + player.world.rand.nextFloat() * 0.5 - 0.25;
                double z = this.z + player.world.rand.nextFloat() * 0.5 - 0.25;
                double speedX = 0.1f * this.d.getXOffset();
                double speedY = 0.1f * this.d.getYOffset();
                double speedZ = 0.1f * this.d.getZOffset();
                AEParticles.create(data, x, y, z, speedX, speedY, speedZ);
            }
        }
    }

}
