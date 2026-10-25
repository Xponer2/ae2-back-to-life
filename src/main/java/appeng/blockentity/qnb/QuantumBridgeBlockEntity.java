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

package appeng.blockentity.qnb;

import appeng.util.Platform;

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import appeng.api.ids.AEComponents;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.block.qnb.QnbFormedState;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.client.render.model.ModelData;
import appeng.client.render.model.ModelProperty;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.QuantumCalculator;
import appeng.me.cluster.implementations.QuantumCluster;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

public class QuantumBridgeBlockEntity extends AENetworkedInvBlockEntity
        implements IAEMultiBlock<QuantumCluster>, ServerTickingBlockEntity {

    public static final ModelProperty<QnbFormedState> FORMED_STATE = new ModelProperty<>();

    private static int singularitySeed = 0;

    private final byte corner = 16;
    private final AppEngInternalInventory internalInventory = new AppEngInternalInventory(this, 1, 1);
    private final FilteredInternalInventory externalInventory = new FilteredInternalInventory(this.internalInventory,
            new EntangledSingularityFilter());
    private final byte hasSingularity = 32;
    private final byte powered = 64;

    private final QuantumCalculator calc = new QuantumCalculator(this);
    private byte constructed = -1;
    private QuantumCluster cluster;
    private boolean updateStatus = false;

    public QuantumBridgeBlockEntity() {
        super();
        this.getMainNode().setFlags(GridFlags.DENSE_CAPACITY);
        this.getMainNode().setIdlePowerUsage(22);
        onGridConnectableSidesChanged();
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        if (!isFormed()) {
            return EnumSet.noneOf(EnumFacing.class);
        }

        if (this.isCorner() || this.isCenter()) {
            return this.getAdjacentQuantumBridges();
        } else {
            return EnumSet.allOf(EnumFacing.class);
        }
    }

    @Override
    public void serverTick() {
        if (this.updateStatus) {
            this.updateStatus = false;
            if (this.cluster != null) {
                this.cluster.updateStatus(true);
            }
            this.markForUpdate();

            // big hack: markForUpdate will generally send a block update that will cause the cluster to initialize
            // correctly after it's been unloaded, however sometimes it doesn't so we manually rescan...
            neighborUpdate(getPos());
        }
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        int out = this.constructed;

        if (!this.internalInventory.getStackInSlot(0).isEmpty() && this.constructed != -1) {
            out |= this.hasSingularity;
        }

        if (this.getMainNode().isActive() && this.constructed != -1) {
            out |= this.powered;
        }

        data.writeByte((byte) out);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final int oldValue = this.constructed;
        this.constructed = data.readByte();
        return this.constructed != oldValue || c;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.internalInventory;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (this.cluster != null) {
            this.cluster.updateStatus(true);
        }
    }

    @Override
    public InternalInventory getExposedInventoryForSide(EnumFacing side) {
        if (this.isCenter()) {
            return this.externalInventory;
        }
        return InternalInventory.empty();
    }

    private boolean isCenter() {
        return Platform.is(getBlockState(), AEBlocks.QUANTUM_LINK.block());
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.updateStatus = true;
    }

    @Override
    public void onChunkUnloaded() {
        this.disconnect(false);
        super.onChunkUnloaded();
    }

    @Override
    public void onReady() {
        super.onReady();

        var quantumRing = AEBlocks.QUANTUM_RING;

        if (this.getBlockState().getBlock() == quantumRing.block()) {
            this.getMainNode().setVisualRepresentation(quantumRing.stack());
        }

        this.updateStatus = true;
    }

    @Override
    public void setRemoved() {
        this.disconnect(false);
        super.setRemoved();
    }

    @Override
    public void disconnect(boolean affectWorld) {
        if (this.cluster != null) {
            if (!affectWorld) {
                this.cluster.setUpdateStatus(false);
            }

            this.cluster.destroy();
        }

        this.cluster = null;

        if (affectWorld) {
            onGridConnectableSidesChanged();
        }
    }

    @Override
    public QuantumCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return !this.isRemoved();
    }

    public void updateStatus(QuantumCluster c, byte flags, boolean affectWorld) {
        this.cluster = c;

        if (affectWorld) {
            if (this.constructed != flags) {
                this.constructed = flags;
                this.markForUpdate();
            }

            onGridConnectableSidesChanged();
        }
    }

    public boolean isCorner() {
        return (this.constructed & this.getCorner()) == this.getCorner() && this.constructed != -1;
    }

    public EnumSet<EnumFacing> getAdjacentQuantumBridges() {
        var set = EnumSet.noneOf(EnumFacing.class);

        if (world != null) {
            for (var d : EnumFacing.VALUES) {
                var te = this.world.getTileEntity(this.pos.offset(d));
                if (te instanceof QuantumBridgeBlockEntity) {
                    set.add(d);
                }
            }
        }

        return set;
    }

    public long getQEFrequency() {
        var is = this.internalInventory.getStackInSlot(0);
        if (!is.isEmpty()) {
            return AEComponents.ENTANGLED_SINGULARITY_ID.getOrDefault(is, 0L);
        }
        return 0;
    }

    public boolean isPowered() {
        if (isClientSide()) {
            return (this.constructed & this.powered) == this.powered && this.constructed != -1;
        }

        var node = getMainNode().getNode();
        return node != null && node.isPowered();
    }

    public boolean isFormed() {
        return this.constructed != -1;
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.DENSE_SMART;
    }

    public void neighborUpdate(BlockPos fromPos) {
        if (world instanceof WorldServer serverLevel) {
            this.calc.updateMultiblockAfterNeighborUpdate(serverLevel, this.pos, fromPos);
        }
    }

    public boolean hasQES() {
        if (this.constructed == -1) {
            return false;
        }
        return (this.constructed & this.hasSingularity) == this.hasSingularity;
    }

    public void breakClusterOnRemove() {
        if (this.cluster != null) {
            // Prevents cluster.destroy() from changing the block state back to an unformed QNB,
            // because that would undo the removal.
            this.tileEntityInvalid = true;
            this.cluster.destroy();
            this.tileEntityInvalid = false;
        }
    }

    public byte getCorner() {
        return this.corner;
    }

    @Override
    public ModelData getModelData() {
        // FIXME must trigger model data updates

        return ModelData.builder()
                .with(FORMED_STATE, new QnbFormedState(getAdjacentQuantumBridges(), isCorner(), isPowered()))
                .build();

    }

    /**
     * Allows only valid {@link AEItems#QUANTUM_ENTANGLED_SINGULARITY} with a frequency.
     */
    private static class EntangledSingularityFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return isValidEntangledSingularity(stack);
        }
    }

    public static boolean isValidEntangledSingularity(ItemStack stack) {
        return AEComponents.ENTANGLED_SINGULARITY_ID.has(stack);
    }

    public static void assignFrequency(ItemStack stack) {
        var frequency = new Date().getTime() * 100 + singularitySeed++ % 100;
        AEComponents.ENTANGLED_SINGULARITY_ID.set(stack, frequency);
    }
}
