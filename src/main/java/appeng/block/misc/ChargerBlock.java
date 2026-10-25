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

package appeng.block.misc;

import appeng.client.render.effects.AEParticles;
import net.minecraft.util.math.Vec3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.AEAxisAlignedBB;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.ChargerRecipes;
import appeng.client.render.effects.LightningArcParticleData;
import appeng.core.AEConfig;
import appeng.core.AppEngClient;
import appeng.util.ItemInteractionResult;
import appeng.util.Platform;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class ChargerBlock extends AEBaseEntityBlock<ChargerBlockEntity> {

    public ChargerBlock() {
        super(metalProps().noOcclusion());
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.full();
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess level, BlockPos pos) {
        return 2; // FIXME Double check this (esp. value range)
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (level.getTileEntity(pos) instanceof ChargerBlockEntity charger) {
            var inv = charger.getInternalInventory();
            var chargingItem = inv.getStackInSlot(0);
            if (chargingItem.isEmpty()) {
                if (ChargerRecipes.findRecipe(level, heldItem) != null || Platform.isChargeable(heldItem)) {
                    var toInsert = heldItem.splitStack(1);
                    inv.setItemDirect(0, toInsert);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (level.getTileEntity(pos) instanceof ChargerBlockEntity charger) {
            var inv = charger.getInternalInventory();
            var chargingItem = inv.getStackInSlot(0);
            if (!chargingItem.isEmpty()) {
                inv.setItemDirect(0, ItemStack.EMPTY);
                Platform.spawnDrops(player.world, charger.getPos().offset(charger.getFront()),
                        ImmutableList.of(chargingItem));
                return EnumActionResult.SUCCESS;
            }
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void animateTick(IBlockState state, World level, BlockPos pos, Random r) {
        if (!AEConfig.instance().isEnableEffects()) {
            return;
        }

        var blockEntity = this.getTileEntity(level, pos);
        if (blockEntity != null && blockEntity.isWorking()) {
            if (r.nextFloat() < 0.5) {
                return;
            }

            var rotation = BlockOrientation.get(blockEntity);

            for (int bolts = 0; bolts < 3; bolts++) {
                // Slightly offset the lightning arc on the x/z plane
                var xOff = -0.15f + r.nextFloat() * 0.3f;
                var zOff = -0.15f + r.nextFloat() * 0.3f;

                // Compute two points in the charger block. One at the bottom, and one on the top.
                // Account for the rotation while doing this.
                var rotationMatrix = new Matrix4f();
                rotationMatrix.set(rotation.getQuaternion());
                var center = new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
                var origin = new Vector3f(xOff, -0.3f, zOff);
                rotationMatrix.transform(origin);
                origin.add(center);
                var target = new Vector3f(xOff, 0.3f, zOff);
                rotationMatrix.transform(target);
                target.add(center);

                // Split the arcs between arc coming from the top/bottom of the charger since it's symmetrical
                if (r.nextBoolean()) {
                    var tmp = target;
                    target = origin;
                    origin = tmp;
                }

                if (AppEngClient.instance().shouldAddParticles(r)) {
                    AEParticles.add(new LightningArcParticleData(new Vec3d(target.x, target.y, target.z)),
                            origin.x, origin.y, origin.z, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        var orientation = getOrientation(state);
        var up = orientation.getSide(RelativeSide.TOP);
        var forward = orientation.getSide(RelativeSide.FRONT);
        var twoPixels = 2.0 / 16.0;

        var bb = new AEAxisAlignedBB(twoPixels, twoPixels, twoPixels, 1.0 - twoPixels,
                1.0 - twoPixels, 1.0 - twoPixels);

        if (up.getXOffset() != 0) {
            bb.minX = 0;
            bb.maxX = 1;
        }
        if (up.getYOffset() != 0) {
            bb.minY = 0;
            bb.maxY = 1;
        }
        if (up.getZOffset() != 0) {
            bb.minZ = 0;
            bb.maxZ = 1;
        }

        switch (forward) {
            case DOWN -> bb.maxY = 1;
            case UP -> bb.minY = 0;
            case NORTH -> bb.maxZ = 1;
            case SOUTH -> bb.minZ = 0;
            case EAST -> bb.minX = 0;
            case WEST -> bb.maxX = 1;
            default -> {
            }
        }

        return Shapes.create(bb.getBoundingBox());
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos,
            CollisionContext context) {
        return Shapes.create(new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
    }

}
