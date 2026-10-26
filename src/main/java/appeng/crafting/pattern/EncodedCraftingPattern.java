package appeng.crafting.pattern;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.core.definitions.AEItems;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.util.AECodecs;

public final class EncodedCraftingPattern {
    private final List<ItemStack> inputs;
    private final ItemStack result;
    private final ResourceLocation recipeId;
    private final boolean canSubstitute;
    private final boolean canSubstituteFluids;

    public EncodedCraftingPattern(List<ItemStack> inputs, ItemStack result, ResourceLocation recipeId, boolean canSubstitute, boolean canSubstituteFluids) {
        this.inputs = inputs;
        this.result = result;
        this.recipeId = recipeId;
        this.canSubstitute = canSubstitute;
        this.canSubstituteFluids = canSubstituteFluids;
    }

    public List<ItemStack> inputs() {
        return inputs;
    }

    public ItemStack result() {
        return result;
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    public boolean canSubstitute() {
        return canSubstitute;
    }

    public boolean canSubstituteFluids() {
        return canSubstituteFluids;
    }

    @Override
    public String toString() {
        return "EncodedCraftingPattern[" + "inputs=" + inputs + ", " + "result=" + result + ", " + "recipeId=" + recipeId + ", " + "canSubstitute=" + canSubstitute + ", " + "canSubstituteFluids=" + canSubstituteFluids + "]";
    }

    public static final Codec<EncodedCraftingPattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            AECodecs.FAULT_TOLERANT_OPTIONAL_ITEMSTACK_CODEC.listOf().fieldOf("inputs")
                    .forGetter(EncodedCraftingPattern::inputs),
            AECodecs.FAULT_TOLERANT_ITEMSTACK_CODEC.fieldOf("result").forGetter(EncodedCraftingPattern::result),
            AECodecs.RESOURCE_LOCATION.fieldOf("recipeId").forGetter(EncodedCraftingPattern::recipeId),
            Codec.BOOL.fieldOf("canSubstitute").forGetter(EncodedCraftingPattern::canSubstitute),
            Codec.BOOL.fieldOf("canSubstituteFluids").forGetter(EncodedCraftingPattern::canSubstituteFluids))
            .apply(builder, EncodedCraftingPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedCraftingPattern> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.ITEM_STACK_LIST,
                    EncodedCraftingPattern::inputs,
                    ByteBufCodecs.ITEM_STACK,
                    EncodedCraftingPattern::result,
                    ByteBufCodecs.RESOURCE_LOCATION,
                    EncodedCraftingPattern::recipeId,
                    ByteBufCodecs.BOOL,
                    EncodedCraftingPattern::canSubstitute,
                    ByteBufCodecs.BOOL,
                    EncodedCraftingPattern::canSubstituteFluids,
                    EncodedCraftingPattern::new);

    public boolean containsMissingContent() {
        return AEItems.MISSING_CONTENT.is(result) || inputs.stream().anyMatch(AEItems.MISSING_CONTENT::is);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;

        EncodedCraftingPattern that = (EncodedCraftingPattern) object;
        return canSubstitute == that.canSubstitute
                && canSubstituteFluids == that.canSubstituteFluids
                && ItemStack.areItemStacksEqual(result, that.result)
                && PatternStacks.matches(inputs, that.inputs)
                && recipeId.equals(that.recipeId);
    }

    @Override
    public int hashCode() {
        int result1 = PatternStacks.hash(inputs);
        result1 = 31 * result1 + PatternStacks.hash(result);
        result1 = 31 * result1 + recipeId.hashCode();
        result1 = 31 * result1 + Boolean.hashCode(canSubstitute);
        result1 = 31 * result1 + Boolean.hashCode(canSubstituteFluids);
        return result1;
    }
}
