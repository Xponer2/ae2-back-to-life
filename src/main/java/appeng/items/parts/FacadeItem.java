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

package appeng.items.parts;

import appeng.util.Platform;

import appeng.util.ItemProperties;

import appeng.core.CreativeTabOutput;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import appeng.api.ids.AEComponents;
import appeng.api.ids.AETags;
import appeng.api.implementations.items.IFacadeItem;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.facade.FacadePart;
import appeng.items.AEBaseItem;
import appeng.util.UseOnContext;
import net.minecraft.util.EnumBlockRenderType;

public class FacadeItem extends AEBaseItem implements IFacadeItem {

    public FacadeItem(ItemProperties properties) {
        super(properties);
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (stack.getItem() != this) {
            return EnumActionResult.PASS;
        }

        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();

        var facade = createPartFromItemStack(stack, context.getClickedFace());
        if (facade == null || !placeFacade(facade, level, pos)) {
            return EnumActionResult.PASS;
        }

        if (!level.isRemote && player != null && !player.isCreative()) {
            stack.grow(-1);
            if (stack.isEmpty()) {
                player.setHeldItem(context.getHand(), ItemStack.EMPTY);
            }
        }

        return EnumActionResult.SUCCESS;
    }

    public static boolean canPlaceFacade(IPartHost host, IFacadePart facade) {
        // Can only place a facade on cables if there's actually a cable at the center to hold them
        if (host.getPart(null) == null) {
            return false;
        }

        return host.getFacadeContainer().canAddFacade(facade);
    }

    private static boolean placeFacade(FacadePart facade, World level, BlockPos blockPos) {
        var host = PartHelper.getPartHost(level, blockPos);
        if (host == null) {
            return false;
        }

        if (!canPlaceFacade(host, facade)) {
            return false;
        }

        if (!host.getFacadeContainer().addFacade(facade)) {
            return false;
        }

        // Play a placement sound of the underlying block
        IBlockState blockState = facade.getBlockState();
        SoundType soundType = blockState.getBlock().getSoundType();
        level.playSound(null, blockPos, soundType.getPlaceSound(), SoundCategory.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);

        host.markForSave();
        host.markForUpdate();
        return true;
    }

    public static IFacadePart createFacade(ItemStack held, EnumFacing side) {
        if (held.getItem() instanceof IFacadeItem) {
            return ((IFacadeItem) held.getItem()).createPartFromItemStack(held, side);
        }

        return null;
    }

    @Override
    public ITextComponent getName(ItemStack is) {
        try {
            final ItemStack in = this.getTextureItem(is);
            if (!in.isEmpty()) {
                return super.getName(is).createCopy().appendText(" - ").appendSibling(Platform.getHoverName(in));
            }
        } catch (Throwable ignored) {

        }

        return super.getName(is);
    }

    @Override
    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // Don't show in creative mode, since it's not useful without NBT
    }

    public ItemStack createFacadeForItem(ItemStack itemStack, boolean returnItem) {
        if (itemStack.isEmpty() || itemStack.hasTagCompound()
                || !(itemStack.getItem() instanceof ItemBlock blockItem)) {
            return ItemStack.EMPTY;
        }

        Block block = blockItem.getBlock();
        if (block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }

        IBlockState blockState = getBlockState(block, blockItem, itemStack.getMetadata());

        final boolean isWhiteListed = AETags.FACADE_BLOCK_WHITELIST.contains(block);
        final boolean isModel = blockState.getRenderType() == EnumBlockRenderType.MODEL;

        final IBlockState defaultState = blockState;
        final boolean isBlockEntity = block.hasTileEntity(defaultState);
        final boolean isFullCube = defaultState.isFullCube();

        final boolean isBlockEntityAllowed = !isBlockEntity || isWhiteListed;
        final boolean isBlockAllowed = isFullCube || isWhiteListed;

        if (isModel && isBlockEntityAllowed && isBlockAllowed) {
            if (returnItem) {
                return itemStack;
            }

            return createFacadeForItemUnchecked(itemStack);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack createFacadeForItemUnchecked(ItemStack itemStack) {
        var is = new ItemStack(this);
        AEComponents.FACADE_ITEM.set(is, itemStack.getItem());
        if (itemStack.getMetadata() != 0) {
            AEComponents.FACADE_ITEM_META.set(is, itemStack.getMetadata());
        }
        return is;
    }

    @Override
    public FacadePart createPartFromItemStack(ItemStack is, EnumFacing side) {
        final ItemStack in = this.getTextureItem(is);
        if (!in.isEmpty()) {
            return new FacadePart(getTextureBlockState(is), side);
        }
        return null;
    }

    @Override
    public ItemStack getTextureItem(ItemStack is) {
        var baseItem = AEComponents.FACADE_ITEM.get(is);

        if (baseItem == null) {
            return ItemStack.EMPTY;
        }

        var meta = AEComponents.FACADE_ITEM_META.get(is);
        return new ItemStack(baseItem, 1, meta != null ? meta : 0);
    }

    @Override
    public IBlockState getTextureBlockState(ItemStack is) {

        ItemStack baseItemStack = this.getTextureItem(is);

        if (baseItemStack.isEmpty()) {
            return Blocks.GLASS.getDefaultState();
        }

        Block block = Block.getBlockFromItem(baseItemStack.getItem());

        if (block == Blocks.AIR || !(baseItemStack.getItem() instanceof ItemBlock blockItem)) {
            return Blocks.GLASS.getDefaultState();
        }

        return getBlockState(block, blockItem, baseItemStack.getMetadata());
    }

    /**
     * The block state that the item places (the variant given by the metadata).
     */
    @SuppressWarnings("deprecation")
    private static IBlockState getBlockState(Block block, ItemBlock blockItem, int itemMeta) {
        try {
            return block.getStateFromMeta(blockItem.getMetadata(itemMeta));
        } catch (RuntimeException e) {
            return block.getDefaultState();
        }
    }
}
