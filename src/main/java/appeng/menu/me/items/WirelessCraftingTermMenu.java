package appeng.menu.me.items;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.networking.IGridNode;
import appeng.helpers.WirelessCraftingTerminalMenuHost;
import appeng.menu.MenuType;
import appeng.menu.implementations.MenuTypeBuilder;

/**
 * Can only be used with a host that implements {@link ISegmentedInventory} and exposes an inventory named "crafting" to
 * store the crafting grid and output.
 *
 * @see appeng.client.gui.me.items.CraftingTermScreen
 */
public class WirelessCraftingTermMenu extends CraftingTermMenu {

    public static final MenuType<WirelessCraftingTermMenu> TYPE = MenuTypeBuilder
            .create(WirelessCraftingTermMenu::new, WirelessCraftingTerminalMenuHost.class)
            .build("wirelesscraftingterm");

    private final WirelessCraftingTerminalMenuHost<?> menuHost;

    public WirelessCraftingTermMenu(int id, InventoryPlayer ip, WirelessCraftingTerminalMenuHost monitorable) {
        super(TYPE, id, ip, monitorable, false);
        this.createPlayerInventorySlots(ip);
        this.menuHost = monitorable;
    }

    public IGridNode getGridNode() {
        return menuHost.getActionableNode();
    }
}
