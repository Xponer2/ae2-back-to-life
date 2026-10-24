package appeng.menu.locator;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Locates a menu host based on {@link IMenuItem} in the player inventory.
 * <p/>
 * Optionally also contains a block position and side in case the menu is to be opened by the item but for a clicked
 * host (i.e. network tool).
 */
final class InventoryItemLocator implements ItemMenuHostLocator {
    private final int itemIndex;
    private final RayTraceResult hitResult;

    public InventoryItemLocator(int itemIndex, @Nullable RayTraceResult hitResult) {
        this.itemIndex = itemIndex;
        this.hitResult = hitResult;
    }

    public int itemIndex() {
        return itemIndex;
    }

    @Nullable public RayTraceResult hitResult() {
        return hitResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItemLocator)) return false;
        InventoryItemLocator that = (InventoryItemLocator) o;
        return itemIndex == that.itemIndex
                && java.util.Objects.equals(hitResult, that.hitResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(itemIndex, hitResult);
    }

    public ItemStack locateItem(EntityPlayer player) {
        return player.inventory.getStackInSlot(itemIndex);
    }

    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeInt(itemIndex);
        buf.writeOptional(Optional.ofNullable(hitResult), RegistryFriendlyByteBuf::writeBlockHitResult);
    }

    public static InventoryItemLocator readFromPacket(RegistryFriendlyByteBuf buf) {
        return new InventoryItemLocator(
                buf.readInt(),
                buf.readOptional(RegistryFriendlyByteBuf::readBlockHitResult).orElse(null));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("slot ").append(itemIndex);
        if (hitResult != null) {
            result.append(" used on ").append(hitResult.getBlockPos());
        }
        return result.toString();
    }

    @Override
    public Integer getPlayerInventorySlot() {
        return itemIndex;
    }
}
