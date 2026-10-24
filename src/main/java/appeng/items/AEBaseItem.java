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

package appeng.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.ItemLike;
import appeng.api.util.TooltipComponent;
import appeng.core.CreativeTabOutput;
import appeng.util.ItemProperties;
import appeng.util.ItemTooltipContext;
import appeng.util.UseOnContext;

/**
 * Base class of AE2 items. Bridges the Minecraft 1.21 item API ({@link #use}, {@link #useOn},
 * {@link #appendHoverText}, {@link #getName}, durability bars, ...) to Minecraft 1.12.2.
 */
public abstract class AEBaseItem extends Item implements ItemLike {
    protected final ItemProperties properties;

    public AEBaseItem(ItemProperties properties) {
        this.properties = properties;
        properties.applyTo(this);
    }

    @Override
    public Item asItem() {
        return this;
    }

    public ItemProperties getProperties() {
        return properties;
    }

    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        output.accept(this);
    }

    @Override
    public String toString() {
        String regName = this.getRegistryName() != null ? this.getRegistryName().getPath() : "unregistered";
        return this.getClass().getSimpleName() + "[" + regName + "]";
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Naming
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Modern translation key ({@code item.ae2.<path>}), used directly by AE2's lang files.
     */
    public String getDescriptionId() {
        return makeDescriptionId(getRegistryName());
    }

    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    public static String makeDescriptionId(@Nullable ResourceLocation id) {
        return id == null ? "item.ae2.unknown" : "item." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    @Override
    public String getTranslationKey() {
        return getDescriptionId();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getDescriptionId(stack);
    }

    /**
     * Modern getName(ItemStack).
     */
    public ITextComponent getName(ItemStack stack) {
        return new TextComponentTranslation(getDescriptionId(stack));
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return getName(stack).getFormattedText();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Use
    // ---------------------------------------------------------------------------------------------------------------

    public ActionResult<ItemStack> use(World level, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        return use(worldIn, playerIn, handIn);
    }

    public EnumActionResult useOn(UseOnContext context) {
        return EnumActionResult.PASS;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        return useOn(UseOnContext.of(player, worldIn, pos, hand, facing, hitX, hitY, hitZ));
    }

    /**
     * NeoForge's onItemUseFirst(ItemStack, UseOnContext).
     */
    public EnumActionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return EnumActionResult.PASS;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        return onItemUseFirst(player.getHeldItem(hand), UseOnContext.of(player, world, pos, hand, side, hitX, hitY,
                hitZ));
    }

    public void inventoryTick(ItemStack stack, World level, Entity entity, int slotId, boolean isSelected) {
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Tooltips
    // ---------------------------------------------------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> lines,
            ITooltipFlag advancedTooltips) {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        var lines = new ArrayList<ITextComponent>();
        appendHoverText(stack, ItemTooltipContext.of(worldIn), lines, flagIn);
        for (var line : lines) {
            tooltip.add(line.getFormattedText());
        }
    }

    /**
     * Modern Item.getTooltipImage: an image shown in the tooltip (rendered by the client tooltip hook).
     */
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Durability bar & glint
    // ---------------------------------------------------------------------------------------------------------------

    public boolean isBarVisible(ItemStack stack) {
        return stack.isItemDamaged();
    }

    /**
     * @return 0..13 like modern Minecraft.
     */
    public int getBarWidth(ItemStack stack) {
        if (stack.getMaxDamage() <= 0) {
            return 0;
        }
        return Math.round(13.0F - (float) stack.getItemDamage() * 13.0F / (float) stack.getMaxDamage());
    }

    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, getBarWidth(stack) / 13.0F);
        return java.awt.Color.HSBtoRGB(f / 3.0F, 1.0F, 1.0F) & 0xFFFFFF;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return isBarVisible(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - getBarWidth(stack) / 13.0;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return getBarColor(stack);
    }

    public boolean isFoil(ItemStack stack) {
        return stack.isItemEnchanted();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack) {
        return isFoil(stack);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Creative tab & capabilities
    // ---------------------------------------------------------------------------------------------------------------

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
