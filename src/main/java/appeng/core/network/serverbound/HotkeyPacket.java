package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import appeng.client.Hotkey;
import appeng.core.AELog;
import appeng.core.localization.PlayerMessages;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.hotkeys.HotkeyActions;

public final class HotkeyPacket implements ServerboundPacket {
    private final String hotkey;

    public HotkeyPacket(String hotkey) {
        this.hotkey = hotkey;
    }

    public String hotkey() {
        return hotkey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HotkeyPacket)) return false;
        HotkeyPacket that = (HotkeyPacket) o;
        return java.util.Objects.equals(hotkey, that.hotkey);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hotkey);
    }

    @Override
    public String toString() {
        return "HotkeyPacket[" + "hotkey=" + hotkey + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, HotkeyPacket> STREAM_CODEC = StreamCodec.ofMember(
            HotkeyPacket::write,
            HotkeyPacket::decode);

    public static final Type<HotkeyPacket> TYPE = CustomAppEngPayload.createType("hotkey");

    @Override
    public Type<HotkeyPacket> type() {
        return TYPE;
    }

    public HotkeyPacket(Hotkey hotkey) {
        this(hotkey.name());
    }

    public static HotkeyPacket decode(RegistryFriendlyByteBuf stream) {
        var hotkey = stream.readUtf();
        return new HotkeyPacket(hotkey);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUtf(this.hotkey);
    }

    public void handleOnServer(EntityPlayerMP player) {
        var actions = HotkeyActions.REGISTRY.get(hotkey);
        if (actions == null) {
            player.sendMessage(
                    PlayerMessages.UnknownHotkey.text().createCopy()
                            .appendSibling(new TextComponentTranslation("key.ae2." + hotkey)));
            AELog.warn("Player %s tried using unknown hotkey \"%s\"", player, hotkey);
            return;
        }

        for (var action : actions) {
            if (action.run(player)) {
                break;
            }
        }
    }
}
