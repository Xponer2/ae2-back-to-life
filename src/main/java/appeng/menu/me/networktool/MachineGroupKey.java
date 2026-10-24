package appeng.menu.me.networktool;

import appeng.api.stacks.AEItemKey;
import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * The information by which machines are grouped together.
 */
final class MachineGroupKey {
    private final AEItemKey display;
    private final boolean missingChannel;

    public MachineGroupKey(AEItemKey display, boolean missingChannel) {
        this.display = display;
        this.missingChannel = missingChannel;
    }

    public AEItemKey display() {
        return display;
    }

    public boolean missingChannel() {
        return missingChannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineGroupKey)) return false;
        MachineGroupKey that = (MachineGroupKey) o;
        return java.util.Objects.equals(display, that.display)
                && missingChannel == that.missingChannel;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(display, missingChannel);
    }

    @Override
    public String toString() {
        return "MachineGroupKey[" + "display=" + display + ", " + "missingChannel=" + missingChannel + "]";
    }

    public static MachineGroupKey fromPacket(RegistryFriendlyByteBuf data) {
        var display = AEItemKey.fromPacket(data);
        var missingChannel = data.readBoolean();
        return new MachineGroupKey(display, missingChannel);

    }

    public void write(RegistryFriendlyByteBuf data) {
        display.writeToPacket(data);
        data.writeBoolean(missingChannel);
    }

}
