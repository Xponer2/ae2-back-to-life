/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.recipes;

import java.util.function.Function;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.util.DataResults;

/**
 * Codec helpers of newer DataFixerUpper versions that the recipe codecs use.
 */
public final class RecipeCodecs {
    private RecipeCodecs() {
    }

    /**
     * A value written as a string (newer {@code Codec.stringResolver}).
     */
    public static <E> Codec<E> stringResolver(Function<E, String> toString, Function<String, E> fromString) {
        return Codec.STRING.comapFlatMap(s -> {
            var value = fromString.apply(s);
            return value != null ? DataResult.success(value) : DataResult.error("Unknown value: " + s);
        }, toString);
    }

    /**
     * Sends a value as the JSON its codec writes, for recipe parts that have no compact network form (recipes are not
     * synchronized in this version, so this is rarely used).
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> viaJson(Codec<T> codec) {
        return StreamCodec.of(
                (buffer, value) -> buffer.writeUtf(DataResults.getOrThrow(codec.encodeStart(JsonOps.INSTANCE, value))
                        .toString()),
                buffer -> DataResults.getOrThrow(codec.parse(JsonOps.INSTANCE,
                        new JsonParser().parse(buffer.readUtf()))));
    }
}
