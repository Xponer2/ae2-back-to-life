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

package appeng.block.misc;

import appeng.util.Platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.entity.projectile.AbstractArrow;

import appeng.block.AEBaseBlock;
import appeng.entity.TinyTNTPrimedEntity;
import appeng.util.ItemInteractionResult;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class TinyTNTBlock extends AEBaseBlock {

    private static final VoxelShape SHAPE = Shapes
            .create(new AxisAlignedBB(0.25f, 0.0f, 0.25f, 0.75f, 0.5f, 0.75f));

    public TinyTNTBlock(BlockProperties props) {
        super(props);
    }

    @Override
    public int getLightBlock(IBlockState state, IBlockAccess level, BlockPos pos) {
        return 2; // FIXME: Validate that this is the correct value range
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (Platform.is(heldItem, Items.FLINT_AND_STEEL) || Platform.is(heldItem, Items.FIRE_CHARGE)) {
            onCaughtFire(state, level, pos, hit.getDirection(), player);
            level.setBlock(pos, Blocks.AIR.getDefaultState(), Block.UPDATE_ALL_IMMEDIATE);
            Item item = heldItem.getItem();
            if (!player.isCreative()) {
                if (Platform.is(heldItem, Items.FLINT_AND_STEEL)) {
                    heldItem.hurtAndBreak(1, player, EntityLivingBase.getSlotForHand(hand));
                } else {
                    heldItem.shrink(1);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(item));
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    public void onCaughtFire(IBlockState state, World level, BlockPos pos, @Nullable EnumFacing direction,
            @Nullable EntityLivingBase igniter) {
        this.startFuse(level, pos, igniter);
    }

    public void startFuse(World level, BlockPos pos, EntityLivingBase igniter) {
        if (!level.isRemote) {
            var primedTinyTNTEntity = new TinyTNTPrimedEntity(level, pos.getX() + 0.5F, pos.getY(),
                    pos.getZ() + 0.5F, igniter);
            level.spawnEntity(primedTinyTNTEntity);
            level.playSound(null, primedTinyTNTEntity.getX(), primedTinyTNTEntity.getY(),
                    primedTinyTNTEntity.getZ(), SoundEvents.TNT_PRIMED, SoundCategory.BLOCKS, 1, 1);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        if (level.isBlockIndirectlyGettingPowered(pos) > 0) {
            this.startFuse(level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void onPlace(IBlockState state, World level, BlockPos pos, IBlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (level.isBlockIndirectlyGettingPowered(pos) > 0) {
            this.startFuse(level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void stepOn(World level, BlockPos pos, IBlockState state, Entity entity) {
        if (!level.isRemote && entity instanceof AbstractArrow arrow) {

            if (arrow.isOnFire()) {
                EntityLivingBase igniter = null;
                // Check if the shooter still exists
                Entity shooter = arrow.getOwner();
                if (shooter instanceof EntityLivingBase) {
                    igniter = (EntityLivingBase) shooter;
                }
                this.startFuse(level, pos, igniter);
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion exp) {
        return false;
    }

    @Override
    public void wasExploded(World level, BlockPos pos, Explosion exp) {
        super.wasExploded(level, pos, exp);
        if (!level.isRemote) {
            final TinyTNTPrimedEntity primedTinyTNTEntity = new TinyTNTPrimedEntity(level, pos.getX() + 0.5F,
                    pos.getY(), pos.getZ() + 0.5F, exp.getIndirectSourceEntity());
            primedTinyTNTEntity
                    .setFuse(level.rand.nextInt(primedTinyTNTEntity.getFuse() / 4)
                            + primedTinyTNTEntity.getFuse() / 8);
            level.spawnEntity(primedTinyTNTEntity);
        }
    }

}
