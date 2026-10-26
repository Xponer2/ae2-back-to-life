package appeng.menu.locator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;

/**
 * A locator for stacks of items that implement {@link appeng.api.implementations.menuobjects.IMenuItem}. This allows
 * such items to be stored in various places (player inventory, curio slots) when they host a menu.
 */
public interface ItemMenuHostLocator extends MenuHostLocator {
    Logger LOG = LogManager.getLogger(ItemMenuHostLocator.class);

    default <T> T locate(EntityPlayer player, Class<T> hostInterface) {
        ItemStack it = locateItem(player);

        if (!it.isEmpty() && it.getItem() instanceof IMenuItem menuItem) {
            var menuHost = menuItem.getMenuHost(player, this, hitResult());
            if (hostInterface.isInstance(menuHost)) {
                return hostInterface.cast(menuHost);
            } else if (menuHost != null) {
                LogManager.getLogger(ItemMenuHostLocator.class).warn("Item in {} of {} did not create a compatible menu of type {}: {}",
                        this, player, hostInterface, menuHost);
            }
        } else {
            LogManager.getLogger(ItemMenuHostLocator.class).warn("Item in {} of {} is not an IMenuItem: {}",
                    this, player, it);
        }

        return null;
    }

    /**
     * @return The optional location where the item was used on to open the menu.
     */
    @Nullable
    RayTraceResult hitResult();

    /**
     * Locates the MenuItem in the Players inventory and returns it if it satisfies the expected menu host interface.
     * <strong>The returned stack will be modified by the {@link ItemMenuHost}</strong>, it must be updated in-place.
     */
    ItemStack locateItem(EntityPlayer player);

    /**
     * @return The slot of the item in the player inventory if this locator represents a location in the player
     *         inventory. Used to lock the slot against accidentally moving the item out.
     */
    @Nullable
    default Integer getPlayerInventorySlot() {
        return null;
    }
}
