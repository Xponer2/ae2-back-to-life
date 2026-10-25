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

package appeng.blockentity.misc;

import net.minecraft.block.state.BlockFaceShape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.netty.buffer.Unpooled;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import appeng.api.util.AEColor;
import appeng.block.paint.PaintSplotches;
import appeng.block.paint.PaintSplotchesBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.client.render.model.ModelData;
import appeng.client.render.model.ModelProperty;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.helpers.Splotch;
import appeng.items.misc.PaintBallItem;

public class PaintSplotchesBlockEntity extends AEBaseBlockEntity {

    public static final ModelProperty<PaintSplotches> SPLOTCHES = new ModelProperty<>();

    private List<Splotch> dots = null;

    public PaintSplotchesBlockEntity() {
        super();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        final RegistryFriendlyByteBuf myDat = new RegistryFriendlyByteBuf(Unpooled.buffer());
        this.writeBuffer(myDat);
        if (myDat.hasArray()) {
            data.setByteArray("dots", myDat.array());
        }
    }

    private void writeBuffer(RegistryFriendlyByteBuf out) {
        if (this.dots == null) {
            out.writeByte(0);
            return;
        }

        out.writeByte(this.dots.size());

        for (Splotch s : this.dots) {
            s.writeToStream(out);
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        if (data.hasKey("dots")) {
            this.readBuffer(new RegistryFriendlyByteBuf(Unpooled.copiedBuffer(data.getByteArray("dots"))));
        }
    }

    private void readBuffer(RegistryFriendlyByteBuf in) {
        final byte howMany = in.readByte();

        if (howMany == 0) {
            this.dots = null;
            return;
        }

        this.dots = new ArrayList<>(howMany);
        for (int x = 0; x < howMany; x++) {
            this.dots.add(new Splotch(in));
        }
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        this.writeBuffer(data);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        super.readFromStream(data);
        this.readBuffer(data);
        return true;
    }

    public void neighborChanged() {
        if (this.dots == null) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (!this.isSideValid(side)) {
                this.removeSide(side);
            }
        }

        this.updateData();
    }

    public boolean isSideValid(EnumFacing side) {
        final BlockPos p = this.pos.offset(side);
        final IBlockState blk = this.world.getBlockState(p);
        return blk.getBlockFaceShape(world, p, side.getOpposite()) == BlockFaceShape.SOLID;
    }

    private void removeSide(EnumFacing side) {
        this.dots.removeIf(s -> s.getSide() == side);

        this.markForUpdate();
        this.saveChanges();
    }

    private void updateData() {
        if (this.dots.isEmpty()) {
            this.dots = null;
        }

        if (this.dots == null) {
            this.world.setBlockToAir(this.pos);
        } else {
            var lumenCount = 0;
            for (Splotch dot : dots) {
                if (dot.isLumen()) {
                    lumenCount++;
                    if (lumenCount >= 2) {
                        break;
                    }
                }
            }
            this.world.setBlockState(getPos(),
                    getBlockState().withProperty(PaintSplotchesBlock.LIGHT_LEVEL, lumenCount));
        }
    }

    public void cleanSide(EnumFacing side) {
        if (this.dots == null) {
            return;
        }

        this.removeSide(side);
        this.updateData();
    }

    public void addBlot(ItemStack type, EnumFacing side, Vec3d hitVec) {
        PaintBallItem paintBallItem = (PaintBallItem) type.getItem();
        addBlot(paintBallItem.getColor(), paintBallItem.isLumen(), side, hitVec);
    }

    public void addBlot(AEColor color, boolean lit, EnumFacing side, Vec3d hitVec) {
        var p = this.pos.offset(side);
        var blk = this.world.getBlockState(p);
        if (blk.getBlockFaceShape(this.world, p, side.getOpposite()) == BlockFaceShape.SOLID) {
            if (this.dots == null) {
                this.dots = new ArrayList<>();
            }

            if (this.dots.size() > 20) {
                this.dots.remove(0);
            }

            this.dots.add(new Splotch(color, lit, side, hitVec));

            updateData();
            this.markForUpdate();
            this.saveChanges();
        }
    }

    public Collection<Splotch> getDots() {
        if (this.dots == null) {
            return Collections.emptyList();
        }

        return this.dots;
    }

    @Override
    public ModelData getModelData() {
        // FIXME update trigger
        return ModelData.builder().with(SPLOTCHES, new PaintSplotches(getDots())).build();
    }
}
