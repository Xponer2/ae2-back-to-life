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

import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * NeoForge's standard capabilities mapped to the Forge 1.12.2 ones.
 */
public final class Capabilities {
    private Capabilities() {
    }

    public static final class ItemHandler {
        public static final BlockCapability<IItemHandler, EnumFacing> BLOCK = BlockCapability.forge(
                new ResourceLocation("forge", "item_handler"), IItemHandler.class,
                () -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        public static final ItemCapability<IItemHandler, Void> ITEM = ItemCapability.forge(
                new ResourceLocation("forge", "item_handler"), IItemHandler.class,
                () -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        public static final EntityCapability<IItemHandler> ENTITY = new EntityCapability<>(
                () -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

        private ItemHandler() {
        }
    }

    public static final class FluidHandler {
        public static final BlockCapability<IFluidHandler, EnumFacing> BLOCK = BlockCapability.forge(
                new ResourceLocation("forge", "fluid_handler"), IFluidHandler.class,
                () -> CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        public static final ItemCapability<IFluidHandlerItem, Void> ITEM = ItemCapability.forge(
                new ResourceLocation("forge", "fluid_handler_item"), IFluidHandlerItem.class,
                () -> CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);

        private FluidHandler() {
        }
    }

    public static final class EnergyStorage {
        public static final BlockCapability<IEnergyStorage, EnumFacing> BLOCK = BlockCapability.forge(
                new ResourceLocation("forge", "energy"), IEnergyStorage.class, () -> CapabilityEnergy.ENERGY);
        public static final ItemCapability<IEnergyStorage, Void> ITEM = ItemCapability.forge(
                new ResourceLocation("forge", "energy"), IEnergyStorage.class, () -> CapabilityEnergy.ENERGY);

        private EnergyStorage() {
        }
    }

    /**
     * Minimal entity capability (only used for the player inventory item handler).
     */
    public static final class EntityCapability<T> {
        private final java.util.function.Supplier<net.minecraftforge.common.capabilities.Capability<T>> cap;

        EntityCapability(java.util.function.Supplier<net.minecraftforge.common.capabilities.Capability<T>> cap) {
            this.cap = cap;
        }

        @org.jetbrains.annotations.Nullable
        public T getCapability(net.minecraft.entity.Entity entity) {
            var c = cap.get();
            return entity.hasCapability(c, null) ? entity.getCapability(c, null) : null;
        }
    }
}
