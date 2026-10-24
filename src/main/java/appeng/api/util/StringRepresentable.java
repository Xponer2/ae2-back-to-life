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

package appeng.api.util;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.IStringSerializable;

/**
 * Backport of Minecraft's StringRepresentable. Extends 1.12.2's {@link IStringSerializable} so enums can be used as
 * block state property values.
 */
public interface StringRepresentable extends IStringSerializable {
    String getSerializedName();

    @Override
    default String getName() {
        return getSerializedName();
    }

    static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(Supplier<E[]> values) {
        E[] all = values.get();
        Map<String, E> byName = Arrays.stream(all)
                .collect(Collectors.toMap(StringRepresentable::getSerializedName, Function.identity()));
        return Codec.STRING.comapFlatMap(name -> {
            E value = byName.get(name);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown element name: " + name);
        }, StringRepresentable::getSerializedName);
    }

    static <E extends Enum<E> & StringRepresentable> Codec<E> fromValues(Supplier<E[]> values) {
        return fromEnum(values);
    }
}
