
package appeng.core.network.clientbound;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;

/**
 * Sends the content for a single {@link appeng.helpers.patternprovider.PatternContainer} shown in the pattern access
 * terminal to the client.
 */
public final class PatternAccessTerminalPacket implements ClientboundPacket {
    private final boolean fullUpdate;
    private final long inventoryId;
    private final int inventorySize;
    private final long sortBy;
    private final PatternContainerGroup group;
    private final Int2ObjectMap<ItemStack> slots;

    public PatternAccessTerminalPacket(boolean fullUpdate, long inventoryId, int inventorySize, long sortBy, PatternContainerGroup group, Int2ObjectMap<ItemStack> slots) {
        this.fullUpdate = fullUpdate;
        this.inventoryId = inventoryId;
        this.inventorySize = inventorySize;
        this.sortBy = sortBy;
        this.group = group;
        this.slots = slots;
    }

    public boolean fullUpdate() {
        return fullUpdate;
    }

    public long inventoryId() {
        return inventoryId;
    }

    public int inventorySize() {
        return inventorySize;
    }

    public long sortBy() {
        return sortBy;
    }

    public PatternContainerGroup group() {
        return group;
    }

    public Int2ObjectMap<ItemStack> slots() {
        return slots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatternAccessTerminalPacket)) return false;
        PatternAccessTerminalPacket that = (PatternAccessTerminalPacket) o;
        return fullUpdate == that.fullUpdate
                && inventoryId == that.inventoryId
                && inventorySize == that.inventorySize
                && sortBy == that.sortBy
                && java.util.Objects.equals(group, that.group)
                && java.util.Objects.equals(slots, that.slots);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fullUpdate, inventoryId, inventorySize, sortBy, group, slots);
    }

    @Override
    public String toString() {
        return "PatternAccessTerminalPacket[" + "fullUpdate=" + fullUpdate + ", " + "inventoryId=" + inventoryId + ", " + "inventorySize=" + inventorySize + ", " + "sortBy=" + sortBy + ", " + "group=" + group + ", " + "slots=" + slots + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternAccessTerminalPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    PatternAccessTerminalPacket::write,
                    PatternAccessTerminalPacket::decode);

    private static final StreamCodec<RegistryFriendlyByteBuf, Int2ObjectMap<ItemStack>> SLOTS_STREAM_CODEC = ByteBufCodecs
            .map(
                    Int2ObjectOpenHashMap::new, ByteBufCodecs.SHORT.map(Short::intValue, Integer::shortValue),
                    ByteBufCodecs.OPTIONAL_ITEM_STACK, 128);

    public static final Type<PatternAccessTerminalPacket> TYPE = CustomAppEngPayload
            .createType("pattern_access_terminal");

    @Override
    public Type<PatternAccessTerminalPacket> type() {
        return TYPE;
    }

    public static PatternAccessTerminalPacket decode(RegistryFriendlyByteBuf stream) {
        var inventoryId = stream.readVarLong();
        var fullUpdate = stream.readBoolean();
        int inventorySize = 0;
        long sortBy = 0;
        PatternContainerGroup group = null;
        if (fullUpdate) {
            inventorySize = stream.readVarInt();
            sortBy = stream.readVarLong();
            group = PatternContainerGroup.readFromPacket(stream);
        }

        var slots = SLOTS_STREAM_CODEC.decode(stream);
        return new PatternAccessTerminalPacket(fullUpdate, inventoryId, inventorySize, sortBy, group, slots);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeVarLong(inventoryId);
        data.writeBoolean(fullUpdate);
        if (fullUpdate) {
            data.writeVarInt(inventorySize);
            data.writeVarLong(sortBy);
            group.writeToPacket(data);
        }
        SLOTS_STREAM_CODEC.encode(data, slots);
    }

    public static PatternAccessTerminalPacket fullUpdate(long inventoryId,
            int inventorySize,
            long sortBy,
            PatternContainerGroup group,
            Int2ObjectMap<ItemStack> slots) {
        return new PatternAccessTerminalPacket(
                true,
                inventoryId,
                inventorySize,
                sortBy,
                group,
                slots);
    }

    public static PatternAccessTerminalPacket incrementalUpdate(long inventoryId,
            Int2ObjectMap<ItemStack> slots) {
        return new PatternAccessTerminalPacket(
                false,
                inventoryId,
                0,
                0,
                null,
                slots);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (Minecraft.getMinecraft().currentScreen instanceof PatternAccessTermScreen<?> patternAccessTerminal) {
            if (fullUpdate) {
                patternAccessTerminal.postFullUpdate(this.inventoryId, sortBy, group, inventorySize, slots);
            } else {
                patternAccessTerminal.postIncrementalUpdate(this.inventoryId, slots);
            }
        }
    }
}
