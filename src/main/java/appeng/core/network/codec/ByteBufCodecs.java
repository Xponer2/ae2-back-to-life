/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.network.codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import appeng.core.network.RegistryFriendlyByteBuf;

/**
 * Backport of the most commonly used Minecraft 1.20.5 ByteBufCodecs, plus replacements for codecs that 1.20.5 put
 * onto vanilla classes ({@code ItemStack.STREAM_CODEC} etc.).
 */
public final class ByteBufCodecs {
    private ByteBufCodecs() {
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean,
            ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((b, v) -> b.writeByte(v), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((b, v) -> b.writeShort(v),
            ByteBuf::readShort);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble,
            ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (b, v) -> new PacketBuffer(b).writeVarInt(v), b -> new PacketBuffer(b).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (b, v) -> new PacketBuffer(b).writeVarLong(v), b -> new PacketBuffer(b).readVarLong());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
            (b, v) -> new PacketBuffer(b).writeString(v), b -> new PacketBuffer(b).readString(32767));
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(
            (b, v) -> new PacketBuffer(b).writeByteArray(v), b -> new PacketBuffer(b).readByteArray());

    public static final StreamCodec<ByteBuf, NBTTagCompound> COMPOUND_TAG = StreamCodec.of(
            (b, v) -> new PacketBuffer(b).writeCompoundTag(v),
            b -> RegistryFriendlyByteBuf.wrap(b).readNbt());
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = STRING_UTF8
            .map(ResourceLocation::new, ResourceLocation::toString);
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
            (b, v) -> b.writeLong(v.toLong()), b -> BlockPos.fromLong(b.readLong()));
    public static final StreamCodec<ByteBuf, ChunkPos> CHUNK_POS = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.x);
                b.writeInt(v.z);
            }, b -> new ChunkPos(b.readInt(), b.readInt()));
    public static final StreamCodec<ByteBuf, ITextComponent> COMPONENT = STRING_UTF8
            .map(ITextComponent.Serializer::jsonToComponent, ITextComponent.Serializer::componentToJson);

    /**
     * Allows empty stacks, retains full stack size and NBT.
     */
    public static final StreamCodec<ByteBuf, ItemStack> OPTIONAL_ITEM_STACK = StreamCodec.of(
            (b, v) -> RegistryFriendlyByteBuf.writeFullItemStack(new PacketBuffer(b), v),
            b -> RegistryFriendlyByteBuf.readFullItemStack(new PacketBuffer(b)));
    public static final StreamCodec<ByteBuf, ItemStack> ITEM_STACK = OPTIONAL_ITEM_STACK;
    public static final StreamCodec<ByteBuf, List<ItemStack>> ITEM_STACK_LIST = OPTIONAL_ITEM_STACK
            .apply(list());

    public static final StreamCodec<ByteBuf, Item> ITEM = VAR_INT.map(Item::getItemById, Item::getIdFromItem);
    public static final StreamCodec<ByteBuf, Block> BLOCK = VAR_INT.map(Block::getBlockById, Block::getIdFromBlock);
    public static final StreamCodec<ByteBuf, Fluid> FLUID = STRING_UTF8.map(FluidRegistry::getFluid,
            FluidRegistry::getFluidName);
    public static final StreamCodec<ByteBuf, FluidStack> FLUID_STACK = StreamCodec.of(
            (b, v) -> RegistryFriendlyByteBuf.wrap(b).writeFluidStack(v),
            b -> RegistryFriendlyByteBuf.wrap(b).readFluidStack());

    public static <B extends ByteBuf, E extends Enum<E>> StreamCodec<B, E> enumCodec(Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        return VAR_INT.<E>map(i -> values[i], Enum::ordinal).cast();
    }

    public static <B extends ByteBuf, T> StreamCodec<B, T> idMapper(IntFunction<T> byId,
            java.util.function.ToIntFunction<T> toId) {
        return VAR_INT.<T>map(byId::apply, toId::applyAsInt).cast();
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return codec -> collection(ArrayList::new, codec, maxSize);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
            IntFunction<C> factory, StreamCodec<? super B, V> elementCodec) {
        return collection(factory, elementCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
            IntFunction<C> factory, StreamCodec<? super B, V> elementCodec, int maxSize) {
        return new StreamCodec<B, C>() {
            @Override
            public C decode(B buf) {
                int size = VAR_INT.decode(buf);
                if (size > maxSize) {
                    throw new IllegalStateException(size + " elements exceeded max size of: " + maxSize);
                }
                C c = factory.apply(Math.min(size, 65536));
                for (int i = 0; i < size; i++) {
                    c.add(elementCodec.decode(buf));
                }
                return c;
            }

            @Override
            public void encode(B buf, C value) {
                VAR_INT.encode(buf, value.size());
                for (V v : value) {
                    elementCodec.encode(buf, v);
                }
            }
        };
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(
            IntFunction<C> factory) {
        return codec -> collection(factory, codec);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory,
            StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory,
            StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize) {
        return new StreamCodec<B, M>() {
            @Override
            public M decode(B buf) {
                int size = VAR_INT.decode(buf);
                if (size > maxSize) {
                    throw new IllegalStateException(size + " elements exceeded max size of: " + maxSize);
                }
                M m = factory.apply(Math.min(size, 65536));
                for (int i = 0; i < size; i++) {
                    K k = keyCodec.decode(buf);
                    V v = valueCodec.decode(buf);
                    m.put(k, v);
                }
                return m;
            }

            @Override
            public void encode(B buf, M value) {
                VAR_INT.encode(buf, value.size());
                value.forEach((k, v) -> {
                    keyCodec.encode(buf, k);
                    valueCodec.encode(buf, v);
                });
            }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return new StreamCodec<B, Optional<V>>() {
            @Override
            public Optional<V> decode(B buf) {
                return buf.readBoolean() ? Optional.of(codec.decode(buf)) : Optional.empty();
            }

            @Override
            public void encode(B buf, Optional<V> value) {
                if (value.isPresent()) {
                    buf.writeBoolean(true);
                    codec.encode(buf, value.get());
                } else {
                    buf.writeBoolean(false);
                }
            }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, V> nullable(StreamCodec<B, V> codec) {
        return new StreamCodec<B, V>() {
            @Override
            public V decode(B buf) {
                return buf.readBoolean() ? codec.decode(buf) : null;
            }

            @Override
            public void encode(B buf, V value) {
                buf.writeBoolean(value != null);
                if (value != null) {
                    codec.encode(buf, value);
                }
            }
        };
    }
}
