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

package appeng.api.orientation;

import java.util.EnumSet;
import java.util.Set;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * All possible rotations for a fully orientable block.
 */
public enum BlockOrientation {

    // DUNSWE
    // @formatter:off
    DOWN_NORTH(90, 0, 0, 0),
    DOWN_WEST(90, 0, 270, 1),
    DOWN_SOUTH(90, 0, 180, 2),
    DOWN_EAST(90, 0, 90, 3),

    UP_NORTH(270, 0, 180, 0),
    UP_EAST(270, 0, 90, 1),
    UP_SOUTH(270, 0, 0, 2),
    UP_WEST(270, 0, 270, 3),

    NORTH_UP(0, 0, 0, 0), // Default,
    NORTH_WEST(0, 0, 270, 1),
    NORTH_DOWN(0, 0, 180, 2),
    NORTH_EAST(0, 0, 90, 3),

    SOUTH_UP(0, 180, 0, 0),
    SOUTH_EAST(0, 180, 90, 1),
    SOUTH_DOWN(0, 180, 180, 2),
    SOUTH_WEST(0, 180, 270, 3),

    WEST_UP(0, 270, 0, 0),
    WEST_SOUTH(0, 270, 270, 1),
    WEST_DOWN(0, 270, 180, 2),
    WEST_NORTH(0, 270, 90, 3),

    EAST_UP(0, 90, 0, 0),
    EAST_NORTH(0, 90, 270, 1),
    EAST_DOWN(0, 90, 180, 2),
    EAST_SOUTH(0, 90, 90, 3);
    // @formatter:on

    private final int angleX;
    private final int angleY;
    private final int angleZ;
    private final Quat4f quaternion;
    private final Matrix4f matrix;
    private final TRSRTransformation transformation;
    /**
     * How many times it has been rotated clock-wise around in 90° increments around its facing.
     */
    private final int spin;
    // Map each EnumFacing to the EnumFacing it'll be rotated to
    private final EnumFacing[] rotatedSideTo;
    // Reverse of rotatedSideTo
    private final EnumFacing[] rotatedSideFrom;

    BlockOrientation(int angleX, int angleY, int angleZ, int spin) {
        this.angleX = angleX;
        this.angleY = angleY;
        this.angleZ = angleZ;

        // NOTE: Mojangs block model rotation rotates in the opposite direction
        // Same as JOML's Quaternionf.rotateYXZ(-y, -x, -z): R = Ry * Rx * Rz
        final float degToRad = (float) (Math.PI / 180.0);
        var ry = new Matrix4f();
        ry.rotY(-angleY * degToRad);
        var rx = new Matrix4f();
        rx.rotX(-angleX * degToRad);
        var rz = new Matrix4f();
        rz.rotZ(-angleZ * degToRad);
        var m = new Matrix4f(ry);
        m.mul(rx);
        m.mul(rz);
        this.matrix = m;
        this.quaternion = new Quat4f();
        this.quaternion.set(m);

        if (angleX == 0 && angleY == 0 && angleZ == 0) {
            this.transformation = TRSRTransformation.identity();
        } else {
            this.transformation = new TRSRTransformation(m);
        }
        this.spin = spin;

        // Build a mapping between the sides in this orientation
        this.rotatedSideTo = new EnumFacing[EnumFacing.VALUES.length];
        this.rotatedSideFrom = new EnumFacing[EnumFacing.VALUES.length];
        for (var direction : EnumFacing.VALUES) {
            var normal = new Vector3f(direction.getXOffset(), direction.getYOffset(), direction.getZOffset());
            matrix.transform(normal);
            var rotatedTo = EnumFacing.getFacingFromVector(normal.x, normal.y, normal.z);
            rotatedSideTo[direction.ordinal()] = rotatedTo;
            rotatedSideFrom[rotatedTo.ordinal()] = direction;
        }
    }

    /**
     * Changes the orientation of the given block entity to this, if possible.
     */
    public void setOn(TileEntity be) {
        setOn(be.getWorld(), be.getPos());
    }

