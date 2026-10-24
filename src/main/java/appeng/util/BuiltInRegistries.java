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

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.IntFunction;

import com.mojang.serialization.Codec;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Facade with the same method names as Minecraft's BuiltInRegistries for the registries AE2 needs.
 */
public final class BuiltInRegistries {
    private BuiltInRegistries() {
    }

    public static final Registry<Item> ITEM = new Registry<>(
            id -> ForgeRegistries.ITEMS.getValue(id),
            Item::getRegistryName,
            Item::getIdFromItem,
            Item::getItemById,
            () -> ForgeRegistries.ITEMS.iterator(),
            AECodecs.ITEM,
            new ResourceLocation("minecraft:air"),
            Items.AIR);

    public static final Registry<Block> BLOCK = new Registry<>(
            id -> ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null,
            Block::getRegistryName,
            Block::getIdFromBlock,
            Block::getBlockById,
            () -> ForgeRegistries.BLOCKS.iterator(),
            AECodecs.BLOCK,
            new ResourceLocation("minecraft:air"),
            Blocks.AIR);

    public static final Registry<Fluid> FLUID = new Registry<>(
            id -> {
                Fluid f = FluidRegistry.getFluid(id.getPath());
                return f != null ? f : FluidRegistry.getFluid(id.toString());
            },
            f -> new ResourceLocation(Platform.getFluidId(f)),
            f -> FluidRegistry.getRegisteredFluidIDs().getOrDefault(f, -1),
            i -> {
                for (var e : FluidRegistry.getRegisteredFluidIDs().entrySet()) {
                    if (e.getValue() == i) {
                        return e.getKey();
                    }
                }
                return null;
            },
            () -> FluidRegistry.getRegisteredFluids().values().iterator(),
            AECodecs.FLUID,
            new ResourceLocation("minecraft:empty"),
            null);

    public static final class Registry<T> implements Iterable<T> {
        private final Function<ResourceLocation, T> byId;
        private final Function<T, ResourceLocation> toId;
        private final ToIntFunction<T> rawId;
        private final IntFunction<T> byRawId;
        private final java.util.function.Supplier<Iterator<T>> iterator;
        private final Codec<T> codec;
        private final ResourceLocation defaultKey;
        @Nullable
        private final T defaultValue;

        Registry(Function<ResourceLocation, T> byId, Function<T, ResourceLocation> toId, ToIntFunction<T> rawId,
                IntFunction<T> byRawId, java.util.function.Supplier<Iterator<T>> iterator, Codec<T> codec,
                ResourceLocation defaultKey, @Nullable T defaultValue) {
            this.byId = byId;
            this.toId = toId;
            this.rawId = rawId;
            this.byRawId = byRawId;
            this.iterator = iterator;
            this.codec = codec;
            this.defaultKey = defaultKey;
            this.defaultValue = defaultValue;
        }

        public ResourceLocation getKey(T value) {
            var id = toId.apply(value);
            return id != null ? id : defaultKey;
        }

        /**
         * Returns the default value (air for items/blocks) if the id is unknown, like vanilla.
         */
        public T get(ResourceLocation id) {
            T value = byId.apply(id);
            return value != null ? value : defaultValue;
        }

        public Optional<T> getOptional(ResourceLocation id) {
            T value = byId.apply(id);
            if (value == defaultValue && !defaultKey.equals(id)) {
                return Optional.empty();
            }
            return Optional.ofNullable(value);
        }

        public boolean containsKey(ResourceLocation id) {
            return getOptional(id).isPresent();
        }

        public int getId(T value) {
            return rawId.applyAsInt(value);
        }

        @Nullable
        public T byId(int id) {
            return byRawId.apply(id);
        }

        public Codec<T> byNameCodec() {
            return codec;
        }

        public Codec<T> holderByNameCodec() {
            return codec;
        }

        public ResourceLocation getDefaultKey() {
            return defaultKey;
        }

        @Override
        public Iterator<T> iterator() {
            return iterator.get();
        }

        public java.util.stream.Stream<T> stream() {
            return com.google.common.collect.Streams.stream(this);
        }

        public java.util.stream.Stream<T> filter(Predicate<T> predicate) {
            return stream().filter(predicate);
        }
    }
}
