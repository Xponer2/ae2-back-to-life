package appeng.items.misc;

import appeng.util.TextUtil;

import appeng.util.ItemProperties;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.api.stacks.GenericStack;
import appeng.util.ItemTooltipContext;
import appeng.items.AEBaseItem;
import appeng.core.CreativeTabOutput;

public class MissingContentItem extends AEBaseItem {
    public MissingContentItem(ItemProperties properties) {
        super(properties);
    }

    @Nullable
    public BrokenStackInfo getBrokenStackInfo(ItemStack stack) {
        // This is a broken pattern entry, try to recover the ID and use it as the tooltip line
        var itemStackData = AEComponents.MISSING_CONTENT_ITEMSTACK_DATA.get(stack);
        var genericStackData = AEComponents.MISSING_CONTENT_AEKEY_DATA.get(stack);

        // "id" is just the most common ID field for key types
        if (itemStackData != null && itemStackData.hasKey("id")) {
            var brokenDataTag = itemStackData;
            if (!brokenDataTag.hasKey("id", Constants.NBT.TAG_STRING)) {
                return null; // Without any ID this info is worthless
            }
            var missingId = brokenDataTag.getString("id");

            long amount;
            try {
                amount = Math.max(1, brokenDataTag.getLong("count"));
            } catch (Exception ignored) {
                amount = 1;
            }

            return new BrokenStackInfo(new TextComponentString(missingId), AEKeyType.items(), amount);
        } else if (genericStackData != null && genericStackData.hasKey("id")) {
            var brokenDataTag = genericStackData;
            if (!brokenDataTag.hasKey("id", Constants.NBT.TAG_STRING)) {
                return null; // Without any ID this info is worthless
            }
            var missingId = new TextComponentString(brokenDataTag.getString("id"));

            // Try figuring out which AEKeyType it may have been. If that fails just default to item
            AEKeyType keyType = null;
            try {
                var keyTypeString = brokenDataTag.getString(AEKey.TYPE_FIELD);
                keyType = AEKeyTypesInternal.get(new ResourceLocation(keyTypeString));
                if (keyType == null) {
                    missingId.appendText(" (").appendText(keyTypeString).appendText(")");
                }
            } catch (Exception ignored) {
            }

            long amount;
            try {
                amount = Math.max(1, brokenDataTag.getLong(GenericStack.AMOUNT_FIELD));
            } catch (Exception ignored) {
                amount = 1;
            }

            return new BrokenStackInfo(missingId, keyType, amount);
        }

        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> lines, ITooltipFlag advanced) {
        super.appendHoverText(stack, context, lines, advanced);

        var error = AEComponents.MISSING_CONTENT_ERROR.get(stack);
        if (error != null) {
            lines.add(TextUtil.withStyle(new TextComponentString(error), TextFormatting.GRAY));
        }
    }

    public final static class BrokenStackInfo {
        private final ITextComponent displayName;
        private final AEKeyType keyType;
        private final long amount;

        public BrokenStackInfo(ITextComponent displayName, @Nullable AEKeyType keyType, long amount) {
            this.displayName = displayName;
            this.keyType = keyType;
            this.amount = amount;
        }

        public ITextComponent displayName() {
            return displayName;
        }

        @Nullable public AEKeyType keyType() {
            return keyType;
        }

        public long amount() {
            return amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenStackInfo)) return false;
            BrokenStackInfo that = (BrokenStackInfo) o;
            return java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(keyType, that.keyType)
                    && amount == that.amount;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(displayName, keyType, amount);
        }

        @Override
        public String toString() {
            return "BrokenStackInfo[" + "displayName=" + displayName + ", " + "keyType=" + keyType + ", " + "amount=" + amount + "]";
        }

    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // Only exists for content that could not be loaded
    }
}
