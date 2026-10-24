
package appeng.core.network.serverbound;


import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.ForgeHooks;

import appeng.api.parts.IPartHost;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;

/**
 * Packet sent when a player left-clicks on a part attached to a cable bus. This packet contains the hit position to
 * restore the part that was hit on the client.
 */
public final class PartLeftClickPacket implements ServerboundPacket {
    private final RayTraceResult hitResult;
    private final boolean alternateUseMode;

    public PartLeftClickPacket(RayTraceResult hitResult, boolean alternateUseMode) {
        this.hitResult = hitResult;
        this.alternateUseMode = alternateUseMode;
    }

    public RayTraceResult hitResult() {
        return hitResult;
    }

    public boolean alternateUseMode() {
        return alternateUseMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartLeftClickPacket)) return false;
        PartLeftClickPacket that = (PartLeftClickPacket) o;
        return java.util.Objects.equals(hitResult, that.hitResult)
                && alternateUseMode == that.alternateUseMode;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hitResult, alternateUseMode);
    }

    @Override
    public String toString() {
        return "PartLeftClickPacket[" + "hitResult=" + hitResult + ", " + "alternateUseMode=" + alternateUseMode + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PartLeftClickPacket> STREAM_CODEC = StreamCodec.ofMember(
            PartLeftClickPacket::write,
            PartLeftClickPacket::decode);

    public static final Type<PartLeftClickPacket> TYPE = CustomAppEngPayload.createType("part_left_click");

    @Override
    public Type<PartLeftClickPacket> type() {
        return TYPE;
    }

    public static PartLeftClickPacket decode(RegistryFriendlyByteBuf stream) {
        var hitResult = stream.readBlockHitResult();
        var alternateUseMode = stream.readBoolean();
        return new PartLeftClickPacket(hitResult, alternateUseMode);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBlockHitResult(hitResult);
        data.writeBoolean(alternateUseMode);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        // Fire event on the server to give protection mods a chance to cancel the interaction
        var pos = hitResult.getBlockPos();
        var evt = ForgeHooks.onLeftClickBlock(player, pos, hitResult.sideHit, hitResult.hitVec);
        if (evt.isCanceled()) {
            return;
        }

        var localPos = hitResult.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());

        if (player.world.getTileEntity(pos) instanceof IPartHost partHost) {
            var selectedPart = partHost.selectPartLocal(localPos);
            if (selectedPart.part != null) {
                if (!alternateUseMode) {
                    selectedPart.part.onClicked(player, localPos);
                } else {
                    selectedPart.part.onShiftClicked(player, localPos);
                }
            } else if (selectedPart.facade != null) {
                selectedPart.facade.onClicked(player, localPos);
            }
        }
    }
}
