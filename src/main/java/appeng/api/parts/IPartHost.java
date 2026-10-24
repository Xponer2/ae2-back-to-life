/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
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

package appeng.api.parts;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import appeng.api.util.AEColor;
import appeng.api.util.DimensionalBlockPos;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.VoxelShape;

/**
 * Implemented on AE's block entities.
 * <p>
 * Do Not Implement
 */
public interface IPartHost extends ICustomCableConnection {

    /**
     * @return the facade container
     */
    IFacadeContainer getFacadeContainer();

    /**
     * Get a part attached to the host based on the location it's attached to.
     *
     * @param side side of host or null for center.
     * @return the part located on the specified side, or null if there is no part.
     */
    @Nullable
    IPart getPart(@Nullable EnumFacing side);

    /**
     * Test if you can add a part to the specified side of the Part Host. A null side represents the center of the part
     * host, where a cable would normally be.
     *
     * @param part to be added part
     * @param side onto side or null for center of host.
     * @return returns false if the part cannot be added.
     */
    boolean canAddPart(ItemStack part, @Nullable EnumFacing side);

    /**
     * Add a new part to the specified side, returns false if it failed to be added.
     *
     * @param partItem new part item
     * @param side     onto side or null for center of host.
     * @param owner    with owning player
     * @return The part if the part could be placed, or null.
     */
    @Nullable
    <T extends IPart> T addPart(IPartItem<T> partItem, @Nullable EnumFacing side, @Nullable EntityPlayer owner);

    /**
     * Replace an existing part on the specific side with a new one. Returns null if it failed to be replaced.
     *
     * @param partItem the part item to create the part from
     * @param side     onto side or null for center of host.
     * @param owner    with owning player
     * @return If the part could be replaced, or null.
     */
    @Nullable
    <T extends IPart> T replacePart(IPartItem<T> partItem, @Nullable EnumFacing side, @Nullable EntityPlayer owner,
            @Nullable EnumHand hand);

    /**
     * Removes the part on the side, this doesn't drop it or anything, if you don't do something with it, its just
     * "gone" and its never coming back; think about it.
     * <p>
     * if you want to drop the part you must request it prior to removing it.
     *
     * @param side onto side or null for center of host
     */
    void removePartFromSide(@Nullable EnumFacing side);

    /**
     * something changed, might want to send a packet to clients to update state.
     */
    void markForUpdate();

    /**
     * @return the physical location of the part host in the universe.
     */
    DimensionalBlockPos getLocation();

    /**
     * @return the block entity for the host, this can either be an FMP block entity, or a AE block entity
     */
    TileEntity getTileEntity();

    /**
     * @return the color of the host type ( this is determined by the middle cable. ) if no cable is present, it returns
     *         {@link AEColor} .Transparent other wise it returns the color of the cable in the center.
     */
    AEColor getColor();

    /**
     * destroys the part container, for internal use.
     */
    void clearContainer();

    /**
     * Used to test for FMP microblock blocking internally.
     *
     * @return returns if microblocks are blocking this cable path.
     */
    boolean isBlocked(EnumFacing side);

    /**
     * Finds the part located at the position in block-local coordinates (0,0,0 is located at the block pos).
     *
     * @param pos part position
     * @return a new SelectedPart, this is never null.
     */
    SelectedPart selectPartLocal(Vec3d pos);

    /**
     * Gets the collision shape of the part host. Used to reject placing parts if the resulting collision shape would be
     * obstructed in the level.
     *
     * @return The collision shape of this part host including all attached parts.
     */
    VoxelShape getCollisionShape(CollisionContext context);

    /**
     * Removes a specific part from this host and returns if the part was actually present and removed.
     */
    boolean removePart(IPart part);

    /**
     * Same as {@link #selectPartLocal(Vec3d)}, but with world instead of local coordinates. Provided for easier
     * interoperability with {@link RayTraceResult#getLocation()}.
     */
    default SelectedPart selectPartWorld(Vec3d pos) {
        var worldPos = getLocation();
        return selectPartLocal(pos.subtract(
                worldPos.getPos().getX(),
                worldPos.getPos().getY(),
                worldPos.getPos().getZ()));
    }

    /**
     * can be used by parts to trigger the block entity or part to save.
     */
    void markForSave();

    void partChanged();

    /**
     * Check if the part host currently has redstone power greater than 0. This value is cached internally.
     *
     * @return true of the part host is receiving redstone from an external source.
     */
    boolean hasRedstone();

    /**
     * returns false if this block contains any parts or facades, true other wise.
     */
    boolean isEmpty();

    /**
     * remove host from level...
     */
    void cleanup();

    /**
     * notify neighbors uf updated status.
     */
    void notifyNeighbors();

    /**
     * notify neighbors uf updated status on the given side of the host. issue the notification immediately and
     * synchronously.
     */
    void notifyNeighborNow(EnumFacing side);

    /**
     * true if the block entity is in the world, other wise false.
     *
     * @return true if block entity is in world
     */
    boolean isInWorld();
}
