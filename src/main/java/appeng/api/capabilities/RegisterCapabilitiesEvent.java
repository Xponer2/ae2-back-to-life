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

package appeng.api.capabilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import appeng.api.util.ItemLike;
import appeng.core.definitions.BlockEntityType;

/**
 * Backport of NeoForge's RegisterCapabilitiesEvent. On 1.12.2 this is a static registry that AE2's tile entities
 * ({@code AEBaseBlockEntity#getCapability}) and items ({@code initCapabilities}) consult.
 */
public final class RegisterCapabilitiesEvent {
    private static final RegisterCapabilitiesEvent INSTANCE = new RegisterCapabilitiesEvent();

    private final Map<Capability<?>, List<BlockEntityProvider<?, ?, ?>>> blockEntityProviders = new IdentityHashMap<>();
    private final Map<Capability<?>, Map<Item, ICapabilityProvider<ItemStack, ?, ?>>> itemProviders = new IdentityHashMap<>();
    private final Map<Block, Set<BlockCapability<?, ?>>> blockRegistrations = new HashMap<>();
    private final Set<Object> proxyable = new HashSet<>();
    // Cache: TE class -> capability -> providers
    private final Map<Class<?>, Map<Capability<?>, List<BlockEntityProvider<?, ?, ?>>>> lookupCache = new HashMap<>();

    private static final class BlockEntityProvider<T, C, BE extends TileEntity> {
        final BlockCapability<T, C> capability;
        final Class<BE> beClass;
        final ICapabilityProvider<? super BE, C, T> provider;

        BlockEntityProvider(BlockCapability<T, C> capability, Class<BE> beClass,
                ICapabilityProvider<? super BE, C, T> provider) {
            this.capability = capability;
            this.beClass = beClass;
            this.provider = provider;
        }
    }

    private RegisterCapabilitiesEvent() {
    }

    public static RegisterCapabilitiesEvent get() {
        return INSTANCE;
    }

    public void setProxyable(Object capability) {
        proxyable.add(capability);
    }

    public void setNonProxyable(Object capability) {
        proxyable.remove(capability);
    }

    public boolean isProxyable(Object capability) {
        return proxyable.contains(capability);
    }

    public <T, C, BE extends TileEntity> void registerBlockEntity(BlockCapability<T, C> capability,
            BlockEntityType<BE> type, ICapabilityProvider<? super BE, C, T> provider) {
        blockEntityProviders.computeIfAbsent(capability.forgeCapability(), c -> new ArrayList<>())
                .add(new BlockEntityProvider<>(capability, type.getBlockEntityClass(), provider));
        for (Block block : type.getValidBlocks()) {
            blockRegistrations.computeIfAbsent(block, b -> new HashSet<>()).add(capability);
        }
        lookupCache.clear();
    }

    public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider,
            ItemLike... items) {
        var map = itemProviders.computeIfAbsent(capability.forgeCapability(), c -> new IdentityHashMap<>());
        for (ItemLike item : items) {
            if (map.put(item.asItem(), provider) != null) {
                throw new IllegalStateException("Duplicate item capability " + capability + " for " + item.asItem());
            }
        }
    }

    public boolean isBlockRegistered(BlockCapability<?, ?> capability, Block block) {
        var caps = blockRegistrations.get(block);
        return caps != null && caps.contains(capability);
    }

    private List<BlockEntityProvider<?, ?, ?>> providersFor(TileEntity be, Capability<?> capability) {
        var perClass = lookupCache.computeIfAbsent(be.getClass(), c -> new IdentityHashMap<>());
        return perClass.computeIfAbsent(capability, cap -> {
            var all = blockEntityProviders.get(cap);
            if (all == null) {
                return java.util.Collections.emptyList();
            }
            var result = new ArrayList<BlockEntityProvider<?, ?, ?>>();
            for (var p : all) {
                if (p.beClass.isInstance(be)) {
                    result.add(p);
                }
            }
            return result;
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Nullable
    public <T> T getBlockEntityCapability(TileEntity be, Capability<T> capability, @Nullable EnumFacing side) {
        for (BlockEntityProvider p : providersFor(be, capability)) {
            Object context = p.capability.isSided() ? side : null;
            var result = p.provider.getCapability(be, context);
            if (result != null) {
                return (T) result;
            }
        }
        return null;
    }

    public boolean hasBlockEntityCapability(TileEntity be, Capability<?> capability, @Nullable EnumFacing side) {
        return getBlockEntityCapability(be, capability, side) != null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Nullable
    public <T> T getItemCapability(ItemStack stack, Capability<T> capability) {
        var map = itemProviders.get(capability);
        if (map == null) {
            return null;
        }
        ICapabilityProvider provider = map.get(stack.getItem());
        return provider != null ? (T) provider.getCapability(stack, null) : null;
    }

    public boolean hasItemCapabilities(Item item) {
        for (var map : itemProviders.values()) {
            if (map.containsKey(item)) {
                return true;
            }
        }
        return false;
    }
}
