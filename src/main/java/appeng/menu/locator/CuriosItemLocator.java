package appeng.menu.locator;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;

import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Implements {@link ItemMenuHostLocator} for items equipped in curios slots.
 */
final class CuriosItemLocator implements ItemMenuHostLocator {
    private final int curioSlot;
    private final RayTraceResult hitResult;

    public CuriosItemLocator(int curioSlot, @Nullable RayTraceResult hitResult) {
        this.curioSlot = curioSlot;
        this.hitResult = hitResult;
    }

    public int curioSlot() {
        return curioSlot;
    }

    @Nullable public RayTraceResult hitResult() {
        return hitResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CuriosItemLocator)) return false;
        CuriosItemLocator that = (CuriosItemLocator) o;
        return curioSlot == that.curioSlot
                && java.util.Objects.equals(hitResult, that.hitResult);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(curioSlot, hitResult);
    }

    public ItemStack locateItem(EntityPlayer player) {
        // Curios doesn't exist for 1.12.2 (Baubles would be the counterpart), so curio slots are always empty
        return ItemStack.EMPTY;
    }

    public void writeToPacket(RegistryFriendlyByteBuf buf) {
        buf.writeInt(curioSlot);
        buf.writeOptional(Optional.ofNullable(hitResult), RegistryFriendlyByteBuf::writeBlockHitResult);
    }

    public static CuriosItemLocator readFromPacket(RegistryFriendlyByteBuf buf) {
        return new CuriosItemLocator(
                buf.readInt(),
                buf.readOptional(RegistryFriendlyByteBuf::readBlockHitResult).orElse(null));
    }

    @Override
    public String toString() {
        return "curios slot " + curioSlot;
    }
}
