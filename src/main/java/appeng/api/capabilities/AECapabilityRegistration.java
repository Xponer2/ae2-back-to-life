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

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;

/**
 * Registers AE2's capability interfaces with Forge's {@link CapabilityManager}.
 */
public final class AECapabilityRegistration {
    @CapabilityInject(MEStorage.class)
    private static Capability<MEStorage> meStorage;
    @CapabilityInject(ICraftingMachine.class)
    private static Capability<ICraftingMachine> craftingMachine;
    @CapabilityInject(GenericInternalInventory.class)
    private static Capability<GenericInternalInventory> genericInternalInv;
    @CapabilityInject(IInWorldGridNodeHost.class)
    private static Capability<IInWorldGridNodeHost> inWorldGridNodeHost;
    @CapabilityInject(ICrankable.class)
    private static Capability<ICrankable> crankable;

    private static boolean registered;

    private AECapabilityRegistration() {
    }

    @ApiStatus.Internal
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        registerNoPersistence(MEStorage.class);
        registerNoPersistence(ICraftingMachine.class);
        registerNoPersistence(GenericInternalInventory.class);
        registerNoPersistence(IInWorldGridNodeHost.class);
        registerNoPersistence(ICrankable.class);
    }

    private static <T> void registerNoPersistence(Class<T> type) {
        CapabilityManager.INSTANCE.register(type, new Capability.IStorage<T>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {
            }
        }, () -> {
            throw new UnsupportedOperationException("No default implementation for " + type);
        });
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <T> Capability<T> get(Class<T> type) {
        Map<Class<?>, Capability<?>> map = new IdentityHashMap<>();
        map.put(MEStorage.class, meStorage);
        map.put(ICraftingMachine.class, craftingMachine);
        map.put(GenericInternalInventory.class, genericInternalInv);
        map.put(IInWorldGridNodeHost.class, inWorldGridNodeHost);
        map.put(ICrankable.class, crankable);
        return (Capability<T>) map.get(type);
    }
}
