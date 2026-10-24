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

package appeng.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

import appeng.api.config.YesNo;
import appeng.api.implementations.parts.ICablePart;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.parts.CableRenderMode;
import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalBlockPos;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.client.render.cablebus.CableCoreType;
import appeng.client.render.cablebus.FacadeRenderState;
import appeng.core.AELog;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.facade.FacadeContainer;
import appeng.helpers.AEMultiBlockEntity;
import appeng.hooks.VisualStateSaving;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.FacadeItem;
import appeng.me.InWorldGridNode;
import appeng.parts.networking.CablePart;
import appeng.util.Platform;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.VoxelShape;

public class CableBusContainer implements AEMultiBlockEntity, ICableBusContainer {

    /**
     * NBT property names used to store the parts for the given side. Index is 0-5 for normal directions and 6 for the
     * null-direction.
     */
    private static final String[] NBT_KEY_SIDES = Arrays.stream(Platform.DIRECTIONS_WITH_NULL)
            .map(d -> d == null ? "cable" : d.getName())
            .toArray(new String[0]);

    private static final ThreadLocal<Boolean> IS_LOADING = new ThreadLocal<>();
    private final CableBusStorage storage = new CableBusStorage();
    private YesNo hasRedstone = YesNo.UNDECIDED;
    private IPartHost tcb;
    private boolean requiresDynamicRender = false;
    private boolean inWorld = false;
    // Cached collision shape for living entities
    private VoxelShape cachedCollisionShapeLiving;
    // Cached collision shape for anything but living entities
    private VoxelShape cachedCollisionShape;
    private VoxelShape cachedShape;
    // For which cable render mode the cached shape was created
    private CableRenderMode cachedShapeCableRenderMode;

    public CableBusContainer(IPartHost host) {
        this.tcb = host;
    }

    public static boolean isLoading() {
        final Boolean is = IS_LOADING.get();
        return is != null && is;
    }

    public void setHost(IPartHost host) {
        this.tcb.clearContainer();
        this.tcb = host;
    }

    @Override
    public IFacadeContainer getFacadeContainer() {
        return new FacadeContainer(this.storage, this::facadeChanged);
    }

    private void facadeChanged(EnumFacing side) {
        invalidateShapes();
        updateNeighborShapeOnSide(side);
    }

    private ICablePart getCable() {
        return this.storage.getCenter();
    }

    @Nullable
    @Override
    public IPart getPart(@Nullable EnumFacing partLocation) {
        if (partLocation == null) {
            return this.storage.getCenter();
        }
        return this.storage.getPart(partLocation);
    }

    @Override
    public boolean canAddPart(ItemStack is, EnumFacing side) {
        if (FacadeItem.createFacade(is, side) != null) {
            return true;
        }

        if (is.getItem() instanceof IPartItem<?> partItem) {
            var part = partItem.createPart();
            if (part == null) {
                return false;
            }

            if (part instanceof ICablePart cablePart) {
                // Cables can be added if there's currently no cable, and existing parts work with the new cable
                return getCable() == null && arePartsCompatibleWithCable(cablePart);
            } else if (side != null) {
                // Parts can be added if the side is free, and they work with the existing cable (if any)
                return getPart(side) == null && isPartCompatibleWithCable(part, getCable());
            }
        }

        return false;
    }

