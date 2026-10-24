/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

/**
 * {@link DynamicOps} for Minecraft 1.12.2 NBT, modelled after the vanilla NbtOps of later versions.
 */
public final class NbtOps implements DynamicOps<NBTBase> {
    public static final NbtOps INSTANCE = new NbtOps();

    private static final int TAG_END = 0, TAG_BYTE = 1, TAG_SHORT = 2, TAG_INT = 3, TAG_LONG = 4, TAG_FLOAT = 5,
            TAG_DOUBLE = 6, TAG_BYTE_ARRAY = 7, TAG_STRING = 8, TAG_LIST = 9, TAG_COMPOUND = 10, TAG_INT_ARRAY = 11,
            TAG_LONG_ARRAY = 12;

    private NbtOps() {
    }

    @Override
    public NBTBase empty() {
        return new NBTTagEnd();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, NBTBase tag) {
        switch (tag.getId()) {
            case TAG_END:
                return outOps.empty();
            case TAG_BYTE:
                return outOps.createByte(((NBTPrimitive) tag).getByte());
            case TAG_SHORT:
                return outOps.createShort(((NBTPrimitive) tag).getShort());
            case TAG_INT:
                return outOps.createInt(((NBTPrimitive) tag).getInt());
            case TAG_LONG:
                return outOps.createLong(((NBTPrimitive) tag).getLong());
            case TAG_FLOAT:
                return outOps.createFloat(((NBTPrimitive) tag).getFloat());
            case TAG_DOUBLE:
                return outOps.createDouble(((NBTPrimitive) tag).getDouble());
            case TAG_BYTE_ARRAY:
                return outOps.createByteList(ByteBuffer.wrap(((NBTTagByteArray) tag).getByteArray()));
            case TAG_STRING:
                return outOps.createString(((NBTTagString) tag).getString());
            case TAG_LIST:
                return convertList(outOps, tag);
            case TAG_COMPOUND:
                return convertMap(outOps, tag);
            case TAG_INT_ARRAY:
                return outOps.createIntList(IntStream.of(((NBTTagIntArray) tag).getIntArray()));
            case TAG_LONG_ARRAY:
                return outOps.createLongList(LongStream.of(Platform.getLongArray((NBTTagLongArray) tag)));
            default:
                throw new IllegalStateException("Unknown tag type: " + tag);
        }
    }

    @Override
    public DataResult<Number> getNumberValue(NBTBase input) {
        if (input instanceof NBTPrimitive p) {
            switch (input.getId()) {
                case TAG_BYTE:
                    return DataResult.success(p.getByte());
                case TAG_SHORT:
                    return DataResult.success(p.getShort());
                case TAG_INT:
                    return DataResult.success(p.getInt());
                case TAG_LONG:
                    return DataResult.success(p.getLong());
                case TAG_FLOAT:
                    return DataResult.success(p.getFloat());
                default:
                    return DataResult.success(p.getDouble());
            }
        }
        return DataResult.error("Not a number: " + input);
    }

    @Override
    public NBTBase createNumeric(Number i) {
        return new NBTTagDouble(i.doubleValue());
    }

    @Override
    public NBTBase createByte(byte value) {
        return new NBTTagByte(value);
    }

    @Override
    public NBTBase createShort(short value) {
        return new NBTTagShort(value);
    }

    @Override
    public NBTBase createInt(int value) {
        return new NBTTagInt(value);
    }

    @Override
    public NBTBase createLong(long value) {
        return new NBTTagLong(value);
    }

    @Override
    public NBTBase createFloat(float value) {
        return new NBTTagFloat(value);
    }

    @Override
    public NBTBase createDouble(double value) {
        return new NBTTagDouble(value);
    }

    @Override
    public NBTBase createBoolean(boolean value) {
        return new NBTTagByte((byte) (value ? 1 : 0));
    }

    @Override
    public DataResult<String> getStringValue(NBTBase input) {
        return input instanceof NBTTagString s ? DataResult.success(s.getString())
                : DataResult.error("Not a string: " + input);
    }

