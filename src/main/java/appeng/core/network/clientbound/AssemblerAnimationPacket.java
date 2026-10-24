package appeng.core.network.clientbound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.stacks.AEKey;
import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public final class AssemblerAnimationPacket implements ClientboundPacket {
    private final BlockPos pos;
    private final byte rate;
    private final AEKey what;

    public AssemblerAnimationPacket(BlockPos pos, byte rate, AEKey what) {
        this.pos = pos;
        this.rate = rate;
        this.what = what;
    }

    public BlockPos pos() {
        return pos;
    }

    public byte rate() {
        return rate;
    }

    public AEKey what() {
        return what;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssemblerAnimationPacket)) return false;
        AssemblerAnimationPacket that = (AssemblerAnimationPacket) o;
        return java.util.Objects.equals(pos, that.pos)
                && rate == that.rate
                && java.util.Objects.equals(what, that.what);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pos, rate, what);
    }

    @Override
    public String toString() {
        return "AssemblerAnimationPacket[" + "pos=" + pos + ", " + "rate=" + rate + ", " + "what=" + what + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblerAnimationPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    AssemblerAnimationPacket::write,
                    AssemblerAnimationPacket::decode);

    public static final Type<AssemblerAnimationPacket> TYPE = CustomAppEngPayload.createType("assembler_animation");

    @Override
    public Type<AssemblerAnimationPacket> type() {
        return TYPE;
    }

    public static AssemblerAnimationPacket decode(RegistryFriendlyByteBuf data) {
        var pos = data.readBlockPos();
        var rate = data.readByte();
        var what = AEKey.readKey(data);
        return new AssemblerAnimationPacket(pos, rate, what);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBlockPos(pos);
        data.writeByte(rate);
        AEKey.writeKey(data, what);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        TileEntity te = player.world.getTileEntity(pos);
        if (te instanceof MolecularAssemblerBlockEntity ma) {
            ma.setAnimationStatus(new AssemblerAnimationStatus(rate, what.wrapForDisplayOrFilter()));
        }
    }
}
