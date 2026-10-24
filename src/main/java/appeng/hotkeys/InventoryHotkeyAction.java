package appeng.hotkeys;

import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.features.HotkeyAction;
import appeng.api.util.ItemLike;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;

public final class InventoryHotkeyAction implements HotkeyAction {
    private final Predicate<ItemStack> locatable;
    private final Opener opener;

    public InventoryHotkeyAction(Predicate<ItemStack> locatable, Opener opener) {
        this.locatable = locatable;
        this.opener = opener;
    }

    public Predicate<ItemStack> locatable() {
        return locatable;
    }

    public Opener opener() {
        return opener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryHotkeyAction)) return false;
        InventoryHotkeyAction that = (InventoryHotkeyAction) o;
        return java.util.Objects.equals(locatable, that.locatable)
                && java.util.Objects.equals(opener, that.opener);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(locatable, opener);
    }

    @Override
    public String toString() {
        return "InventoryHotkeyAction[" + "locatable=" + locatable + ", " + "opener=" + opener + "]";
    }

    public InventoryHotkeyAction(ItemLike item, Opener opener) {
        this((stack) -> stack.is(item.asItem()), opener);
    }

    @Override
    public boolean run(EntityPlayer player) {
        var items = player.inventory.items;
        for (int i = 0; i < items.size(); i++) {
            if (locatable.test(items.get(i))) {
                if (opener.open(player, MenuLocators.forInventorySlot(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface Opener {
        boolean open(EntityPlayer player, ItemMenuHostLocator locator);
    }
}
