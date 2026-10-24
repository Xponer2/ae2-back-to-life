package appeng.api.behaviors;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;

/**
 * Strategy to interact with the non-item keys held by container items, for example the fluid contained in a bucket.
 *
 * @param <C> Any context object that can accept or offer resources, directly or indirectly. Usually the API instance
 *            such as {@code Storage<FluidVariant> on fabric}.
 */
@ApiStatus.Experimental
public interface ContainerItemStrategy<T extends AEKey, C> {
    @Nullable
    GenericStack getContainedStack(ItemStack stack);

    @Nullable
    C findCarriedContext(EntityPlayer player, Container menu);

    @Nullable
    default C findPlayerSlotContext(EntityPlayer player, int slot) {
        return null;
    }

    long extract(C context, T what, long amount, Actionable mode);

    long insert(C context, T what, long amount, Actionable mode);

    void playFillSound(EntityPlayer player, T what);

    void playEmptySound(EntityPlayer player, T what);

    @Nullable
    GenericStack getExtractableContent(C context);

    static <T extends AEKey> void register(AEKeyType keyType, Class<T> keyClass, ContainerItemStrategy<T, ?> strategy) {
        ContainerItemStrategies.register(keyType, keyClass, strategy);
    }

}