    @Override
    @Nullable
    public <T extends IPart> T addPart(IPartItem<T> partItem, EnumFacing side, @Nullable EntityPlayer player) {
        // This code-path does not allow adding facades, while canAddPart allows facades.

        var part = partItem.createPart();

        if (part == null) {
            return null;
        }

        if (part instanceof ICablePart cablePart) {
            if (getCable() != null || !arePartsCompatibleWithCable(cablePart)) {
                return null;
            }

            this.storage.setCenter(cablePart);
            cablePart.setPartHostInfo(null, this, this.tcb.getTileEntity());

            if (player != null) {
                cablePart.onPlacement(player);
            }

            if (this.inWorld) {
                // Ensure the exposed sides are set correctly before connecting it to nodes around
                updateConnections();
                cablePart.addToWorld();
            }

            // Connect the cables grid node to all existing internal grid nodes on the host
            var cableNode = cablePart.getGridNode();
            if (cableNode != null) {
                for (var partSide : EnumFacing.VALUES) {
                    var existingPart = this.getPart(partSide);
                    if (existingPart != null) {
                        var existingPartNode = existingPart.getGridNode();
                        if (existingPartNode != null) {
                            GridHelper.createConnection(cableNode, existingPartNode);
                        }
                    }
                }
            }
        } else if (side != null) {
            var cable = getCable();
            if (!isPartCompatibleWithCable(part, cable)) {
                return null;
            }

            this.storage.setPart(side, part);
            part.setPartHostInfo(side, this, this.getTileEntity());

            if (player != null) {
                part.onPlacement(player);
            }

            if (this.inWorld) {
                part.addToWorld();
            }

            // Connect the parts grid node to the existing cables grid node
            if (cable != null) {
                var cableNode = cable.getGridNode();
                var partNode = part.getGridNode();

                if (cableNode != null && partNode != null) {
                    GridHelper.createConnection(cableNode, partNode);
                }
            }
        }

        updateAfterPartChange(side);

        return part;
    }

