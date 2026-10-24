package appeng.core.network.serverbound;

import org.jetbrains.annotations.NotNull;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.definitions.AEAttachmentTypes;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;

public final class UpdateHoldingCtrlPacket implements ServerboundPacket {
    private final boolean keyDown;

    public UpdateHoldingCtrlPacket(boolean keyDown) {
        this.keyDown = keyDown;
    }

    public boolean keyDown() {
        return keyDown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateHoldingCtrlPacket)) return false;
        UpdateHoldingCtrlPacket that = (UpdateHoldingCtrlPacket) o;
        return keyDown == that.keyDown;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(keyDown);
    }

    @Override
    public String toString() {
        return "UpdateHoldingCtrlPacket[" + "keyDown=" + keyDown + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateHoldingCtrlPacket> STREAM_CODEC = StreamCodec
            .ofMember(UpdateHoldingCtrlPacket::write, UpdateHoldingCtrlPacket::decode);

    public static final Type<UpdateHoldingCtrlPacket> TYPE = CustomAppEngPayload.createType("toggle_ctrl_down");

    @NotNull
    @Override
    public Type<UpdateHoldingCtrlPacket> type() {
        return TYPE;
    }

    public static UpdateHoldingCtrlPacket decode(RegistryFriendlyByteBuf buf) {
        var keyDown = buf.readBoolean();
        return new UpdateHoldingCtrlPacket(keyDown);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBoolean(keyDown);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        AEAttachmentTypes.HOLDING_CTRL.set(player, keyDown);
    }
}
