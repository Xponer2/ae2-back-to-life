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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Backport of Minecraft's DataComponentMap: an immutable set of component values (used to export machine settings to
 * memory cards and to items when a machine is picked up).
 */
public final class DataComponentMap {
    public static final DataComponentMap EMPTY = new DataComponentMap(Collections.emptyMap());

    private final Map<DataComponentType<?>, Object> values;

    private DataComponentMap(Map<DataComponentType<?>, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * All components of the given stack that are known AE2 data components.
     */
    public static DataComponentMap of(ItemStack stack) {
        return of(stack.getTagCompound());
    }

    public static DataComponentMap of(@Nullable NBTTagCompound tag) {
        if (tag == null) {
            return EMPTY;
        }
        var builder = builder();
        for (var type : DataComponentType.getAll()) {
            copy(type, tag, builder);
        }
        return builder.build();
    }

    private static <T> void copy(DataComponentType<T> type, NBTTagCompound tag, Builder builder) {
        var value = type.get(tag);
        if (value != null) {
            builder.set(type, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(DataComponentType<? extends T> type) {
        return (T) values.get(type);
    }

    public <T> T getOrDefault(DataComponentType<? extends T> type, T defaultValue) {
        T value = get(type);
        return value != null ? value : defaultValue;
    }

    public boolean has(DataComponentType<?> type) {
        return values.containsKey(type);
    }

    public Set<DataComponentType<?>> keySet() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    /**
     * Writes all values onto the given stack.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void applyTo(ItemStack stack) {
        for (var entry : values.entrySet()) {
            ((DataComponentType) entry.getKey()).set(stack, entry.getValue());
        }
    }

    public static final class Builder {
        private final Map<DataComponentType<?>, Object> values = new IdentityHashMap<>();

        public <T> Builder set(DataComponentType<T> type, @Nullable T value) {
            if (value == null) {
                values.remove(type);
            } else {
                values.put(type, value);
            }
            return this;
        }

        public Builder addAll(DataComponentMap map) {
            values.putAll(map.values);
            return this;
        }

        public DataComponentMap build() {
            return values.isEmpty() ? EMPTY : new DataComponentMap(new IdentityHashMap<>(values));
        }
    }
}
