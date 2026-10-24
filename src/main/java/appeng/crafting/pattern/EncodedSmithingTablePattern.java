package appeng.crafting.pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.core.definitions.AEItems;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.util.AECodecs;

public final class EncodedSmithingTablePattern {
    private final ItemStack template;
    private final ItemStack base;
    private final ItemStack addition;
    private final ItemStack resultItem;
    private final boolean canSubstitute;
    private final ResourceLocation recipeId;

    public EncodedSmithingTablePattern(ItemStack template, ItemStack base, ItemStack addition, ItemStack resultItem, boolean canSubstitute, ResourceLocation recipeId) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.resultItem = resultItem;
        this.canSubstitute = canSubstitute;
        this.recipeId = recipeId;
    }

    public ItemStack template() {
        return template;
    }

    public ItemStack base() {
        return base;
    }

    public ItemStack addition() {
        return addition;
    }

    public ItemStack resultItem() {
        return resultItem;
    }

    public boolean canSubstitute() {
        return canSubstitute;
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    @Override
    public String toString() {
        return "EncodedSmithingTablePattern[" + "template=" + template + ", " + "base=" + base + ", " + "addition=" + addition + ", " + "resultItem=" + resultItem + ", " + "canSubstitute=" + canSubstitute + ", " + "recipeId=" + recipeId + "]";
    }

    public static final Codec<EncodedSmithingTablePattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            AECodecs.ITEM_STACK.fieldOf("template").forGetter(EncodedSmithingTablePattern::template),
            AECodecs.ITEM_STACK.fieldOf("base").forGetter(EncodedSmithingTablePattern::base),
            AECodecs.ITEM_STACK.fieldOf("addition").forGetter(EncodedSmithingTablePattern::addition),
            AECodecs.ITEM_STACK.fieldOf("resultItem").forGetter(EncodedSmithingTablePattern::resultItem),
            Codec.BOOL.fieldOf("canSubstitute").forGetter(EncodedSmithingTablePattern::canSubstitute),
            AECodecs.RESOURCE_LOCATION.fieldOf("recipeId").forGetter(EncodedSmithingTablePattern::recipeId))
            .apply(builder, EncodedSmithingTablePattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedSmithingTablePattern> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.ITEM_STACK,
                    EncodedSmithingTablePattern::template,
                    ByteBufCodecs.ITEM_STACK,
                    EncodedSmithingTablePattern::base,
                    ByteBufCodecs.ITEM_STACK,
                    EncodedSmithingTablePattern::addition,
                    ByteBufCodecs.ITEM_STACK,
                    EncodedSmithingTablePattern::resultItem,
                    ByteBufCodecs.BOOL,
                    EncodedSmithingTablePattern::canSubstitute,
                    ByteBufCodecs.RESOURCE_LOCATION,
                    EncodedSmithingTablePattern::recipeId,
                    EncodedSmithingTablePattern::new);

    public boolean containsMissingContent() {
        return AEItems.MISSING_CONTENT.is(template)
                || AEItems.MISSING_CONTENT.is(base)
                || AEItems.MISSING_CONTENT.is(addition)
                || AEItems.MISSING_CONTENT.is(resultItem);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;

        EncodedSmithingTablePattern that = (EncodedSmithingTablePattern) object;
        return canSubstitute == that.canSubstitute
                && ItemStack.matches(base, that.base)
                && ItemStack.matches(template, that.template)
                && ItemStack.matches(addition, that.addition)
                && ItemStack.matches(resultItem, that.resultItem)
                && recipeId.equals(that.recipeId);
    }

    @Override
    public int hashCode() {
        int result = ItemStack.hashItemAndComponents(template);
        result = 31 * result + ItemStack.hashItemAndComponents(base);
        result = 31 * result + ItemStack.hashItemAndComponents(addition);
        result = 31 * result + ItemStack.hashItemAndComponents(resultItem);
        result = 31 * result + Boolean.hashCode(canSubstitute);
        result = 31 * result + recipeId.hashCode();
        return result;
    }
}
