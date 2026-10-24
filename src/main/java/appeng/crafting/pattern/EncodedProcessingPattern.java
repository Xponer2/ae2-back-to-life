package appeng.crafting.pattern;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;

public final class EncodedProcessingPattern {
    private final List<GenericStack> sparseInputs;
    private final List<GenericStack> sparseOutputs;

    public List<GenericStack> sparseInputs() {
        return sparseInputs;
    }

    public List<GenericStack> sparseOutputs() {
        return sparseOutputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncodedProcessingPattern)) return false;
        EncodedProcessingPattern that = (EncodedProcessingPattern) o;
        return java.util.Objects.equals(sparseInputs, that.sparseInputs)
                && java.util.Objects.equals(sparseOutputs, that.sparseOutputs);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sparseInputs, sparseOutputs);
    }

    @Override
    public String toString() {
        return "EncodedProcessingPattern[" + "sparseInputs=" + sparseInputs + ", " + "sparseOutputs=" + sparseOutputs + "]";
    }

    public EncodedProcessingPattern(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs) {
        sparseInputs = Collections.unmodifiableList(sparseInputs);
        sparseOutputs = Collections.unmodifiableList(sparseOutputs);
        this.sparseInputs = sparseInputs;
        this.sparseOutputs = sparseOutputs;
    }

    public static final Codec<EncodedProcessingPattern> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("sparseInputs")
                    .forGetter(EncodedProcessingPattern::sparseInputs),
            GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("sparseOutputs")
                    .forGetter(EncodedProcessingPattern::sparseOutputs))
            .apply(builder, EncodedProcessingPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EncodedProcessingPattern> STREAM_CODEC = StreamCodec
            .composite(
                    GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    EncodedProcessingPattern::sparseInputs,
                    GenericStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    EncodedProcessingPattern::sparseOutputs,
                    EncodedProcessingPattern::new);

    public boolean containsMissingContent() {
        return Stream.concat(sparseInputs.stream(), sparseOutputs.stream())
                .anyMatch(stack -> stack != null && AEItems.MISSING_CONTENT.is(stack.what()));
    }
}
