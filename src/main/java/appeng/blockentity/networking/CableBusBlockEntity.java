/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.blockentity.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalBlockPos;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.client.render.model.ModelData;
import appeng.core.AppEng;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.helpers.AEMultiBlockEntity;
import appeng.parts.CableBusContainer;
import appeng.util.IDebugExportable;
import appeng.util.Platform;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.VoxelShape;

public class CableBusBlockEntity extends AEBaseBlockEntity implements AEMultiBlockEntity {

    private CableBusContainer cb = new CableBusContainer(this);

    private int oldLV = -1; // on re-calculate light when it changes

    public CableBusBlockEntity() {
        super();
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.getCableBus().readFromNBT(data);
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.getCableBus().writeToNBT(data);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        boolean ret = this.getCableBus().readFromStream(data);

        final int newLV = this.getCableBus().getLightValue();
        if (newLV != this.oldLV) {
            this.oldLV = newLV;
            this.world.checkLight(this.pos);
            ret = true;
        }

        this.updateBlockEntitySettings();
        return ret || c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        this.getCableBus().writeToStream(data);
    }

    /**
     * Changes this block entity to the TESR version if any of the parts require dynamic rendering.
     */
    protected void updateBlockEntitySettings() {
        // FIXME: potentially invalidate voxel shape cache?
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.getCableBus().removeFromWorld();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        scheduleInit(); // Required for onReady to be called
    }

    @Override
    public IGridNode getGridNode(EnumFacing dir) {
        return this.getCableBus().getGridNode(dir);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing side) {
        return this.getCableBus().getCableConnectionType(side);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return this.getCableBus().getCableConnectionLength(cable);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.getCableBus().removeFromWorld();
    }

    @Override
    public void markForUpdate() {
        if (this.world == null) {
            return;
        }

        final int newLV = this.getCableBus().getLightValue();
        if (newLV != this.oldLV) {
            this.oldLV = newLV;
            this.world.checkLight(this.pos);
        }

        super.markForUpdate();
    }

    @Override
    public void addAdditionalDrops(World level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        this.getCableBus().addAdditionalDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.getCableBus().clearContent();
    }

    @Override
    public void onReady() {
        super.onReady();
        if (this.getCableBus().isEmpty()) {
            if (this.world.getTileEntity(this.pos) == this) {
                this.world.destroyBlock(this.pos, true);
            }
        } else {
            this.getCableBus().addToWorld();
        }
    }

    @Override
    public IFacadeContainer getFacadeContainer() {
        return this.getCableBus().getFacadeContainer();
    }

    @Nullable
    @Override
    public IPart getPart(@Nullable EnumFacing side) {
        return this.cb.getPart(side);
    }

    @Override
    public boolean canAddPart(ItemStack is, EnumFacing side) {
        return this.getCableBus().canAddPart(is, side);
    }

    @Override
    @Nullable
    public <T extends IPart> T addPart(IPartItem<T> partItem, EnumFacing side,
            @Nullable EntityPlayer player) {
        return cb.addPart(partItem, side, player);
    }

    @Nullable
    @Override
    public <T extends IPart> T replacePart(IPartItem<T> partItem, @Nullable EnumFacing side,
            EntityPlayer owner, EnumHand hand) {
        return cb.replacePart(partItem, side, owner, hand);
    }

    @Override
    public void removePartFromSide(@Nullable EnumFacing side) {
        this.getCableBus().removePartFromSide(side);
    }

    @Override
    public boolean removePart(IPart part) {
        return this.getCableBus().removePart(part);
    }

    @Override
    public DimensionalBlockPos getLocation() {
        return new DimensionalBlockPos(this);
    }

    @Override
    public AEColor getColor() {
        return this.getCableBus().getColor();
    }

    @Override
    public void clearContainer() {
        this.setCableBus(new CableBusContainer(this));
    }

    @Override
    public boolean isBlocked(EnumFacing side) {
        // TODO 1.10.2-R - Stuff.
        return false;
    }

    @Override
    public SelectedPart selectPartLocal(Vec3d pos) {
        return this.getCableBus().selectPartLocal(pos);
    }

