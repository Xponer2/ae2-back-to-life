/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.block.crafting;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import appeng.util.ItemInteractionResult;
import appeng.util.Platform;

public class PatternProviderBlock extends AEBaseEntityBlock<PatternProviderBlockEntity> {

    public static final PropertyEnum<PushDirection> PUSH_DIRECTION = PropertyEnum.create("push_direction",
            PushDirection.class);

    public PatternProviderBlock() {
        super(metalProps());
        setDefaultState(getDefaultState().withProperty(PUSH_DIRECTION, PushDirection.ALL));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PUSH_DIRECTION);
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        var be = this.getTileEntity(level, pos);
        if (be != null) {
            be.getLogic().updateRedstoneState();
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (InteractionUtil.canWrenchRotate(heldItem)) {
            setSide(level, pos, hit.sideHit);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        var be = this.getTileEntity(level, pos);
        if (be != null) {
            if (!level.isRemote) {
                be.openMenu(player, MenuLocators.forBlockEntity(be));
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    public void setSide(World level, BlockPos pos, EnumFacing facing) {
        var currentState = level.getBlockState(pos);
        var pushSide = currentState.getValue(PUSH_DIRECTION).getDirection();

        PushDirection newPushDirection;
        if (pushSide == facing.getOpposite()) {
            newPushDirection = PushDirection.fromDirection(facing);
        } else if (pushSide == facing) {
            newPushDirection = PushDirection.ALL;
        } else if (pushSide == null) {
            newPushDirection = PushDirection.fromDirection(facing.getOpposite());
        } else {
            newPushDirection = PushDirection.fromDirection(Platform.rotateAround(pushSide, facing));
        }

        level.setBlockState(pos, currentState.withProperty(PUSH_DIRECTION, newPushDirection));
    }
}
