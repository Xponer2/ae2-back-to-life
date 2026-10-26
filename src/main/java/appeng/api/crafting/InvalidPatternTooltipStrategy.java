package appeng.api.crafting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This stragegy is used when a player views the tooltip of an encoded pattern that failed to decode properly. It should
 * attempt to extract as much information as possible from the given tag to help the player identify which recipe the
 * pattern contained originally.
 *
 * @see EncodedPatternItemBuilder#invalidPatternTooltip(InvalidPatternTooltipStrategy)
 */
@FunctionalInterface
public interface InvalidPatternTooltipStrategy {
    /**
     * @param stack The encoded pattern.
     * @param cause The error thrown by the decoder - if any.
     * @param advancedTooltips {@code ITooltipFlag.isAdvanced()}. The flag type is client-only in 1.12.2, and the method
     *                         references used as strategies could not be linked on a dedicated server with it.
     */
    PatternDetailsTooltip getTooltip(ItemStack stack, World level, @Nullable Exception cause,
            boolean advancedTooltips);
}
