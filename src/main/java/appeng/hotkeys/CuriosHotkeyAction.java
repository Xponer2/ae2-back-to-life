package appeng.hotkeys;

import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.features.HotkeyAction;
import appeng.api.util.ItemLike;

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
        this((stack) -> stack.getItem() == item.asItem(), opener);
    }

    @Override
    public boolean run(EntityPlayer player) {
        // There is no Curios for this version, so there are no curio slots to search
        return false;
    }
}
