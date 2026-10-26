/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.gui.style;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Reads enum constants by their name in any case. The screen files use the constant names ({@code CENTER},
 * {@code DEFAULT_TEXT_COLOR}), while the enum adapter of this game version only knows lower case names and turns
 * everything else into null (two palette entries then clash as null map keys). Unknown names still become null like in
 * Gson: text components of the files can have RGB colors ({@code "#575e54"}), which this version can't show, so they
 * keep the palette color.
 */
final class CaseInsensitiveEnumTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }
        Map<String, Object> byName = new HashMap<>();
        for (Object constant : rawType.getEnumConstants()) {
            byName.put(((Enum<?>) constant).name().toLowerCase(Locale.ROOT), constant);
        }
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(((Enum<?>) value).name());
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return (T) byName.get(in.nextString().toLowerCase(Locale.ROOT));
            }
        };
    }
}
