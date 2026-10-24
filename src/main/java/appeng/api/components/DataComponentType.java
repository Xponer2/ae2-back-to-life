/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 * Copyright (c) 2026 the AE2: Back to Life contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.components;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.serialization.Codec;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import appeng.core.AELog;
import appeng.util.NbtOps;

/**
 * Backport of Minecraft 1.20.5 item data components. Values are persisted in the item stack NBT under the component
 * id (e.g. {@code ae2:stored_energy}), so equality of stacks/keys automatically includes the component values, just like
 * on modern versions.
 * <p/>
 * Decoded values are cached by identity of the backing NBT element, values must therefore be treated as immutable.
 */
public final class DataComponentType<T> {
    private static final Map<ResourceLocation, DataComponentType<?>> REGISTRY = new LinkedHashMap<>();

    @Nullable
    private ResourceLocation id;
    private String key;
    private final Codec<T> codec;
    private final Cache<NBTBase, Object> decodeCache = CacheBuilder.newBuilder().weakKeys().maximumSize(2048)
            .build();
    private static final Object FAILED = new Object();
    @Nullable
    private final VirtualAccess<T> virtual;

    private DataComponentType(Codec<T> codec) {
        this.codec = Objects.requireNonNull(codec, "persistent codec");
        this.virtual = null;
    }

    private DataComponentType(Codec<T> codec, VirtualAccess<T> virtual) {
        this.codec = Objects.requireNonNull(codec, "persistent codec");
        this.virtual = virtual;
    }

    /**
     * Components that map onto existing 1.12.2 stack NBT (e.g. the display name in {@code display.Name}).
     */
    public interface VirtualAccess<T> {
        @Nullable
        T get(NBTTagCompound tag);

        void set(NBTTagCompound tag, T value);

        void remove(NBTTagCompound tag);
    }

    public static <T> DataComponentType<T> virtual(Codec<T> codec, VirtualAccess<T> access) {
        return new DataComponentType<>(codec, access);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> DataComponentType<T> register(ResourceLocation id, DataComponentType<T> type) {
        if (type.id != null) {
            throw new IllegalStateException("Component " + type.id + " registered twice");
        }
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate component id " + id);
        }
        type.id = id;
        type.key = id.toString();
        REGISTRY.put(id, type);
        return type;
    }

    @Nullable
    public static DataComponentType<?> byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static Collection<DataComponentType<?>> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public ResourceLocation getId() {
        return Objects.requireNonNull(id, "unregistered component");
    }

    /**
     * The key used inside of the item stack NBT.
     */
    public String getKey() {
        return key;
    }

    public Codec<T> codec() {
        return codec;
    }

    @Nullable
    public T get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return get(stack.getTagCompound());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T get(@Nullable NBTTagCompound tag) {
        if (tag == null || key == null) {
            return null;
        }
        if (virtual != null) {
            return virtual.get(tag);
        }
        var element = tag.getTag(key);
        if (element == null) {
            return null;
        }
        try {
            Object result = decodeCache.get(element, () -> {
                var decoded = codec.parse(NbtOps.INSTANCE, element);
                var value = decoded.resultOrPartial(err -> AELog.warn("Failed to decode component %s: %s", key, err));
                return value.isPresent() ? value.get() : FAILED;
            });
            return result == FAILED ? null : (T) result;
        } catch (ExecutionException e) {
            AELog.warn("Failed to decode component %s: %s", key, e);
            return null;
        }
    }

    @Nullable
    public T get(appeng.api.stacks.AEKey key) {
        return key.get(this);
    }

    public T getOrDefault(ItemStack stack, T defaultValue) {
        var value = get(stack);
        return value != null ? value : defaultValue;
    }

    public T getOrDefault(@Nullable NBTTagCompound tag, T defaultValue) {
        var value = get(tag);
        return value != null ? value : defaultValue;
    }

    public boolean has(ItemStack stack) {
        return !stack.isEmpty() && has(stack.getTagCompound());
    }

    public boolean has(@Nullable NBTTagCompound tag) {
        if (virtual != null) {
            return tag != null && virtual.get(tag) != null;
        }
        return tag != null && key != null && tag.hasKey(key);
    }

    /**
     * Sets or (if value is null) removes this component on the stack.
     *
     * @return The previous value.
     */
    @Nullable
    public T set(ItemStack stack, @Nullable T value) {
        if (stack.isEmpty()) {
            return null;
        }
        var previous = get(stack);
        if (value == null) {
            remove(stack);
            return previous;
        }
        var tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        set(tag, value);
        return previous;
    }

    public void set(NBTTagCompound tag, T value) {
        if (virtual != null) {
            virtual.set(tag, value);
            return;
        }
        var encoded = codec.encodeStart(NbtOps.INSTANCE, value)
                .getOrThrow(false, err -> AELog.warn("Failed to encode component %s: %s", key, err));
        tag.setTag(key, encoded);
        // Seed the cache so a subsequent get returns the same instance
        decodeCache.put(encoded, value);
    }

    @Nullable
    public T remove(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var previous = get(stack);
        var tag = stack.getTagCompound();
        if (tag != null) {
            if (virtual != null) {
                virtual.remove(tag);
            } else {
                tag.removeTag(key);
            }
            if (tag.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
        return previous;
    }

    public T update(ItemStack stack, T defaultValue, UnaryOperator<T> updater) {
        var newValue = updater.apply(getOrDefault(stack, defaultValue));
        set(stack, newValue);
        return newValue;
    }

    /**
     * Copies this component from one stack to another.
     */
    public void copy(ItemStack from, ItemStack to) {
        if (virtual != null) {
            var value = get(from);
            if (value != null) {
                set(to, value);
            } else {
                remove(to);
            }
            return;
        }
        var tag = from.getTagCompound();
        if (tag != null && tag.hasKey(key)) {
            var toTag = to.getTagCompound();
            if (toTag == null) {
                toTag = new NBTTagCompound();
                to.setTagCompound(toTag);
            }
            toTag.setTag(key, tag.getTag(key).copy());
        } else {
            remove(to);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    public static final class Builder<T> {
        private Codec<T> codec;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Components are synced as part of the item NBT on 1.12.2, the stream codec is not needed.
         */
        public Builder<T> networkSynchronized(Object streamCodec) {
            return this;
        }

        public Builder<T> cacheEncoding() {
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(codec);
        }
    }
}
