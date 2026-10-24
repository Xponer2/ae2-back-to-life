
package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.me.crafting.CraftAmountMenu;

public final class ConfirmAutoCraftPacket implements ServerboundPacket {
    private final int amount;
    private final boolean craftMissingAmount;
    private final boolean autoStart;

    public ConfirmAutoCraftPacket(int amount, boolean craftMissingAmount, boolean autoStart) {
        this.amount = amount;
        this.craftMissingAmount = craftMissingAmount;
        this.autoStart = autoStart;
    }

    public int amount() {
        return amount;
    }

    public boolean craftMissingAmount() {
        return craftMissingAmount;
    }

    public boolean autoStart() {
        return autoStart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfirmAutoCraftPacket)) return false;
        ConfirmAutoCraftPacket that = (ConfirmAutoCraftPacket) o;
        return amount == that.amount
                && craftMissingAmount == that.craftMissingAmount
                && autoStart == that.autoStart;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(amount, craftMissingAmount, autoStart);
    }

    @Override
    public String toString() {
        return "ConfirmAutoCraftPacket[" + "amount=" + amount + ", " + "craftMissingAmount=" + craftMissingAmount + ", " + "autoStart=" + autoStart + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmAutoCraftPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    ConfirmAutoCraftPacket::write,
                    ConfirmAutoCraftPacket::decode);

    public static final Type<ConfirmAutoCraftPacket> TYPE = CustomAppEngPayload.createType("confirm_auto_craft");

    @Override
    public Type<ConfirmAutoCraftPacket> type() {
        return TYPE;
    }

    public static ConfirmAutoCraftPacket decode(RegistryFriendlyByteBuf stream) {
        var amount = stream.readInt();
        var craftMissingAmount = stream.readBoolean();
        var autoStart = stream.readBoolean();
        return new ConfirmAutoCraftPacket(amount, craftMissingAmount, autoStart);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeInt(amount);
        data.writeBoolean(craftMissingAmount);
        data.writeBoolean(autoStart);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof CraftAmountMenu menu) {
            menu.confirm(amount, craftMissingAmount, autoStart);
        }
    }
}
