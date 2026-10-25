/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.crafting.pattern;

import appeng.util.TextUtil;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import appeng.api.crafting.EncodedPatternDecoder;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.InvalidPatternTooltipStrategy;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import appeng.items.misc.MissingContentItem;
import appeng.items.misc.WrappedGenericStack;
import appeng.util.InteractionUtil;
import appeng.util.UseOnContext;

/**
 * Reusable item class for encoded patterns.
 *
 * @param <T>
 */
public class EncodedPatternItem<T extends IPatternDetails> extends AEBaseItem {
    // rather simple client side caching.
    private static final Map<ItemStack, ItemStack> SIMPLE_CACHE = new WeakHashMap<>();

    private final EncodedPatternDecoder<T> decoder;

    @Nullable
    private final InvalidPatternTooltipStrategy invalidPatternTooltip;

    public EncodedPatternItem(ItemProperties properties,
            EncodedPatternDecoder<T> decoder,
            @Nullable InvalidPatternTooltipStrategy invalidPatternTooltip) {
        super(properties);
        this.decoder = decoder;
        this.invalidPatternTooltip = invalidPatternTooltip;
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // Don't show in creative mode, since it's not useful without NBT
    }

    @Override
    public ActionResult<ItemStack> use(World level, EntityPlayer player, EnumHand hand) {
        this.clearPattern(player.getHeldItem(hand), player);

        return new ActionResult<>(EnumActionResult.SUCCESS,
                player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return this.clearPattern(stack, context.getPlayer())
                ? EnumActionResult.SUCCESS
                : EnumActionResult.PASS;
    }

    private boolean clearPattern(ItemStack stack, EntityPlayer player) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            if (player.getEntityWorld().isRemote) {
                return false;
            }

            final InventoryPlayer inv = player.inventory;

            ItemStack is = AEItems.BLANK_PATTERN.stack(stack.getCount());
            if (!is.isEmpty()) {
                for (int s = 0; s < player.inventory.getSizeInventory(); s++) {
                    if (inv.getStackInSlot(s) == stack) {
                        inv.setInventorySlotContents(s, is);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> lines,
            ITooltipFlag flags) {
        var what = AEItemKey.of(stack);
        if (what == null) {
            // This can be called very early to index tooltips for search. In those cases,
            // there is no encoded pattern present.
            return;
        }

        var clientLevel = AppEng.instance().getClientLevel();
        if (clientLevel == null) {
            return; // Showing pattern details will only work reliably client-side
        }

        PatternDetailsTooltip tooltip;
        try {
            var details = Objects.requireNonNull(decoder.decode(what, clientLevel), "decoder returned null");
            tooltip = details.getTooltip(clientLevel, flags);
        } catch (Exception e) {
            lines.add(TextUtil.withStyle(GuiText.InvalidPattern.text().createCopy(), TextFormatting.RED));
            if (invalidPatternTooltip != null) {
                tooltip = invalidPatternTooltip.getTooltip(stack, clientLevel, e, flags);
            } else {
                tooltip = null;
            }
        }

        if (tooltip != null) {
            var label = new TextComponentString("").appendSibling(tooltip.getOutputMethod())
                    .appendText(": ").withStyle(TextFormatting.GRAY);
            var and = new TextComponentString(" ").appendSibling(GuiText.And.text())
                    .appendText(" ").withStyle(TextFormatting.GRAY);
            var with = GuiText.With.text().createCopy().appendText(": ").withStyle(TextFormatting.GRAY);

            boolean first = true;
            for (var output : tooltip.getOutputs()) {
                lines.add(new TextComponentString("").appendSibling(first ? label : and).appendSibling(getTooltipEntryLine(output)));
                first = false;
            }

            first = true;
            for (var input : tooltip.getInputs()) {
                lines.add(new TextComponentString("").appendSibling(first ? with : and).appendSibling(getTooltipEntryLine(input)));
                first = false;
            }

            for (var property : tooltip.getProperties()) {
                if (property.value() != null) {
                    lines.add(new TextComponentString("").appendSibling(property.name())
                            .append(TextUtil.withStyle(new TextComponentString(": "), TextFormatting.GRAY))
                            .append(property.value()));
                } else {
                    lines.add(TextUtil.withStyle(new TextComponentString(""), TextFormatting.GRAY).append(property.name()));
                }
            }
        }
    }

    protected static ITextComponent getTooltipEntryLine(GenericStack stack) {
        if (stack.what() instanceof AEItemKey itemKey
                && itemKey.getReadOnlyStack().getItem() instanceof MissingContentItem missingContentItem) {
            var brokenStackInfo = missingContentItem.getBrokenStackInfo(itemKey.getReadOnlyStack());
            if (brokenStackInfo != null) {
                return getTooltipEntryLine(TextUtil.withStyle(brokenStackInfo.displayName().createCopy(), TextFormatting.RED),
                        brokenStackInfo.keyType(), brokenStackInfo.amount());
            }
        }

        return getTooltipEntryLine(stack.what().getDisplayName(), stack.what().getType(), stack.amount());
    }

    protected static ITextComponent getTooltipEntryLine(ITextComponent displayName, @Nullable AEKeyType amountType, long amount) {
        if (amount > 0) {
            var amountInfo = new TextComponentString(amountType != null ? amountType.formatAmount(amount, AmountFormat.FULL)
                    : String.valueOf(amount));
            return amountInfo.append(TextUtil.withStyle(new TextComponentString(" x "), TextFormatting.GRAY)).append(displayName);
        } else {
            return displayName;
        }
    }

    /**
     * Returns the item stack that should be shown for this pattern when shift is held down.
     */
    public ItemStack getOutput(ItemStack item) {
        var out = SIMPLE_CACHE.get(item);

        if (out != null) {
            return out;
        }

        var level = AppEng.instance().getClientLevel();
        if (level == null) {
            return ItemStack.EMPTY;
        }

        var details = decode(item, level);
        out = ItemStack.EMPTY;

        if (details != null) {
            var output = details.getPrimaryOutput();

            // Can only be an item or fluid stack.
            if (output.what() instanceof AEItemKey itemKey) {
                out = itemKey.toStack();
            } else {
                out = WrappedGenericStack.wrap(output.what(), 0);
            }
        }

        SIMPLE_CACHE.put(item, out);
        return out;
    }

    @Nullable
    public IPatternDetails decode(ItemStack stack, World level) {
        if (stack.getItem() != this || level == null) {
            return null;
        }

        var what = AEItemKey.of(stack);
        try {
            return Objects.requireNonNull(decoder.decode(what, level), "decoder returned null");
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public IPatternDetails decode(AEItemKey what, World level) {
        if (what == null) {
            return null;
        }

        try {
            return decoder.decode(what, level);
        } catch (Exception e) {
            return null;
        }
    }
}