    // Check that the current attached parts are compatible with the given cable part
    private boolean arePartsCompatibleWithCable(ICablePart cable) {
        for (var d : EnumFacing.VALUES) {
            var part = getPart(d);
            if (part != null && !isPartCompatibleWithCable(part, cable)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPartCompatibleWithCable(IPart part, @Nullable ICablePart cable) {
        return cable == null || part.canBePlacedOn(cable.supportsBuses());
    }

    @Override
    public <T extends IPart> T replacePart(IPartItem<T> partItem, @Nullable EnumFacing side, EntityPlayer owner,
            EnumHand hand) {
        this.removePartWithoutUpdates(side);
        return this.addPart(partItem, side, owner);
    }

    @Override
    public void removePartFromSide(@Nullable EnumFacing side) {
        this.removePartWithoutUpdates(side);

        updateAfterPartChange(side);

        // Cleanup the cable bus once it is no longer containing any parts.
        // Also only when the cable bus actually exists, otherwise it might perform a cleanup during initialization.
        if (this.isInWorld() && this.isEmpty()) {
            this.cleanup();
        }
    }

    @Override
    public boolean removePart(IPart part) {
        if (getPart(null) == part) {
            removePartFromSide(null);
            return true;
        }
        for (var side : EnumFacing.VALUES) {
            if (getPart(side) == part) {
                removePartFromSide(side);
                return true;
            }
        }
        return false;
    }

    private void updateAfterPartChange(EnumFacing side) {
        this.invalidateShapes();
        this.updateDynamicRender();
        this.updateConnections();
        this.markForUpdate();
        this.markForSave();
        this.partChanged();
        getTileEntity().invalidateCapabilities();

        updateNeighborShapeOnSide(side);
    }

    private void removePartWithoutUpdates(@Nullable EnumFacing side) {
        if (side == null) {
            if (this.storage.getCenter() != null) {
                this.storage.getCenter().removeFromWorld();
            }
            this.storage.setCenter(null);
        } else {
            if (this.getPart(side) != null) {
                this.getPart(side).removeFromWorld();
            }
            this.storage.removePart(side);
        }
    }

    @Override
    public void markForUpdate() {
        this.tcb.markForUpdate();
    }

    @Override
    public DimensionalBlockPos getLocation() {
        return this.tcb.getLocation();
    }

    @Override
    public TileEntity getTileEntity() {
        return this.tcb.getTileEntity();
    }

    @Override
    public AEColor getColor() {
        if (this.storage.getCenter() != null) {
            final ICablePart c = this.storage.getCenter();
            return c.getCableColor();
        }
        return AEColor.TRANSPARENT;
    }

    @Override
    public void clearContainer() {
        throw new UnsupportedOperationException("Now that is silly!");
    }

    @Override
    public boolean isBlocked(EnumFacing side) {
        return this.tcb.isBlocked(side);
    }

    @Override
    public SelectedPart selectPartLocal(Vec3d pos) {
        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var p = this.getPart(side);
            if (p != null) {
                var boxes = new ArrayList<AxisAlignedBB>();

                var bch = new BusCollisionHelper(boxes, side, true);
                p.getBoxes(bch);
                for (AxisAlignedBB bb : boxes) {
                    bb = bb.grow(0.002, 0.002, 0.002);
                    if (bb.contains(pos)) {
                        return new SelectedPart(p, side);
                    }
                }
            }
        }

        if (PartHelper.getCableRenderMode().opaqueFacades) {
            var fc = this.getFacadeContainer();
            for (var side : EnumFacing.VALUES) {
                var p = fc.getFacade(side);
                if (p != null) {
                    var boxes = new ArrayList<AxisAlignedBB>();

                    var bch = new BusCollisionHelper(boxes, side, true);
                    p.getBoxes(bch, true);
                    for (AxisAlignedBB bb : boxes) {
                        bb = bb.grow(0.01, 0.01, 0.01);
                        if (bb.contains(pos)) {
                            return new SelectedPart(p, side);
                        }
                    }
                }
            }
        }

        return new SelectedPart();
    }

    @Override
    public void markForSave() {
        this.tcb.markForSave();
    }

    @Override
    public void partChanged() {
        // Drop all facades if no center to attach them to exists anymore
        if (this.storage.getCenter() == null) {
            var facades = new ArrayList<ItemStack>();

            var fc = this.getFacadeContainer();
            for (EnumFacing d : EnumFacing.VALUES) {
                final IFacadePart fp = fc.getFacade(d);
                if (fp != null) {
                    facades.add(fp.getItemStack());
                    fc.removeFacade(this.tcb, d);
                }
            }

            if (!facades.isEmpty()) {
                var te = this.tcb.getTileEntity();
                Platform.spawnDrops(te.getWorld(), te.getPos(), facades);
            }
        }

        // Update the exposed sides of exposed nodes
        for (var direction : EnumFacing.VALUES) {
            var part = getPart(direction);
            if (part != null) {
                if (part.getExternalFacingNode() instanceof InWorldGridNode inWorldNode) {
                    inWorldNode.setExposedOnSides(EnumSet.of(direction));
                }
            }
        }

        this.tcb.partChanged();
    }

    @Override
    public boolean hasRedstone() {
        if (this.hasRedstone == YesNo.UNDECIDED) {
            this.updateRedstone();
        }

        return this.hasRedstone == YesNo.YES;
    }

    @Override
    public boolean isEmpty() {
        var fc = this.getFacadeContainer();
        for (var s : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(s);
            if (part != null) {
                return false;
            }

            if (s != null) {
                var fp = fc.getFacade(s);
                if (fp != null) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void cleanup() {
        this.tcb.cleanup();
    }

    @Override
    public void notifyNeighbors() {
        this.tcb.notifyNeighbors();
    }

    @Override
    public void notifyNeighborNow(EnumFacing side) {
        this.tcb.notifyNeighborNow(side);
    }

    @Override
    public boolean isInWorld() {
        return this.inWorld;
    }

    private void updateRedstone() {
        var te = this.getTileEntity();
        this.hasRedstone = te.getWorld().isBlockPowered(te.getPos()) ? YesNo.YES : YesNo.NO;
    }

    private void updateDynamicRender() {
        this.requiresDynamicRender = false;
        for (EnumFacing s : EnumFacing.VALUES) {
            final IPart p = this.getPart(s);
            if (p != null) {
                this.setRequiresDynamicRender(this.isRequiresDynamicRender() || p.requireDynamicRender());
            }
        }
    }

    public void updateConnections() {
        var center = this.storage.getCenter();
        if (center != null) {
            var sides = EnumSet.allOf(EnumFacing.class);

            for (var s : EnumFacing.VALUES) {
                if (this.getPart(s) != null || this.isBlocked(s)) {
                    sides.remove(s);
                }
            }

            center.setExposedOnSides(sides);
        }
    }

    public void addToWorld() {
        if (this.inWorld) {
            return;
        }

        this.inWorld = true;
        IS_LOADING.set(true);

        final TileEntity te = this.getTileEntity();

        // start with the center, then install the side parts into the grid.
        for (int x = 6; x >= 0; x--) {
            final EnumFacing s = Platform.DIRECTIONS_WITH_NULL[x];
            final IPart part = this.getPart(s);

            if (part != null) {
                part.setPartHostInfo(s, this, te);
                part.addToWorld();

                if (s != null) {
                    final IGridNode sn = part.getGridNode();
                    if (sn != null) {
                        // this is a really stupid if statement, why was this
                        // here?
                        // if ( !sn.getConnections().iterator().hasNext() )

                        final IPart center = this.getPart(null);
                        if (center != null) {
                            final IGridNode cn = center.getGridNode();
                            if (cn != null) {
                                GridHelper.createConnection(cn, sn);
                            }
                        }
                    }
                }
            }
        }

        this.partChanged();

        IS_LOADING.set(false);
    }

    public void removeFromWorld() {
        if (!this.inWorld) {
            return;
        }

        this.inWorld = false;

        for (EnumFacing s : Platform.DIRECTIONS_WITH_NULL) {
            final IPart part = this.getPart(s);
            if (part != null) {
                part.removeFromWorld();
            }
        }

        this.invalidateShapes();
        this.partChanged();
    }

    @Override
    public IGridNode getGridNode(EnumFacing side) {
        final IPart part = this.getPart(side);
        if (part != null) {
            final IGridNode n = part.getExternalFacingNode();
            if (n != null) {
                return n;
            }
        }

        if (this.storage.getCenter() != null) {
            return this.storage.getCenter().getGridNode();
        }

        return null;
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        final IPart part = this.getPart(dir);

        if (part != null) {
            return part.getExternalCableConnectionType();
        }

        if (this.storage.getCenter() != null) {
            final ICablePart c = this.storage.getCenter();
            return c.getCableConnectionType();
        }
        return AECableType.NONE;
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return this.getPart(null) instanceof ICablePart
                ? this.getPart(null).getCableConnectionLength(cable)
                : -1;
    }

    @Override
    public int isProvidingStrongPower(EnumFacing side) {
        final IPart part = this.getPart(side);
        return part != null ? part.isProvidingStrongPower() : 0;
    }

    @Override
    public int isProvidingWeakPower(EnumFacing side) {
        final IPart part = this.getPart(side);
        return part != null ? part.isProvidingWeakPower() : 0;
    }

    @Override
    public boolean canConnectRedstone(EnumFacing opposite) {
        final IPart part = this.getPart(opposite);
        return part != null && part.canConnectRedstone();
    }

    @Override
    public void onEntityCollision(Entity entity) {
        for (EnumFacing s : Platform.DIRECTIONS_WITH_NULL) {
            final IPart part = this.getPart(s);
            if (part != null) {
                part.onEntityCollision(entity);
            }
        }
    }

    @Override
    public boolean useItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d localPos) {
        final SelectedPart p = this.selectPartLocal(localPos);
        if (p != null && p.part != null) {
            return p.part.onUseItemOn(heldItem, player, hand, localPos);
        } else if (p != null && p.facade != null) {
            if (p.facade.onUseItemOn(heldItem, player, hand, localPos)) {
                // facade changed state
                markForSave();
                markForUpdate();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean useWithoutItem(EntityPlayer player, Vec3d localPos) {
        final SelectedPart p = this.selectPartLocal(localPos);
        if (p != null && p.part != null) {
            return p.part.onUseWithoutItem(player, localPos);
        }
        return false;
    }

    @Override
    public void onNeighborChanged(IBlockAccess level, BlockPos pos, BlockPos neighbor) {
        this.hasRedstone = YesNo.UNDECIDED;

        for (var s : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(s);
            if (part != null) {
                part.onNeighborChanged(level, pos, neighbor);
            }
        }
    }

    @Override
    public void onUpdateShape(World level, BlockPos pos, EnumFacing side) {
        for (var s : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(s);
            if (part != null) {
                part.onUpdateShape(side);
            }
        }

        // Some parts will change their shape (connected texture style)
        invalidateShapes();
    }

    @Override
    public boolean isLadder(EntityLivingBase entity) {
        for (EnumFacing side : Platform.DIRECTIONS_WITH_NULL) {
            final IPart p = this.getPart(side);
            if (p != null && p.isLadder(entity)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void animateTick(World level, BlockPos pos, Random r) {
        for (EnumFacing side : Platform.DIRECTIONS_WITH_NULL) {
            final IPart p = this.getPart(side);
            if (p != null) {
                p.animateTick(level, pos, r);
            }
        }
    }

    @Override
    public int getLightValue() {
        int light = 0;

        for (EnumFacing d : Platform.DIRECTIONS_WITH_NULL) {
            final IPart p = this.getPart(d);
            if (p != null) {
                light = Math.max(p.getLightLevel(), light);
            }
        }

        return light;
    }

    public void writeToStream(RegistryFriendlyByteBuf data) {
        int sides = 0;
        for (int x = 0; x < Platform.DIRECTIONS_WITH_NULL.length; x++) {
            var p = this.getPart(Platform.DIRECTIONS_WITH_NULL[x]);
            if (p != null) {
                sides |= 1 << x;
            }
        }

        data.writeByte((byte) sides);

        for (int x = 0; x < Platform.DIRECTIONS_WITH_NULL.length; x++) {
            var p = this.getPart(Platform.DIRECTIONS_WITH_NULL[x]);
            if (p != null) {
                data.writeVarInt(IPartItem.getNetworkId(p.getPartItem()));

                p.writeToStream(data);
            }
        }

        this.getFacadeContainer().writeToStream(data);
    }

    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        final byte sides = data.readByte();

        boolean updateBlock = false;

        for (int x = 0; x < Platform.DIRECTIONS_WITH_NULL.length; x++) {
            EnumFacing side = Platform.DIRECTIONS_WITH_NULL[x];
            if ((sides & 1 << x) == 1 << x) {
                IPart p = this.getPart(side);

                var itemId = data.readVarInt();
                var partItem = IPartItem.byNetworkId(itemId);

                if (p != null && p.getPartItem() == partItem) {
                    if (p.readFromStream(data)) {
                        updateBlock = true;
                    }
                } else if (partItem != null) {
                    this.removePartFromSide(side);
                    p = this.addPart(partItem, side, null);
                    if (p != null) {
                        p.readFromStream(data);
                    } else {
                        throw new IllegalStateException("Invalid Stream For CableBus Container.");
                    }
                } else {
                    throw new IllegalStateException("Invalid item from server for part: " + itemId);
                }
            } else if (this.getPart(side) != null) {
                this.removePartFromSide(side);
            }
        }

        updateBlock |= this.getFacadeContainer().readFromStream(data);

        // Updating block entities may change the collision shape
        this.invalidateShapes();

        return updateBlock;
    }

    private static int getSideIndex(@Nullable EnumFacing side) {
        return side == null ? 6 : side.ordinal();
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setInteger("hasRedstone", this.hasRedstone.ordinal());

        getFacadeContainer().writeToNBT(data);

        var saveVisualState = VisualStateSaving.isEnabled(getTileEntity().getWorld());

        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(side);
            if (part != null) {
                var partData = new NBTTagCompound();

                // Save visual state of the part if requested
                if (saveVisualState) {
                    var visualTag = new NBTTagCompound();
                    part.writeVisualStateToNBT(visualTag);
                    partData.setTag("visual", visualTag);
                }

                part.writeToNBT(partData);
                if (partData.hasKey("id")) {
                    throw new IllegalStateException("Part " + part + " used the reserved 'id' field to store its data");
                }

                partData.setString("id", IPartItem.getId(part.getPartItem()).toString());
                var sideKey = NBT_KEY_SIDES[getSideIndex(side)];
                data.setTag(sideKey, partData);
            }
        }
    }

    public void readFromNBT(NBTTagCompound data) {
        invalidateShapes();

        if (data.hasKey("hasRedstone")) {
            this.hasRedstone = YesNo.values()[data.getInteger("hasRedstone")];
        }

        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var sideIndex = getSideIndex(side);

            var sideKey = NBT_KEY_SIDES[sideIndex];
            var sideTag = data.getTag(sideKey);
            if (sideTag instanceof NBTTagCompound partData && loadPart(side, partData)) {
                continue;
            }

            // If nothing was loaded, the side is empty and has to be cleared
            this.removePartFromSide(side);
        }

        this.getFacadeContainer().readFromNBT(data);
    }

    private boolean loadPart(EnumFacing side, NBTTagCompound data) {
        var itemId = new ResourceLocation(data.getString("id"));
        var partItem = IPartItem.byId(itemId);
        if (partItem == null) {
            AELog.warn("Ignoring persisted part with non-part-item %s", itemId);
            return false;
        }

        var p = this.getPart(side);
        if (p != null && p.getPartItem() == partItem) {
            p.readFromNBT(data);
        } else {
            p = this.replacePart(partItem, side, null, null);
            if (p != null) {
                p.readFromNBT(data);
            } else {
                AELog.warn("Invalid NBT For CableBus Container: " + itemId
                        + " is not a valid part; it was ignored.");
            }
        }
        return true;
    }

    public List<ItemStack> addPartDrops(List<ItemStack> drops) {
        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(side);
            if (part != null) {
                part.addPartDrop(drops, false);
            }

            if (side != null) {
                var fp = this.getFacadeContainer().getFacade(side);
                if (fp != null) {
                    drops.add(fp.getItemStack());
                }
            }
        }

        return drops;
    }

    public void addAdditionalDrops(List<ItemStack> drops) {
        for (var side : Platform.DIRECTIONS_WITH_NULL) {
            var part = this.getPart(side);
            if (part != null) {
                part.addAdditionalDrops(drops, false);
            }
        }
    }

    public void clearContent() {
        for (var s : Platform.DIRECTIONS_WITH_NULL) {
            var part = getPart(s);
            if (part != null) {
                part.clearContent();
            }
        }
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor colour, EntityPlayer who) {
        final IPart cable = this.getPart(null);
        if (cable != null) {
            final ICablePart pc = (ICablePart) cable;
            return pc.changeColor(colour, who);
        }
        return false;
    }

    public boolean isRequiresDynamicRender() {
        return this.requiresDynamicRender;
    }

    private void setRequiresDynamicRender(boolean requiresDynamicRender) {
        this.requiresDynamicRender = requiresDynamicRender;
    }

    @Override
    public CableBusRenderState getRenderState() {
        final CablePart cable = (CablePart) this.storage.getCenter();

        final CableBusRenderState renderState = new CableBusRenderState();

        if (cable != null) {
            renderState.setCableColor(cable.getCableColor());
            renderState.setCableType(cable.getCableConnectionType());
            renderState.setCoreType(CableCoreType.fromCableType(cable.getCableConnectionType()));

            // Check each outgoing connection for the desired characteristics
            for (var side : EnumFacing.VALUES) {
                // Is there a connection?
                if (!cable.isConnected(side)) {
                    continue;
                }

                // If there is one, check out which type it has, but default to this cable's
                // type
                AECableType connectionType = cable.getCableConnectionType();

                // Only use the incoming cable-type of the adjacent block, if it's not a cable bus itself
                // Dense cables however also respect the adjacent cable-type since their outgoing connection
                // point would look too big for other cable types
                final BlockPos adjacentPos = this.getTileEntity().getPos().offset(side);
                var adjacentHost = GridHelper.getNodeHost(getTileEntity().getWorld(), adjacentPos);

                if (adjacentHost != null) {
                    var adjacentType = adjacentHost.getCableConnectionType(side.getOpposite());
                    connectionType = AECableType.min(connectionType, adjacentType);
                }

                // Check if the adjacent TE is a cable bus or not
                if (adjacentHost instanceof CableBusContainer) {
                    renderState.getCableBusAdjacent().add(side);
                }

                renderState.getConnectionTypes().put(side, connectionType);
            }

            // Collect the number of channels used per side
            // We have to do this even for non-smart cables since a glass cable can display
            // a connection as smart if the
            // adjacent block entity requires it
            for (var side : EnumFacing.VALUES) {
                int channels = cable.getCableConnectionType().isSmart() ? cable.getChannelsOnSide(side) : 0;
                renderState.getChannelsOnSide().put(side, channels);
            }
        }

        // Determine attachments and facades
        for (var side : EnumFacing.VALUES) {
            final FacadeRenderState facadeState = this.getFacadeRenderState(side);

            if (facadeState != null) {
                renderState.getFacades().put(side, facadeState);
            }

            final IPart part = this.getPart(side);

            if (part == null) {
                continue;
            }

            renderState.getPartModelData().put(side, part.getModelData());

            // This will add the part's bounding boxes to the render state, which is
            // required for facades
            final IPartCollisionHelper bch = new BusCollisionHelper(renderState.getBoundingBoxes(), side, true);
            part.getBoxes(bch);

            // Some attachments want a thicker cable than glass, account for that
            var desiredType = part.getDesiredConnectionType();
            if (renderState.getCoreType() == CableCoreType.GLASS
                    && (desiredType == AECableType.SMART || desiredType == AECableType.COVERED)) {
                renderState.setCoreType(CableCoreType.COVERED);
            }

            int length = (int) part.getCableConnectionLength(null);
            if (length > 0 && length <= 8) {
                renderState.getAttachmentConnections().put(side, length);
            }

            renderState.getAttachments().put(side, part.getStaticModels());
        }

        return renderState;
    }

    private FacadeRenderState getFacadeRenderState(EnumFacing side) {
        // Store the "masqueraded" itemstack for the given side, if there is a facade
        final IFacadePart facade = this.storage.getFacade(side);

        if (facade != null) {
            final IBlockState blockState = facade.getBlockState();

            World level = getTileEntity().getWorld();
            if (blockState != null && level != null) {
                return new FacadeRenderState(blockState,
                        !facade.getBlockState().isSolidRender(level, getTileEntity().getPos()));
            }
        }

        return null;
    }

    /**
     * See {@link IBlockState#getShape}
     */
    public VoxelShape getShape() {
        var currentRenderMode = PartHelper.getCableRenderMode();
        if (cachedShape == null || currentRenderMode != cachedShapeCableRenderMode) {
            cachedShape = createShape(false, false);
            cachedShapeCableRenderMode = currentRenderMode;
        }

        return cachedShape;
    }

    /**
     * See {@link IBlockState#getCollisionShape}
     */
    public VoxelShape getCollisionShape(CollisionContext context) {
        // This is a hack for annihilation planes
        var itemEntity = context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof EntityItem;

        if (itemEntity) {
            if (cachedCollisionShapeLiving == null) {
                cachedCollisionShapeLiving = createShape(true, true);
            }
            return cachedCollisionShapeLiving;
        } else {
            if (cachedCollisionShape == null) {
                cachedCollisionShape = createShape(true, false);
            }
            return cachedCollisionShape;
        }
    }

    private VoxelShape createShape(boolean forCollision, boolean forItemEntity) {
        final List<AxisAlignedBB> boxes = new ArrayList<>();

        var fc = this.getFacadeContainer();
        for (EnumFacing s : Platform.DIRECTIONS_WITH_NULL) {
            final IPartCollisionHelper bch = new BusCollisionHelper(boxes, s, !forCollision);

            final IPart part = this.getPart(s);
            if (part != null) {
                part.getBoxes(bch);
            }

            if ((PartHelper.getCableRenderMode().opaqueFacades || forCollision)
                    && s != null) {
                var fp = fc.getFacade(s);
                if (fp != null) {
                    fp.getBoxes(bch, forItemEntity);
                }
            }
        }

        return VoxelShapeCache.get(boxes);
    }

    private void invalidateShapes() {
        cachedShape = null;
        cachedCollisionShape = null;
        cachedCollisionShapeLiving = null;
    }

    private void updateNeighborShapeOnSide(EnumFacing side) {
        if (side == null) {
            return; // side can be null for cable
        }
        // Update the shape of the neighbor asynchronously (i.e. for walls)
        var be = getTileEntity();
        if (be != null && be.getWorld() != null && !be.getWorld().isRemote) {
            TickHandler.instance().addCallable(be.getWorld(), level -> {
                if (!be.isInvalid()) {
                    var ourPos = be.getPos();
                    var neighborPos = ourPos.offset(side);
                    var neighborState = level.getBlockState(neighborPos);
                    var ourState = Platform.getBlockState(be);
                    IBlockState newNeighborState = neighborState.updateShape(side.getOpposite(), ourState, level,
                            neighborPos, ourPos);
                    Block.updateOrDestroy(neighborState, newNeighborState, level, neighborPos, Block.UPDATE_ALL,
                            Block.UPDATE_LIMIT);
                }
            });
        }
    }
}
