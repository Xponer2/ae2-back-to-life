
package appeng.core.network.serverbound;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.MenuType;
import appeng.util.BuiltInRegistries;

public final class SwitchGuisPacket implements ServerboundPacket {
    private final MenuType<? extends ISubMenu> newGui;

    public SwitchGuisPacket(@Nullable MenuType<? extends ISubMenu> newGui) {
        this.newGui = newGui;
    }

    @Nullable public MenuType<? extends ISubMenu> newGui() {
        return newGui;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SwitchGuisPacket)) return false;
        SwitchGuisPacket that = (SwitchGuisPacket) o;
        return java.util.Objects.equals(newGui, that.newGui);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(newGui);
    }

    @Override
    public String toString() {
        return "SwitchGuisPacket[" + "newGui=" + newGui + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchGuisPacket> STREAM_CODEC = StreamCodec.ofMember(
            SwitchGuisPacket::write,
            SwitchGuisPacket::decode);

    public static final Type<SwitchGuisPacket> TYPE = CustomAppEngPayload.createType("switch_guis");

    @Override
    public Type<SwitchGuisPacket> type() {
        return TYPE;
    }

    @SuppressWarnings("unchecked")
    public static SwitchGuisPacket decode(RegistryFriendlyByteBuf stream) {
        MenuType<? extends ISubMenu> newGui = null;
        if (stream.readBoolean()) {
            newGui = (MenuType<? extends ISubMenu>) MenuType.byId(stream.readResourceLocation());
        }
        return new SwitchGuisPacket(newGui);
    }

    public void write(RegistryFriendlyByteBuf data) {
        if (newGui != null) {
            data.writeBoolean(true);
            data.writeResourceLocation(newGui.getId());
        } else {
            data.writeBoolean(false);
        }
    }

    /**
     * Opens a sub-menu for the current menu, which will allow the player to return to the previous menu.
     */
    public static SwitchGuisPacket openSubMenu(MenuType<? extends ISubMenu> menuType) {
        return new SwitchGuisPacket(menuType);
    }

    /**
     * Creates a packet that instructs the server to return to the parent menu for the currently opened sub-menu.
     */
    public static SwitchGuisPacket returnToParentMenu() {
        return new SwitchGuisPacket((MenuType<? extends ISubMenu>) null);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (this.newGui != null) {
            doOpenSubMenu(player);
        } else {
            doReturnToParentMenu(player);
        }
    }

    private void doOpenSubMenu(EntityPlayerMP player) {
        if (player.openContainer instanceof AEBaseMenu bc) {
            var locator = bc.getLocator();
            if (locator != null) {
                MenuOpener.open(newGui, player, locator);
            }
        }
    }

    private void doReturnToParentMenu(EntityPlayerMP player) {
        if (player.openContainer instanceof ISubMenu subMenu) {
            subMenu.getHost().returnToMainMenu(player, subMenu);
        }
    }
}
