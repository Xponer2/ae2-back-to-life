package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.util.Platform;

public final class InventoryActionPacket implements ServerboundPacket {
    private final InventoryAction action;
    private final int slot;
    private final long extraId;
    private final ItemStack slotItem;

    public InventoryActionPacket(InventoryAction action, int slot, long extraId, ItemStack slotItem) {
        this.action = action;
        this.slot = slot;
        this.extraId = extraId;
        this.slotItem = slotItem;
    }

    public InventoryAction action() {
        return action;
    }

    public int slot() {
        return slot;
    }

    public long extraId() {
        return extraId;
    }

    public ItemStack slotItem() {
        return slotItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryActionPacket)) return false;
        InventoryActionPacket that = (InventoryActionPacket) o;
        return java.util.Objects.equals(action, that.action)
                && slot == that.slot
                && extraId == that.extraId
                && java.util.Objects.equals(slotItem, that.slotItem);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(action, slot, extraId, slotItem);
    }

    @Override
    public String toString() {
        return "InventoryActionPacket[" + "action=" + action + ", " + "slot=" + slot + ", " + "extraId=" + extraId + ", " + "slotItem=" + slotItem + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryActionPacket> STREAM_CODEC = StreamCodec.ofMember(
            InventoryActionPacket::write,
            InventoryActionPacket::decode);

    public static final Type<InventoryActionPacket> TYPE = CustomAppEngPayload.createType("inventory_action");

    @Override
    public Type<InventoryActionPacket> type() {
        return TYPE;
    }

    // api
    public InventoryActionPacket(InventoryAction action, int slot, long id) {
        this(action, slot, id, ItemStack.EMPTY);
    }

    // api
    public InventoryActionPacket(InventoryAction action, int slot, ItemStack slotItem) {
        this(action, slot, 0, slotItem.copy());
        if (Platform.isClient() && action != InventoryAction.SET_FILTER) {
            throw new IllegalStateException("invalid packet, client cannot post inv actions with stacks.");
        }
    }

    public static InventoryActionPacket decode(RegistryFriendlyByteBuf stream) {
        var action = stream.readEnum(InventoryAction.class);
        var slot = stream.readInt();
        var extraId = stream.readLong();
        var slotItem = ByteBufCodecs.OPTIONAL_ITEM_STACK.decode(stream);
        return new InventoryActionPacket(action, slot, extraId, slotItem);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeEnum(action);
        data.writeInt(slot);
        data.writeLong(extraId);
        ByteBufCodecs.OPTIONAL_ITEM_STACK.encode(data, slotItem);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AEBaseMenu baseMenu) {
            if (action == InventoryAction.SET_FILTER) {
                baseMenu.setFilter(this.slot, this.slotItem);
            } else {
                baseMenu.doAction(player, this.action, this.slot, this.extraId);
            }
        }
    }

}
