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

package appeng.parts;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.client.render.cablebus.CableBusRenderState;

public class NullCableBusContainer implements ICableBusContainer {

    @Override
    public int isProvidingStrongPower(EnumFacing opposite) {
        return 0;
    }

    @Override
    public int isProvidingWeakPower(EnumFacing opposite) {
        return 0;
    }

    @Override
    public boolean canConnectRedstone(EnumFacing opposite) {
        return false;
    }

    @Override
    public void onEntityCollision(Entity e) {

    }

    @Override
    public boolean useItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d localPos) {
        return false;
    }

    @Override
    public boolean useWithoutItem(EntityPlayer player, Vec3d localPos) {
        return false;
    }

    @Override
    public void onNeighborChanged(IBlockAccess level, BlockPos pos, BlockPos neighbor) {
    }

    @Override
    public void onUpdateShape(World level, BlockPos pos, EnumFacing side) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public SelectedPart selectPartLocal(Vec3d v3) {
        return new SelectedPart();
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor colour, EntityPlayer who) {
        return false;
    }

    @Override
    public boolean isLadder(EntityLivingBase entity) {
        return false;
    }

    @Override
    public void animateTick(World level, BlockPos pos, Random r) {

    }

    @Override
    public int getLightValue() {
        return 0;
    }

    @Override
    public CableBusRenderState getRenderState() {
        return new CableBusRenderState();
    }

}