    /**
     * Changes the orientation of the block at the given position/level to this, if possible.
     */
    public void setOn(World level, BlockPos pos) {
        // On 1.12.2, orientable tile entities store their orientation themselves
        if (level.getTileEntity(pos) instanceof IOrientationHolder holder) {
            holder.setOrientation(this);
            return;
        }
        var state = level.getBlockState(pos);
        var strategy = IOrientationStrategy.get(state);
        var newState = strategy.setOrientation(state,
                getSide(RelativeSide.FRONT),
                getSpin());
        if (newState != state) {
            level.setBlockState(pos, newState);
        }

    }

    public boolean isRedundant() {
        return angleX == 0 && angleY == 0 && angleZ == 0;
    }

    public Quat4f getQuaternion() {
        return new Quat4f(this.quaternion);
    }

    /**
     * @return The rotation as a 4x4 matrix (a copy).
     */
    public Matrix4f getMatrix() {
        return new Matrix4f(matrix);
    }

    public TRSRTransformation getTransformation() {
        return transformation;
    }

    public EnumFacing rotate(EnumFacing facing) {
        return rotatedSideTo[facing.ordinal()];
    }

    public EnumFacing resultingRotate(EnumFacing facing) {
        return rotatedSideFrom[facing.ordinal()];
    }

    public int getAngleX() {
        return angleX;
    }

    public int getAngleY() {
        return angleY;
    }

    public int getAngleZ() {
        return angleZ;
    }

    public int getSpin() {
        return spin;
    }

    public static BlockOrientation get(EnumFacing facing) {
        return get(facing, 0);
    }

    /**
     * Gets the block orientation in which the blocks front and top are facing the specified directions.
     */
    public static BlockOrientation get(EnumFacing front, EnumFacing top) {
        var offset = front.ordinal() * 4;
        for (var i = offset; i < offset + 4; i++) {
            var orientation = values()[i];
            if (orientation.getSide(RelativeSide.TOP) == top) {
                return orientation;
            }
        }
        return values()[offset]; // Degenerated up -> return default
    }

    public static BlockOrientation get(EnumFacing facing, int spin) {
        return values()[facing.ordinal() * 4 + spin];
    }

    public static BlockOrientation get(TileEntity blockEntity) {
        if (blockEntity instanceof IOrientationHolder holder) {
            return holder.getOrientation();
        }
        var level = blockEntity.getWorld();
        if (level == null) {
            return NORTH_UP;
        }
        return get(level.getBlockState(blockEntity.getPos()).getActualState(level, blockEntity.getPos()));
    }

    public static BlockOrientation get(IBlockState state) {
        var strategy = IOrientationStrategy.get(state);
        return get(strategy, state);
    }

    public static BlockOrientation get(IOrientationStrategy strategy, IBlockState state) {
        var facing = strategy.getFacing(state);
        var spin = strategy.getSpin(state);
        return get(facing, spin);
    }

    public EnumFacing getSide(RelativeSide side) {
        return rotate(side.getUnrotatedSide());
    }

    public RelativeSide getRelativeSide(EnumFacing side) {
        return RelativeSide.fromUnrotatedSide(resultingRotate(side));
    }

    public Set<EnumFacing> getSides(Set<RelativeSide> relativeSides) {
        var result = EnumSet.noneOf(EnumFacing.class);
        for (var relativeSide : relativeSides) {
            result.add(getSide(relativeSide));
        }
        return result;
    }

    public Set<RelativeSide> getRelativeSides(Set<EnumFacing> sides) {
        var result = EnumSet.noneOf(RelativeSide.class);
        for (var side : sides) {
            result.add(getRelativeSide(side));
        }
        return result;
    }

    public BlockOrientation rotateClockwiseAround(EnumFacing side) {
        return rotateClockwiseAround(side.getAxis(), side.getAxisDirection());
    }

    public BlockOrientation rotateClockwiseAround(EnumFacing.Axis axis, EnumFacing.AxisDirection direction) {
        var facing = getSide(RelativeSide.FRONT);
        var up = getSide(RelativeSide.TOP);
        EnumFacing newFacing;
        EnumFacing newUp;
        if (direction == EnumFacing.AxisDirection.POSITIVE) {
            newFacing = facing.rotateAround(axis);
            newUp = up.rotateAround(axis);
        } else {
            newFacing = facing.rotateAround(axis).rotateAround(axis).rotateAround(axis);
            newUp = up.rotateAround(axis).rotateAround(axis).rotateAround(axis);
        }
        return get(newFacing, newUp);
    }
}
