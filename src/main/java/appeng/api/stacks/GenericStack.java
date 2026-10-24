package appeng.api.stacks;

import java.util.List;
import java.util.Objects;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.ids.AEComponents;
import appeng.core.definitions.AEItems;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.items.misc.WrappedGenericStack;
import appeng.util.DataResults;
import appeng.util.NbtOps;

/**
 * Represents some amount of some generic resource that AE can store or handle in crafting.
 */
public final class GenericStack {
    private final AEKey what;
    private final long amount;

    public AEKey what() {
        return what;
    }

    public long amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericStack)) return false;
        GenericStack that = (GenericStack) o;
        return java.util.Objects.equals(what, that.what)
                && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(what, amount);
    }

    @Override
    public String toString() {
        return "GenericStack[" + "what=" + what + ", " + "amount=" + amount + "]";
    }

    @ApiStatus.Internal
    public static final String AMOUNT_FIELD = "#";

    private static final Logger LOG = LogManager.getLogger(GenericStack.class);

    public static final Codec<GenericStack> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            AEKey.MAP_CODEC.forGetter(GenericStack::what),
            Codec.LONG.fieldOf(AMOUNT_FIELD).forGetter(GenericStack::amount)).apply(builder, GenericStack::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericStack> STREAM_CODEC = StreamCodec.ofMember(
            GenericStack::writeBuffer,
            GenericStack::readBuffer);

    /**
     * This result function converts failed serialization results for GenericStack into missing content storing the
     * error message.
     */
    private static final Codec.ResultFunction<GenericStack> MISSING_CONTENT_GENERICSTACK_RESULT = new Codec.ResultFunction<GenericStack>() {
        @Override
        public <T> DataResult<Pair<GenericStack, T>> apply(DynamicOps<T> ops, T input,
                DataResult<Pair<GenericStack, T>> a) {
            if (DataResults.isError(a)) {
                var missingContent = AEItems.MISSING_CONTENT.stack();
                var convert = Dynamic.convert(ops, NbtOps.INSTANCE, input);
                if (convert instanceof NBTTagCompound compoundTag) {
                    AEComponents.MISSING_CONTENT_ITEMSTACK_DATA.set(missingContent, compoundTag.copy());
                }
                LOG.error("Failed to deserialize GenericStack {}: {}", input, DataResults.message(a));
                AEComponents.MISSING_CONTENT_ERROR.set(missingContent, DataResults.message(a));

                var replacement = new GenericStack(AEItemKey.of(missingContent), 1);

                return DataResult.success(
                        Pair.of(replacement, input),
                        Lifecycle.stable());
            }

            // Return unchanged if deserialization succeeded
            return a;
        }

        @Override
        public <T> DataResult<T> coApply(DynamicOps<T> ops, GenericStack input, DataResult<T> t) {
            // When the serialization result failed, we write a missing content item instead
            // this one will NOT be recoverable
            if (DataResults.isError(t)) {
                var missingContent = AEItems.MISSING_CONTENT.stack();
                LOG.error("Failed to serialize GenericStack {}: {}", input, DataResults.message(t));
                AEComponents.MISSING_CONTENT_ERROR.set(missingContent, DataResults.message(t));

                var replacement = new GenericStack(AEItemKey.of(missingContent), 1);
                return CODEC.encodeStart(ops, replacement).setLifecycle(t.lifecycle());
            }

            // When the input is a MISSING_CONTENT item and has the original data attached,
            // we write that back.
            if (input.what() instanceof AEItemKey itemKey && itemKey.is(AEItems.MISSING_CONTENT)) {
                var originalData = AEComponents.MISSING_CONTENT_ITEMSTACK_DATA.get(itemKey);
                if (originalData != null) {
                    return DataResult.success(Dynamic.convert(NbtOps.INSTANCE, ops, originalData),
                            t.lifecycle());
                }
            }

            return t;
        }
    };

    public static final Codec<List<@Nullable GenericStack>> FAULT_TOLERANT_NULLABLE_LIST_CODEC = new GenericStackListCodec(
            CODEC.mapResult(MISSING_CONTENT_GENERICSTACK_RESULT));

    public static final Codec<List<GenericStack>> FAULT_TOLERANT_LIST_CODEC = CODEC
            .mapResult(MISSING_CONTENT_GENERICSTACK_RESULT).listOf();

    public GenericStack(AEKey what, long amount) {
        Objects.requireNonNull(what, "what");
        this.what = what;
        this.amount = amount;
    }

    @Nullable
    public static GenericStack readBuffer(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }

        var what = AEKey.readKey(buffer);
        if (what == null) {
            return null;
        }

        return new GenericStack(what, buffer.readVarLong());
    }

    public static void writeBuffer(@Nullable GenericStack stack, RegistryFriendlyByteBuf buffer) {
        if (stack == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);

            AEKey.writeKey(buffer, stack.what);
            buffer.writeVarLong(stack.amount);
        }
    }

    @Nullable
    public static GenericStack readTag(NBTTagCompound tag) {
        if (tag.isEmpty()) {
            return null;
        }
        var ops = NbtOps.INSTANCE;
        return GenericStack.CODEC.decode(ops, tag)
                .resultOrPartial(err -> LOG.error("Failed to decode GenericStack from {}: {}", tag, err))
                .map(Pair::getFirst)
                .orElse(null);
    }

    public static NBTTagCompound writeTag(@Nullable GenericStack stack) {
        if (stack == null) {
            return new NBTTagCompound();
        }

        var ops = NbtOps.INSTANCE;
        return (NBTTagCompound) DataResults.getOrThrow(GenericStack.CODEC.encodeStart(ops, stack));
    }

    /**
     * Converts a given item stack into a generic stack, accounting for a {@link GenericStack} already wrapped in an
     * {@link ItemStack}, unwrapping it automatically. If the item stack is empty, null is returned.
     */
    @Nullable
    public static GenericStack fromItemStack(ItemStack stack) {
        var genericStack = GenericStack.unwrapItemStack(stack);
        if (genericStack != null) {
            return genericStack;
        }

        var key = AEItemKey.of(stack);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, stack.getCount());
    }

    /**
     * Converts a given fluid stack into a generic stack. If the fluid stack is empty, null is returned.
     */
    @Nullable
    public static GenericStack fromFluidStack(FluidStack stack) {
        var key = AEFluidKey.of(stack);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, stack.amount);
    }

    public static long getStackSizeOrZero(@Nullable GenericStack stack) {
        return stack == null ? 0 : stack.amount;
    }

    public static ItemStack wrapInItemStack(@Nullable GenericStack stack) {
        if (stack != null) {
            return wrapInItemStack(stack.what(), stack.amount());
        } else {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack wrapInItemStack(AEKey what, long amount) {
        return WrappedGenericStack.wrap(what, amount);
    }

    public static boolean isWrapped(ItemStack stack) {
        return stack.getItem() instanceof WrappedGenericStack;
    }

    public static GenericStack unwrapItemStack(ItemStack stack) {
        // The isEmpty stack is needed because the item can match while its count is 0
        if (!stack.isEmpty() && stack.getItem() instanceof WrappedGenericStack item) {
            var what = item.unwrapWhat(stack);
            if (what != null) {
                var amount = item.unwrapAmount(stack);
                return new GenericStack(what, amount);
            }
        }

        return null;
    }

    public static GenericStack sum(GenericStack left, GenericStack right) {
        if (!left.what.equals(right.what)) {
            throw new IllegalArgumentException("Cannot sum generic stacks of " + left.what + " and " + right.what);
        }
        return new GenericStack(left.what, left.amount + right.amount);
    }

}