    @Override
    public NBTBase createString(String value) {
        return new NBTTagString(value);
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, NBTBase value) {
        if (!(list instanceof NBTTagList) && !(list instanceof NBTTagEnd)) {
            return DataResult.error("mergeToList called with not a list: " + list, list);
        }
        var result = new NBTTagList();
        if (list instanceof NBTTagList l) {
            for (int i = 0; i < l.tagCount(); i++) {
                result.appendTag(l.get(i).copy());
            }
        }
        if (!result.isEmpty() && result.getTagType() != value.getId()) {
            return DataResult.error("Heterogeneous list: " + value, list);
        }
        result.appendTag(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, List<NBTBase> values) {
        DataResult<NBTBase> result = DataResult.success(list);
        for (NBTBase value : values) {
            result = result.flatMap(l -> mergeToList(l, value));
        }
        return result;
    }

    @Override
    public DataResult<NBTBase> mergeToMap(NBTBase map, NBTBase key, NBTBase value) {
        if (!(map instanceof NBTTagCompound) && !(map instanceof NBTTagEnd)) {
            return DataResult.error("mergeToMap called with not a map: " + map, map);
        }
        if (!(key instanceof NBTTagString keyStr)) {
            return DataResult.error("key is not a string: " + key, map);
        }
        var result = map instanceof NBTTagCompound c ? c.copy() : new NBTTagCompound();
        result.setTag(keyStr.getString(), value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<NBTBase> mergeToMap(NBTBase map, MapLike<NBTBase> values) {
        if (!(map instanceof NBTTagCompound) && !(map instanceof NBTTagEnd)) {
            return DataResult.error("mergeToMap called with not a map: " + map, map);
        }
        var result = map instanceof NBTTagCompound c ? c.copy() : new NBTTagCompound();
        List<NBTBase> missed = new ArrayList<>();
        values.entries().forEach(pair -> {
            if (pair.getFirst() instanceof NBTTagString keyStr) {
                result.setTag(keyStr.getString(), pair.getSecond());
            } else {
                missed.add(pair.getFirst());
            }
        });
        if (!missed.isEmpty()) {
            return DataResult.error("some keys are not strings: " + missed, result);
        }
        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<NBTBase, NBTBase>>> getMapValues(NBTBase input) {
        if (!(input instanceof NBTTagCompound tag)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(tag.getKeySet().stream()
                .map(key -> Pair.of(createString(key), tag.getTag(key))));
    }

    @Override
    public DataResult<Consumer<BiConsumer<NBTBase, NBTBase>>> getMapEntries(NBTBase input) {
        if (!(input instanceof NBTTagCompound tag)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(c -> {
            for (String key : tag.getKeySet()) {
                c.accept(createString(key), tag.getTag(key));
            }
        });
    }

    @Override
    public DataResult<MapLike<NBTBase>> getMap(NBTBase input) {
        if (!(input instanceof NBTTagCompound tag)) {
            return DataResult.error("Not a map: " + input);
        }
        return DataResult.success(new MapLike<NBTBase>() {
            @Override
            public NBTBase get(NBTBase key) {
                return tag.getTag(((NBTTagString) key).getString()) == null ? null
                        : tag.getTag(((NBTTagString) key).getString());
            }

            @Override
            public NBTBase get(String key) {
                return tag.hasKey(key) ? tag.getTag(key) : null;
            }

            @Override
            public Stream<Pair<NBTBase, NBTBase>> entries() {
                return tag.getKeySet().stream().map(key -> Pair.of(createString(key), tag.getTag(key)));
            }

            @Override
            public String toString() {
                return "MapLike[" + tag + "]";
            }
        });
    }

    @Override
    public NBTBase createMap(Stream<Pair<NBTBase, NBTBase>> map) {
        var tag = new NBTTagCompound();
        map.forEach(p -> tag.setTag(((NBTTagString) p.getFirst()).getString(), p.getSecond()));
        return tag;
    }

    @Override
    public NBTBase createMap(Map<NBTBase, NBTBase> map) {
        var tag = new NBTTagCompound();
        map.forEach((k, v) -> tag.setTag(((NBTTagString) k).getString(), v));
        return tag;
    }

    @Override
    public DataResult<Stream<NBTBase>> getStream(NBTBase input) {
        if (input instanceof NBTTagList list) {
            List<NBTBase> result = new ArrayList<>(list.tagCount());
            if (list.getTagType() == TAG_COMPOUND) {
                for (int i = 0; i < list.tagCount(); i++) {
                    result.add(unwrapListElement(list.get(i)));
                }
            } else {
                for (int i = 0; i < list.tagCount(); i++) {
                    result.add(list.get(i));
                }
            }
            return DataResult.success(result.stream());
        } else if (input instanceof NBTTagByteArray arr) {
            List<NBTBase> result = new ArrayList<>();
            for (byte b : arr.getByteArray()) {
                result.add(new NBTTagByte(b));
            }
            return DataResult.success(result.stream());
        } else if (input instanceof NBTTagIntArray arr) {
            return DataResult.success(IntStream.of(arr.getIntArray()).mapToObj(NBTTagInt::new));
        } else if (input instanceof NBTTagLongArray arr) {
            return DataResult.success(LongStream.of(Platform.getLongArray(arr)).mapToObj(NBTTagLong::new));
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<NBTBase>>> getList(NBTBase input) {
        return getStream(input).map(stream -> stream::forEach);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(NBTBase input) {
        if (input instanceof NBTTagByteArray arr) {
            return DataResult.success(ByteBuffer.wrap(arr.getByteArray()));
        }
        return DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public NBTBase createByteList(ByteBuffer input) {
        ByteBuffer buf = input.duplicate().clear();
        byte[] bytes = new byte[input.capacity()];
        buf.get(bytes, 0, bytes.length);
        return new NBTTagByteArray(bytes);
    }

    @Override
    public DataResult<IntStream> getIntStream(NBTBase input) {
        if (input instanceof NBTTagIntArray arr) {
            return DataResult.success(IntStream.of(arr.getIntArray()));
        }
        return DynamicOps.super.getIntStream(input);
    }

    @Override
    public NBTBase createIntList(IntStream input) {
        return new NBTTagIntArray(input.toArray());
    }

    @Override
    public DataResult<LongStream> getLongStream(NBTBase input) {
        if (input instanceof NBTTagLongArray arr) {
            return DataResult.success(LongStream.of(Platform.getLongArray(arr)));
        }
        return DynamicOps.super.getLongStream(input);
    }

    @Override
    public NBTBase createLongList(LongStream input) {
        return new NBTTagLongArray(input.toArray());
    }

    @Override
    public NBTBase createList(Stream<NBTBase> input) {
        List<NBTBase> elements = new ArrayList<>();
        input.forEach(elements::add);
        var list = new NBTTagList();
        if (elements.isEmpty()) {
            return list;
        }
        // NBT lists must be homogeneous; heterogeneous lists are wrapped into compounds (like 1.20.5+ does)
        int type = elements.get(0).getId();
        boolean homogeneous = true;
        for (NBTBase e : elements) {
            if (e.getId() != type) {
                homogeneous = false;
                break;
            }
        }
        if (homogeneous) {
            if (type == TAG_BYTE) {
                byte[] bytes = new byte[elements.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = ((NBTPrimitive) elements.get(i)).getByte();
                }
                return new NBTTagByteArray(bytes);
            } else if (type == TAG_INT) {
                int[] ints = new int[elements.size()];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = ((NBTPrimitive) elements.get(i)).getInt();
                }
                return new NBTTagIntArray(ints);
            } else if (type == TAG_LONG) {
                long[] longs = new long[elements.size()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = ((NBTPrimitive) elements.get(i)).getLong();
                }
                return new NBTTagLongArray(longs);
            }
            for (NBTBase e : elements) {
                list.appendTag(e);
            }
        } else {
            for (NBTBase e : elements) {
                list.appendTag(wrapListElement(e));
            }
        }
        return list;
    }

    private static NBTBase wrapListElement(NBTBase tag) {
        if (tag instanceof NBTTagCompound c && !isWrapper(c)) {
            return c;
        }
        var wrapper = new NBTTagCompound();
        wrapper.setTag("", tag);
        return wrapper;
    }

    private static NBTBase unwrapListElement(NBTBase tag) {
        if (tag instanceof NBTTagCompound c && isWrapper(c)) {
            return Objects.requireNonNull(c.getTag(""));
        }
        return tag;
    }

    private static boolean isWrapper(NBTTagCompound tag) {
        return tag.getSize() == 1 && tag.hasKey("");
    }

    @Override
    public NBTBase remove(NBTBase input, String key) {
        if (input instanceof NBTTagCompound tag) {
            var copy = tag.copy();
            copy.removeTag(key);
            return copy;
        }
        return input;
    }

    @Override
    public RecordBuilder<NBTBase> mapBuilder() {
        return new NbtRecordBuilder();
    }

    @Override
    public String toString() {
        return "NBT";
    }

    private final class NbtRecordBuilder extends RecordBuilder.AbstractStringBuilder<NBTBase, NBTTagCompound> {
        NbtRecordBuilder() {
            super(NbtOps.this);
        }

        @Override
        protected NBTTagCompound initBuilder() {
            return new NBTTagCompound();
        }

        @Override
        protected NBTTagCompound append(String key, NBTBase value, NBTTagCompound builder) {
            builder.setTag(key, value);
            return builder;
        }

        @Override
        protected DataResult<NBTBase> build(NBTTagCompound builder, NBTBase prefix) {
            if (prefix == null || prefix instanceof NBTTagEnd) {
                return DataResult.success(builder);
            } else if (prefix instanceof NBTTagCompound c) {
                var result = c.copy();
                for (String key : builder.getKeySet()) {
                    result.setTag(key, builder.getTag(key));
                }
                return DataResult.success(result);
            }
            return DataResult.error("mergeToMap called with not a map: " + prefix, prefix);
        }
    }
}
