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

package appeng.block.crafting;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.util.Platform;
import appeng.api.orientation.IOrientationHolder;
import appeng.api.orientation.BlockOrientation;
import appeng.block.BlockProperties;
import appeng.util.TextUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.PlayerMessages;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.recipes.game.CraftingUnitTransformRecipe;
import appeng.util.InteractionUtil;
import appeng.util.ItemInteractionResult;

public abstract class AbstractCraftingUnitBlock<T extends CraftingBlockEntity> extends AEBaseEntityBlock<T> {
    private static final Logger LOG = LogManager.getLogger(AbstractCraftingUnitBlock.class);

    public static final PropertyBool FORMED = PropertyBool.create("formed");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public final ICraftingUnitType type;

    public AbstractCraftingUnitBlock(BlockProperties props, ICraftingUnitType type) {
        super(props);
        this.type = type;
        this.setDefaultState(getDefaultState().withProperty(FORMED, false).withProperty(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
        builder.add(FORMED);
    }

    @Override
    public IBlockState updateShape(IBlockState stateIn, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {
        TileEntity te = level.getTileEntity(currentPos);
        if (te != null) {
            if (te instanceof AEBaseBlockEntity aeBlockEntity) {
                aeBlockEntity.requestModelDataUpdate();
            }
        }
        return super.updateShape(stateIn, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn,
            BlockPos fromPos, boolean isMoving) {
        final CraftingBlockEntity cp = this.getTileEntity(level, pos);
        if (cp != null) {
            cp.updateMultiBlock(fromPos);
        }
    }

    @Override
    public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean isMoving) {
        if (newState.getBlock() == state.getBlock()) {
            return; // Just a block state change
        }

        final CraftingBlockEntity cp = this.getTileEntity(level, pos);
        if (cp != null) {
            cp.breakCluster();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            var result = this.removeUpgrade(level, player, pos, AEBlocks.CRAFTING_UNIT.block().getDefaultState());
            if (result != EnumActionResult.FAIL)
                return result;
        }

        if (level.getTileEntity(pos) instanceof CraftingBlockEntity be && be.isFormed() && be.isActive()) {
            if (!level.isRemote) {
                MenuOpener.open(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(be));
            }

            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (this.upgrade(heldItem, state, level, pos, player, hit))
            return ItemInteractionResult.SUCCESS;
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    public boolean upgrade(ItemStack heldItem, IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hit) {
        if (heldItem.isEmpty()) {
            return false;
        }

        var upgradedBlock = CraftingUnitTransformRecipe.getUpgradedBlock(level, heldItem);
        if (upgradedBlock == null) {
            return false;
        }

        if (!(upgradedBlock instanceof AbstractCraftingUnitBlock<?>)) {
            LOG.warn("Upgraded block for crafting unit upgrade with {} is not a crafting block: {}",
                    heldItem, upgradedBlock);
            return false;
        }

        if (upgradedBlock == state.getBlock()) {
            return false;
        }

        // If Upgrading is possible - but disassembly isn't, this will still make the hand animation play.
        if (level.isRemote) {
            return true;
        }

        var newState = upgradedBlock.getDefaultState();

        // Crafting Unit doesn't have a disassembly recipe, so we can ignore the drops.
        EnumActionResult result = state.getBlock() == AEBlocks.CRAFTING_UNIT.block()
                ? this.transform(level, pos, newState) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL
                : this.removeUpgrade(level, player, pos, newState);

        if (result == EnumActionResult.FAIL)
            return false;
        // Makes sure Crafting Monitors are looking at the player (the facing is kept by the block entity in 1.12.2)
        if (result == EnumActionResult.SUCCESS && level.getTileEntity(pos) instanceof IOrientationHolder holder
                && !getOrientationStrategy().getProperties().isEmpty()) {
            holder.setOrientation(BlockOrientation.get(hit.sideHit));
        }
        // Pass => Crafting Unit is busy!
        if (result == EnumActionResult.PASS)
            return true;
        if (!player.capabilities.isCreativeMode) {
            heldItem.shrink(1);
        }
        return true;
    }

    public EnumActionResult removeUpgrade(World level, EntityPlayer player, BlockPos pos, IBlockState newState) {
        if (this.type == CraftingUnitType.UNIT || level.isRemote)
            return EnumActionResult.FAIL;

        var removedUpgrade = CraftingUnitTransformRecipe.getRemovedUpgrade(level, this);
        if (removedUpgrade.isEmpty()) {
            return EnumActionResult.FAIL;
        }

        var cb = this.getTileEntity(level, pos);
        if (cb != null && cb.getCluster() != null && cb.getCluster().isBusy()) {
            player.sendStatusMessage(TextUtil.withColor(PlayerMessages.CraftingCpuBusy.text(), 0xFF1F1F), true);
            return EnumActionResult.PASS;
        }

        if (!this.transform(level, pos, newState)) {
            return EnumActionResult.FAIL;
        }

        Platform.placeItemBackInInventory(player, removedUpgrade);

        return EnumActionResult.SUCCESS;
    }

    private boolean transform(World level, BlockPos pos, IBlockState state) {
        if (level.isRemote || !level.setBlockToAir(pos) || !level.setBlockState(pos, state, 3)) {
            return false;
        }

        level.playSound(
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM,
                SoundCategory.BLOCKS,
                0.5f,
                1f);
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
