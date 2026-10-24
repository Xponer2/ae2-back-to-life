
package appeng.core.network.clientbound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingPlanSummary;

/**
 * Transfers a {@link CraftingPlanSummary} to the client for a {@link CraftConfirmMenu}
 */
public final class CraftConfirmPlanPacket implements ClientboundPacket {
    private final CraftingPlanSummary plan;

    public CraftConfirmPlanPacket(CraftingPlanSummary plan) {
        this.plan = plan;
    }

    public CraftingPlanSummary plan() {
        return plan;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftConfirmPlanPacket)) return false;
        CraftConfirmPlanPacket that = (CraftConfirmPlanPacket) o;
        return java.util.Objects.equals(plan, that.plan);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(plan);
    }

    @Override
    public String toString() {
        return "CraftConfirmPlanPacket[" + "plan=" + plan + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftConfirmPlanPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    CraftConfirmPlanPacket::write,
                    CraftConfirmPlanPacket::decode);

    public static final Type<CraftConfirmPlanPacket> TYPE = CustomAppEngPayload.createType("craft_confirm_plan");

    @Override
    public Type<CraftConfirmPlanPacket> type() {
        return TYPE;
    }

    public static CraftConfirmPlanPacket decode(RegistryFriendlyByteBuf data) {
        return new CraftConfirmPlanPacket(CraftingPlanSummary.read(data));
    }

    public void write(RegistryFriendlyByteBuf data) {
        plan.write(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (player.openContainer instanceof CraftConfirmMenu menu) {
            menu.setPlan(plan);
        }
    }
}
