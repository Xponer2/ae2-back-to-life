package appeng.menu.me.items;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.menu.MenuType;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;

public class BasicCellChestMenu extends MEStorageMenu {
    public static final MenuType<MEStorageMenu> TYPE = MenuTypeBuilder
            .<MEStorageMenu, ITerminalHost>create(BasicCellChestMenu::new, ITerminalHost.class)
            .build("basic_cell_chest");

    public BasicCellChestMenu(MenuType<?> menuType, int id, InventoryPlayer ip, ITerminalHost host) {
        super(menuType, id, ip, host);
    }

    /**
     * {@link MEChestBlockEntity} is an instance of {@link KeyTypeSelectionHost}, however basic cells only support a
     * single type.
     */
    @Override
    public boolean canConfigureTypeFilter() {
        return false;
    }
}
