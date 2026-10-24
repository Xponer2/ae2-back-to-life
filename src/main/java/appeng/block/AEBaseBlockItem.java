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

package appeng.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.ItemLike;
import appeng.core.CreativeTabOutput;
import appeng.items.ItemCapabilityProvider;
import appeng.util.ItemProperties;
import appeng.util.ItemTooltipContext;

/**
 * Item of all AE2 blocks. Uses the modern translation keys and passes the full placement state (including the
 * orientation) to {@link AEBaseEntityBlock#onPlacedWithState}.
 */
public class AEBaseBlockItem extends ItemBlock implements ItemLike {
    protected final ItemProperties properties;

    public AEBaseBlockItem(Block id, ItemProperties props) {
        super(id);
        this.properties = props;
        props.applyTo(this);
    }

    @Override
    public net.minecraft.item.Item asItem() {
        return this;
    }

    @Nullable
    public AEBaseBlock getAEBlock() {
        return block instanceof AEBaseBlock aeBlock ? aeBlock : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public final void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
            ITooltipFlag flagIn) {
        var lines = new ArrayList<ITextComponent>();
        this.addCheckedInformation(stack, ItemTooltipContext.of(worldIn), lines, flagIn);
        for (var line : lines) {
            tooltip.add(line.getFormattedText());
        }
    }

    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(ItemStack itemStack, ItemTooltipContext context, List<ITextComponent> toolTip,
            ITooltipFlag advancedTooltips) {
        var aeBlock = getAEBlock();
        if (aeBlock != null) {
            aeBlock.appendHoverText(itemStack, context.level(), toolTip, advancedTooltips);
        }
    }

    @Override
    public boolean isBookEnchantable(final ItemStack itemstack1, final ItemStack itemstack2) {
        return false;
    }

    public String getDescriptionId() {
        var aeBlock = getAEBlock();
        if (aeBlock != null) {
            return aeBlock.getDescriptionId();
        }
        var id = block.getRegistryName();
        return id == null ? "block.ae2.unknown" : "block." + id.getNamespace() + "." + id.getPath();
    }

    public String getDescriptionId(ItemStack is) {
        return getDescriptionId();
    }

    @Override
    public String getTranslationKey() {
        return getDescriptionId();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getDescriptionId(stack);
    }

    public ITextComponent getName(ItemStack stack) {
        return new TextComponentTranslation(getDescriptionId(stack));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return getName(stack).getFormattedText();
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            return false;
        }
        if (block instanceof AEBaseEntityBlock<?> entityBlock && world.getBlockState(pos).getBlock() == block) {
            entityBlock.onPlacedWithState(world, pos, newState);
        }
        return true;
    }

    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        var aeBlock = getAEBlock();
        if (aeBlock != null) {
            aeBlock.addToMainCreativeTab(parameters, output);
        } else {
            output.accept(this);
        }
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            addToMainCreativeTab(CreativeTabOutput.DisplayParameters.INSTANCE, new CreativeTabOutput(items));
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return ItemCapabilityProvider.forStack(stack);
    }
}
