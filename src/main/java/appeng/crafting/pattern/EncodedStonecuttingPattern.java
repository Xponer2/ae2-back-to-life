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

public final class EncodedStonecuttingPattern {
    private final ItemStack input;
    private final ItemStack output;
    private final boolean canSubstitute;
    private final ResourceLocation recipeId;

    public EncodedStonecuttingPattern(ItemStack input, ItemStack output, boolean canSubstitute, ResourceLocation recipeId) {
        this.input = input;
        this.output = output;
        this.canSubstitute = canSubstitute;
        this.recipeId = recipeId;
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public boolean canSubstitute() {
        return canSubstitute;
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    @Override
    public String toString() {
        return "EncodedStonecuttingPattern[" + "input=" + input + ", " + "output=" + output + ", " + "canSubstitute=" + canSubstitute + ", " + "recipeId=" + recipeId + "]";
    }

    public static final Codec<EncodedStonecuttingPattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            AECodecs.ITEM_STACK.fieldOf("input").forGetter(EncodedStonecuttingPattern::input),
            AECodecs.ITEM_STACK.fieldOf("output").forGetter(EncodedStonecuttingPattern::output),
            Codec.BOOL.fieldOf("canSubstitute").forGetter(EncodedStonecuttingPattern::canSubstitute),
            AECodecs.RESOURCE_LOCATION.fieldOf("recipeId").forGetter(EncodedStonecuttingPattern::recipeId))
            .apply(builder, EncodedStonecuttingPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedStonecuttingPattern> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.ITEM_STACK,
                    EncodedStonecuttingPattern::input,
                    ByteBufCodecs.ITEM_STACK,
                    EncodedStonecuttingPattern::output,
                    ByteBufCodecs.BOOL,
                    EncodedStonecuttingPattern::canSubstitute,
                    ByteBufCodecs.RESOURCE_LOCATION,
                    EncodedStonecuttingPattern::recipeId,
                    EncodedStonecuttingPattern::new);

    public boolean containsMissingContent() {
        return AEItems.MISSING_CONTENT.is(input) || AEItems.MISSING_CONTENT.is(output);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;

        EncodedStonecuttingPattern that = (EncodedStonecuttingPattern) object;
        return canSubstitute == that.canSubstitute
                && ItemStack.matches(input, that.input)
                && ItemStack.matches(output, that.output)
                && recipeId.equals(that.recipeId);
    }

    @Override
    public int hashCode() {
        int result = ItemStack.hashItemAndComponents(input);
        result = 31 * result + ItemStack.hashItemAndComponents(output);
        result = 31 * result + Boolean.hashCode(canSubstitute);
        result = 31 * result + recipeId.hashCode();
        return result;
    }
}
