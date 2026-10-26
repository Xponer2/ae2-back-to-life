/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;

import appeng.util.ResourceLocations;

/**
 * {@link PacketBuffer} with the method names used by Minecraft 1.20.5+ so that packet code ports 1:1.
 */
public class RegistryFriendlyByteBuf extends PacketBuffer {
    public RegistryFriendlyByteBuf(ByteBuf wrapped) {
        super(wrapped);
    }

    public static RegistryFriendlyByteBuf create() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer());
    }

    public static RegistryFriendlyByteBuf wrap(ByteBuf buf) {
        return buf instanceof RegistryFriendlyByteBuf r ? r : new RegistryFriendlyByteBuf(buf);
    }

    public String readUtf() {
        return readString(32767);
    }

    public String readUtf(int maxLength) {
        return readString(maxLength);
    }

    public RegistryFriendlyByteBuf writeUtf(String s) {
        writeString(s);
        return this;
    }

    /**
     * The 1.12.2 version builds the id with {@code new ResourceLocation(String)}, which doesn't split off a namespace
     * of one character ({@code c:...}).
     */
    @Override
    public ResourceLocation readResourceLocation() {
        return ResourceLocations.parse(readString(32767));
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return readEnumValue(enumClass);
    }

    public RegistryFriendlyByteBuf writeEnum(Enum<?> value) {
        writeEnumValue(value);
        return this;
    }

    public UUID readUUID() {
        return readUniqueId();
    }

    public RegistryFriendlyByteBuf writeUUID(UUID uuid) {
        writeUniqueId(uuid);
        return this;
    }

    @Nullable
    public NBTTagCompound readNbt() {
        try {
            return readCompoundTag();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public RegistryFriendlyByteBuf writeNbt(@Nullable NBTTagCompound tag) {
        writeCompoundTag(tag);
        return this;
    }

    public ItemStack readItem() {
        return readFullItemStack(this); // counterpart of writeItem
    }

    public RegistryFriendlyByteBuf writeItem(ItemStack stack) {
        writeFullItemStack(this, stack);
        return this;
    }

    /**
     * Unlike vanilla 1.12.2 {@link #writeItemStack}, this does not truncate the stack size to a byte and keeps the
     * full NBT (the vanilla variant uses the share tag).
     */
    public static void writeFullItemStack(PacketBuffer buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeShort(-1);
            return;
        }
        buf.writeShort(net.minecraft.item.Item.getIdFromItem(stack.getItem()));
        buf.writeVarInt(stack.getCount());
        buf.writeShort(stack.getMetadata());
        NBTTagCompound tag = null;
        if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
            tag = stack.getTagCompound();
        }
        // Capabilities NBT must survive the round-trip as well
        var full = stack.writeToNBT(new NBTTagCompound());
        if (full.hasKey("ForgeCaps")) {
            var wrapper = tag != null ? tag.copy() : new NBTTagCompound();
            wrapper.setTag("__ae2_caps", full.getTag("ForgeCaps"));
            tag = wrapper;
        }
        buf.writeCompoundTag(tag);
    }

    public static ItemStack readFullItemStack(PacketBuffer buf) {
        int id = buf.readShort();
        if (id < 0) {
            return ItemStack.EMPTY;
        }
        int count = buf.readVarInt();
        int meta = buf.readShort();
        NBTTagCompound tag;
        try {
            tag = buf.readCompoundTag();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        var item = net.minecraft.item.Item.getItemById(id);
        NBTTagCompound caps = null;
        if (tag != null && tag.hasKey("__ae2_caps")) {
            caps = tag.getCompoundTag("__ae2_caps");
            tag.removeTag("__ae2_caps");
            if (tag.isEmpty()) {
                tag = null;
            }
        }
        var stack = new ItemStack(item, count, meta, caps);
        stack.setTagCompound(tag);
        return stack;
    }

    public ItemStack readFullItem() {
        return readFullItemStack(this);
    }

    public FluidStack readFluidStack() {
        if (!readBoolean()) {
            return null;
        }
        return FluidStack.loadFluidStackFromNBT(readNbt());
    }

    public RegistryFriendlyByteBuf writeFluidStack(@Nullable FluidStack stack) {
        writeBoolean(stack != null);
        if (stack != null) {
            writeNbt(stack.writeToNBT(new NBTTagCompound()));
        }
        return this;
    }

    public ITextComponent readComponent() {
        return ITextComponent.Serializer.jsonToComponent(readUtf());
    }

    public RegistryFriendlyByteBuf writeComponent(ITextComponent component) {
        writeUtf(ITextComponent.Serializer.componentToJson(component));
        return this;
    }

    public <T> Optional<T> readOptional(Function<? super RegistryFriendlyByteBuf, T> reader) {
        return readBoolean() ? Optional.of(reader.apply(this)) : Optional.empty();
    }

    public <T> void writeOptional(Optional<T> value, BiConsumer<? super RegistryFriendlyByteBuf, T> writer) {
        if (value.isPresent()) {
            writeBoolean(true);
            writer.accept(this, value.get());
        } else {
            writeBoolean(false);
        }
    }

    @Nullable
    public <T> T readNullable(Function<? super RegistryFriendlyByteBuf, T> reader) {
        return readBoolean() ? reader.apply(this) : null;
    }

    public <T> void writeNullable(@Nullable T value, BiConsumer<? super RegistryFriendlyByteBuf, T> writer) {
        writeBoolean(value != null);
        if (value != null) {
            writer.accept(this, value);
        }
    }

    public <T> void writeCollection(Collection<T> collection, BiConsumer<? super RegistryFriendlyByteBuf, T> writer) {
        writeVarInt(collection.size());
        for (T t : collection) {
            writer.accept(this, t);
        }
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> factory,
            Function<? super RegistryFriendlyByteBuf, T> reader) {
        int size = readVarInt();
        C c = factory.apply(size);
        for (int i = 0; i < size; i++) {
            c.add(reader.apply(this));
        }
        return c;
    }

    public <T> List<T> readList(Function<? super RegistryFriendlyByteBuf, T> reader) {
        return readCollection(ArrayList::new, reader);
    }

    public <K, V> void writeMap(Map<K, V> map, BiConsumer<? super RegistryFriendlyByteBuf, K> keyWriter,
            BiConsumer<? super RegistryFriendlyByteBuf, V> valueWriter) {
        writeVarInt(map.size());
        map.forEach((k, v) -> {
            keyWriter.accept(this, k);
            valueWriter.accept(this, v);
        });
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> factory,
            Function<? super RegistryFriendlyByteBuf, K> keyReader,
            Function<? super RegistryFriendlyByteBuf, V> valueReader) {
        int size = readVarInt();
        M map = factory.apply(size);
        for (int i = 0; i < size; i++) {
            K k = keyReader.apply(this);
            V v = valueReader.apply(this);
            map.put(k, v);
        }
        return map;
    }

    public <K, V> Map<K, V> readMap(Function<? super RegistryFriendlyByteBuf, K> keyReader,
            Function<? super RegistryFriendlyByteBuf, V> valueReader) {
        return readMap(java.util.HashMap::new, keyReader, valueReader);
    }

    public <T> void writeById(java.util.function.ToIntFunction<T> idGetter, T value) {
        writeVarInt(idGetter.applyAsInt(value));
    }

    public <T> T readById(IntFunction<T> idLookup) {
        return idLookup.apply(readVarInt());
    }

    public long[] readLongArray() {
        return readLongArray(null);
    }

    /**
     * Same format as {@link PacketBuffer#writeLongArray}. The 1.12.2 version of this method only exists on the client.
     */
    @Override
    public long[] readLongArray(@Nullable long[] array) {
        int length = readVarInt();
        if (array == null || array.length != length) {
            if (length > readableBytes() / 8) {
                throw new DecoderException("LongArray with size " + length + " is bigger than allowed "
                        + readableBytes() / 8);
            }
            array = new long[length];
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = readLong();
        }
        return array;
    }

    public RegistryFriendlyByteBuf writeVec3(Vec3d vec) {
        writeDouble(vec.x);
        writeDouble(vec.y);
        writeDouble(vec.z);
        return this;
    }

    public Vec3d readVec3() {
        return new Vec3d(readDouble(), readDouble(), readDouble());
    }

    public RegistryFriendlyByteBuf writeBlockHitResult(RayTraceResult hit) {
        writeBlockPos(hit.getBlockPos());
        writeEnumValue(hit.sideHit);
        var vec = hit.hitVec;
        writeFloat((float) (vec.x - hit.getBlockPos().getX()));
        writeFloat((float) (vec.y - hit.getBlockPos().getY()));
        writeFloat((float) (vec.z - hit.getBlockPos().getZ()));
        return this;
    }

    public RayTraceResult readBlockHitResult() {
        BlockPos pos = readBlockPos();
        EnumFacing side = readEnumValue(EnumFacing.class);
        float x = readFloat(), y = readFloat(), z = readFloat();
        return new RayTraceResult(new Vec3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z), side, pos);
    }
}
