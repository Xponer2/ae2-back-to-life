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

package appeng.items.materials;

import appeng.util.ItemProperties;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.PlayerMessages;
import appeng.items.AEBaseItem;
import appeng.util.InteractionUtil;
import appeng.util.UseOnContext;

public class UpgradeCardItem extends AEBaseItem {

    public UpgradeCardItem(ItemProperties properties) {
        super(properties);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> lines,
            ITooltipFlag advancedTooltips) {
        super.appendHoverText(stack, context, lines, advancedTooltips);

        var supportedBy = Upgrades.getTooltipLinesForCard(this);
        if (!supportedBy.isEmpty()) {
            lines.add(ButtonToolTips.SupportedBy.text());
            lines.addAll(supportedBy);
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        EntityPlayer player = context.getPlayer();
        EnumHand hand = context.getHand();
        if (player != null && InteractionUtil.isInAlternateUseMode(player)) {
            final TileEntity te = context.getLevel().getTileEntity(context.getClickedPos());
            IUpgradeInventory upgrades = null;

            if (te instanceof IPartHost) {
                final SelectedPart sp = ((IPartHost) te).selectPartWorld(context.getClickLocation());
                if (sp.part instanceof IUpgradeableObject) {
                    upgrades = ((IUpgradeableObject) sp.part).getUpgrades();
                }
            } else if (te instanceof IUpgradeableObject) {
                upgrades = ((IUpgradeableObject) te).getUpgrades();
            }

            if (upgrades != null && upgrades.size() > 0) {
                var heldStack = player.getHeldItem(hand);

                boolean isFull = true;
                for (int i = 0; i < upgrades.size(); i++) {
                    if (upgrades.getStackInSlot(i).isEmpty()) {
                        isFull = false;
                        break;
                    }
                }
                if (isFull) {
                    player.sendStatusMessage(PlayerMessages.MaxUpgradesInstalled.text(), true);
                    return EnumActionResult.FAIL;
                }

                var maxInstalled = upgrades.getMaxInstalled(ItemLike.of(heldStack.getItem()));
                var installed = upgrades.getInstalledUpgrades(ItemLike.of(heldStack.getItem()));
                if (maxInstalled <= 0) {
                    player.sendStatusMessage(PlayerMessages.UnsupportedUpgrade.text(), true);
                    return EnumActionResult.FAIL;
                } else if (installed >= maxInstalled) {
                    player.sendStatusMessage(PlayerMessages.MaxUpgradesOfTypeInstalled.text(), true);
                    return EnumActionResult.FAIL;
                }

                if (player.getEntityWorld().isRemote) {
                    return EnumActionResult.PASS;
                }

                player.setItemInHand(hand, upgrades.addItems(heldStack));
                return EnumActionResult.SUCCESS;
            }
        }

        return super.onItemUseFirst(stack, context);
    }
}
