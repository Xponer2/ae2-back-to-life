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

package appeng.blockentity.spatial;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Iterators;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.client.render.model.ModelData;
import appeng.client.render.model.ModelProperty;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.SpatialPylonCalculator;
import appeng.me.cluster.implementations.SpatialPylonCluster;
import appeng.util.iterators.ChainedIterator;

public class SpatialPylonBlockEntity extends AENetworkedBlockEntity implements IAEMultiBlock<SpatialPylonCluster> {

    // The lower 6 bits are used
    public static final ModelProperty<ClientState> STATE = new ModelProperty<>(Objects::nonNull);

    private final SpatialPylonCalculator calc = new SpatialPylonCalculator(this);
    private SpatialPylonCluster cluster;
    private ClientState clientState = ClientState.DEFAULT;

    public SpatialPylonBlockEntity() {
        super();
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.MULTIBLOCK)
                .setIdlePowerUsage(0.5)
                .addService(IGridMultiblock.class, this::getMultiblockNodes);
    }

    @Override
    public void onChunkUnloaded() {
        this.disconnect(false);
        super.onChunkUnloaded();
    }

    @Override
    public void onReady() {
        super.onReady();
        if (world instanceof WorldServer serverLevel) {
            this.calc.calculateMultiblock(serverLevel, pos);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Ensure that this block entity is marked as removed before we try to dismantle the cluster, to prevent updates
        // to the block state.
        this.disconnect(false);
    }

    public void neighborChanged(BlockPos changedPos) {
        if (world instanceof WorldServer serverLevel) {
            this.calc.updateMultiblockAfterNeighborUpdate(serverLevel, pos, changedPos);
        }
    }

    @Override
    public void disconnect(boolean b) {
        if (this.cluster != null) {
            this.cluster.destroy();
            this.updateStatus(null);
        }
    }

    @Override
    public SpatialPylonCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void updateStatus(SpatialPylonCluster c) {
        if (this.isRemoved()) {
            // Prevent updating the display, the block state or the node if the block entity was removed.
            // Otherwise, setting the block state will restore the block we just destroyed.
            // Trying to update the node just causes a crash because the node has been removed by now.
            return;
        }

        this.cluster = c;
        onGridConnectableSidesChanged();
        this.recalculateDisplay();
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        return this.cluster == null ? EnumSet.noneOf(EnumFacing.class) : EnumSet.allOf(EnumFacing.class);
    }

    public void recalculateDisplay() {
        var pos = AxisPosition.NONE;
        var axis = EnumFacing.Axis.X;
        var powered = false;
        var online = false;
        if (this.cluster != null) {
            if (this.cluster.getBoundsMin().equals(this.pos)) {
                pos = AxisPosition.START;
            } else if (this.cluster.getBoundsMax().equals(this.pos)) {
                pos = AxisPosition.END;
            } else {
                pos = AxisPosition.MIDDLE;
            }

            axis = switch (this.cluster.getCurrentAxis()) {
                case X -> EnumFacing.Axis.X;
                case Y -> EnumFacing.Axis.Y;
                case Z -> EnumFacing.Axis.Z;
                default -> axis;
            };

            if (this.getMainNode().isPowered()) {
                powered = true;
            }

            if (this.cluster.isValid() && this.getMainNode().isOnline()) {
                online = true;
            }
        }

        var state = new ClientState(powered, online, pos, axis);
        if (!clientState.equals(state)) {
            boolean lightChanged = clientState.powered() != state.powered();
            this.clientState = state;
            this.markForUpdate();
            // The light level depends on POWERED_ON, which 1.12.2 doesn't re-check by itself
            if (lightChanged && world != null) {
                world.checkLight(this.pos);
            }
        }
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        var state = ClientState.readFromStream(data);
        if (!clientState.equals(state)) {
            boolean lightChanged = clientState.powered() != state.powered();
            clientState = state;
            if (lightChanged && world != null) {
                world.checkLight(this.pos);
            }
            return true;
        }
        return c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        clientState.writeToStream(data);
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        clientState.writeToNbt(data);
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        clientState = ClientState.readFromNbt(data);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.recalculateDisplay();
        }
    }

    public ClientState getClientState() {
        return clientState;
    }

    @Override
    public ModelData getModelData() {
        // FIXME: Must force model data update on changes, should potentially be moved
        // to block state (?)
        return ModelData.builder().with(STATE, getClientState()).build();
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.getCluster() == null) {
            return new ChainedIterator<>();
        }
        return Iterators.transform(this.getCluster().getBlockEntities(), SpatialPylonBlockEntity::getGridNode);
    }

    public final static class ClientState {
        private final boolean powered;
        private final boolean online;
        private final AxisPosition axisPosition;
        private final EnumFacing.Axis axis;

        public ClientState(boolean powered, boolean online, AxisPosition axisPosition, EnumFacing.Axis axis) {
            this.powered = powered;
            this.online = online;
            this.axisPosition = axisPosition;
            this.axis = axis;
        }

        public boolean powered() {
            return powered;
        }

        public boolean online() {
            return online;
        }

        public AxisPosition axisPosition() {
            return axisPosition;
        }

        public EnumFacing.Axis axis() {
            return axis;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClientState)) return false;
            ClientState that = (ClientState) o;
            return powered == that.powered
                    && online == that.online
                    && java.util.Objects.equals(axisPosition, that.axisPosition)
                    && java.util.Objects.equals(axis, that.axis);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(powered, online, axisPosition, axis);
        }

        @Override
        public String toString() {
            return "ClientState[" + "powered=" + powered + ", " + "online=" + online + ", " + "axisPosition=" + axisPosition + ", " + "axis=" + axis + "]";
        }

        public static final ClientState DEFAULT = new ClientState(false, false, AxisPosition.NONE, EnumFacing.Axis.X);

        public void writeToStream(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(powered);
            buf.writeBoolean(online);
            buf.writeEnum(axisPosition);
            buf.writeEnum(axis);
        }

        public static ClientState readFromStream(RegistryFriendlyByteBuf buf) {
            return new ClientState(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readEnum(AxisPosition.class),
                    buf.readEnum(EnumFacing.Axis.class));
        }

        public void writeToNbt(NBTTagCompound tag) {
            tag.setBoolean("powered", powered);
            tag.setBoolean("online", online);
            tag.setString("axisPosition", axisPosition.name());
            tag.setString("axis", axis.name());
        }

        public static ClientState readFromNbt(NBTTagCompound tag) {
            var powered = tag.getBoolean("powered");
            var online = tag.getBoolean("online");
            var axisPositionName = tag.getString("axisPosition");
            AxisPosition axisPosition;
            try {
                axisPosition = Enum.valueOf(AxisPosition.class, axisPositionName);
            } catch (IllegalArgumentException ignored) {
                axisPosition = DEFAULT.axisPosition;
            }
            var axisName = tag.getString("axis");
            EnumFacing.Axis axis;
            try {
                axis = Enum.valueOf(EnumFacing.Axis.class, axisName);
            } catch (IllegalArgumentException ignored) {
                axis = DEFAULT.axis;
            }
            return new ClientState(
                    powered,
                    online,
                    axisPosition,
                    axis);
        }
    }

    public enum AxisPosition {
        NONE,
        START,
        MIDDLE,
        END
    }
}