    @Override
    public void markForSave() {
        this.saveChanges();
    }

    @Override
    public void partChanged() {
        this.notifyNeighbors();
    }

    @Override
    public boolean hasRedstone() {
        return this.getCableBus().hasRedstone();
    }

    @Override
    public boolean isEmpty() {
        return this.getCableBus().isEmpty();
    }

    @Override
    public void cleanup() {
        this.getLevel().setBlockToAir(this.pos);
    }

    @Override
    public void notifyNeighbors() {
        if (this.world != null && this.world.isBlockLoaded(this.pos) && !CableBusContainer.isLoading()) {
            Platform.notifyBlocksOfNeighbors(this.world, this.pos);
        }
    }

    @Override
    public void notifyNeighborNow(EnumFacing side) {
        var targetPos = getPos().offset(side);
        if (this.world != null && this.world.isBlockLoaded(targetPos) && !CableBusContainer.isLoading()) {
            var targetState = this.world.getBlockState(targetPos);
            if (!Platform.isAir(targetState)) {
                targetState.neighborChanged(world, targetPos, getBlockType(), getPos());
            }
        }
    }

    @Override
    public boolean isInWorld() {
        return this.getCableBus().isInWorld();
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor colour, EntityPlayer who) {
        return this.getCableBus().recolourBlock(side, colour, who);
    }

    public CableBusContainer getCableBus() {
        return this.cb;
    }

    private void setCableBus(CableBusContainer cb) {
        this.cb = cb;
    }

    @Override
    public ModelData getModelData() {
        World level = getLevel();
        if (level == null) {
            return ModelData.EMPTY;
        }

        CableBusRenderState renderState = this.cb.getRenderState();
        renderState.setPos(pos);
        return ModelData.builder().with(CableBusRenderState.PROPERTY, renderState).build();

    }

    @Override
    public EnumActionResult disassembleWithWrench(EntityPlayer player, World level, RayTraceResult hitResult,
            ItemStack wrench) {

        if (!level.isRemote) {
            var is = new ArrayList<ItemStack>();
            final SelectedPart sp;

            AppEng.instance().setPartInteractionPlayer(player);
            try {
                sp = cb.selectPartWorld(hitResult.hitVec);
            } finally {
                AppEng.instance().setPartInteractionPlayer(null);
            }

            // SelectedPart contains either a facade or a part. Never both.
            if (sp.part != null) {
                sp.part.addPartDrop(is, true);
                sp.part.addAdditionalDrops(is, true);
                if (tileEntityInvalid) {
                    sp.part.clearContent();
                }

                // All facades will be dropped to the ground when the cable is removed,
                // do it manually here, so they are moved to the player inv too
                if (sp.side == null) {
                    var facades = getFacadeContainer();
                    for (var side : EnumFacing.VALUES) {
                        var facade = facades.getFacade(side);
                        if (facade != null) {
                            is.add(facade.getItemStack());
                            facades.removeFacade(cb, side);
                        }
                    }
                }

                cb.removePartFromSide(sp.side);
            } else if (sp.facade != null) {
                is.add(sp.facade.getItemStack());
                cb.getFacadeContainer().removeFacade(cb, sp.side);
                Platform.notifyBlocksOfNeighbors(level, getPos());
            }

            for (var item : is) {
                Platform.placeItemBackInInventory(player, item);
            }
        }

        return EnumActionResult.SUCCESS;

    }

    @Override
    public VoxelShape getCollisionShape(CollisionContext context) {
        return cb.getCollisionShape(context);
    }

    @Override
    public void debugExport(JsonWriter writer, Reference2IntMap<Object> machineIds,
            Reference2IntMap<IGridNode> nodeIds)
            throws IOException {
        super.debugExport(writer, machineIds, nodeIds);

        writer.name("parts");
        writer.beginObject();
        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var part = getPart(side);
            if (part != null) {
                writer.name(side == null ? "center" : side.getName());
                writer.beginObject();
                if (part instanceof IDebugExportable exportable) {
                    exportable.debugExport(writer, machineIds, nodeIds);
                }
                writer.endObject();
            }
        }
        writer.endObject();
    }
}
