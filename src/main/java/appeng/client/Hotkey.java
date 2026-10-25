package appeng.client;

import net.minecraft.client.settings.KeyBinding;

import appeng.core.network.PacketDistributor;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.HotkeyPacket;

public final class Hotkey {
    private final String name;
    private final KeyBinding mapping;

    public Hotkey(String name, KeyBinding mapping) {
        this.name = name;
        this.mapping = mapping;
    }

    public String name() {
        return name;
    }

    public KeyBinding mapping() {
        return mapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotkey)) return false;
        Hotkey that = (Hotkey) o;
        return java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, mapping);
    }

    @Override
    public String toString() {
        return "Hotkey[" + "name=" + name + ", " + "mapping=" + mapping + "]";
    }

    public void check() {
        while (mapping().isPressed()) {
            ServerboundPacket message = new HotkeyPacket(this);
            PacketDistributor.sendToServer(message);
        }
    }
}
