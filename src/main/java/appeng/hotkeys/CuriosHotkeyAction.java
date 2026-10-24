package appeng.hotkeys;

import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.features.HotkeyAction;
import appeng.api.util.ItemLike;
import appeng.integration.modules.curios.CuriosIntegration;
import appeng.menu.locator.MenuLocators;

public final class CuriosHotkeyAction implements HotkeyAction {
    private final Predicate<ItemStack> locatable;
    private final InventoryHotkeyAction.Opener opener;

    public CuriosHotkeyAction(Predicate<ItemStack> locatable, InventoryHotkeyAction.Opener opener) {
        this.locatable = locatable;
        this.opener = opener;
    }

    public Predicate<ItemStack> locatable() {
        return locatable;
    }

    public InventoryHotkeyAction.Opener opener() {
        return opener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CuriosHotkeyAction)) return false;
        CuriosHotkeyAction that = (CuriosHotkeyAction) o;
        return java.util.Objects.equals(locatable, that.locatable)
                && java.util.Objects.equals(opener, that.opener);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(locatable, opener);
    }

    @Override
    public String toString() {
        return "CuriosHotkeyAction[" + "locatable=" + locatable + ", " + "opener=" + opener + "]";
    }

    public CuriosHotkeyAction(ItemLike item, InventoryHotkeyAction.Opener opener) {
        this((stack) -> stack.is(item.asItem()), opener);
    }

    @Override
    public boolean run(EntityPlayer player) {
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null)
            return false;
        for (int i = 0; i < cap.getSlots(); i++) {
            if (locatable.test(cap.getStackInSlot(i))) {
                if (opener.open(player, MenuLocators.forCurioSlot(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
