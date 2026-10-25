/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.p2p;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import appeng.api.capabilities.Capabilities;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;

public class FluidP2PTunnelPart extends CapabilityP2PTunnelPart<FluidP2PTunnelPart, IFluidHandler> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fluids"));
    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public FluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, Capabilities.FluidHandler.BLOCK);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputFluidHandler implements IFluidHandler {

        @Override
        public IFluidTankProperties[] getTankProperties() {
            // One empty tank that takes anything
            return new IFluidTankProperties[] { new FluidTankProperties(null, Integer.MAX_VALUE, true, false) };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            int total = 0;

            final int outputTunnels = FluidP2PTunnelPart.this.getOutputs().size();
            final int amount = resource != null ? resource.amount : 0;

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final int amountPerOutput = amount / outputTunnels;
            int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (FluidP2PTunnelPart target : FluidP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IFluidHandler output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;
                    final FluidStack fillWithFluidStack = resource.copy();
                    fillWithFluidStack.amount = toSend;

                    final int received = output.fill(fillWithFluidStack, doFill);

                    overflow = toSend - received;
                    total += received;
                }
            }

            if (doFill) {
                deductTransportCost(total, AEKeyType.fluids());
            }

            return total;
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }

    }

    private class OutputFluidHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getTankProperties();
            }
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(resource, doDrain);

                if (doDrain && result != null) {
                    deductTransportCost(result.amount, AEKeyType.fluids());
                }

                return result;
            }
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(maxDrain, doDrain);

                if (doDrain && result != null) {
                    deductTransportCost(result.amount, AEKeyType.fluids());
                }

                return result;
            }
        }
    }

    private static class NullFluidHandler implements IFluidHandler {

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[0];
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }

}
