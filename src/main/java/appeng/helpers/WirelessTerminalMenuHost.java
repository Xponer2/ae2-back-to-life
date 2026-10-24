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

package appeng.helpers;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.HotkeyAction;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.SupplierStorage;
import appeng.api.util.IConfigManager;
import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.PlayerSource;
import appeng.me.storage.NullInventory;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;

public class WirelessTerminalMenuHost<T extends WirelessTerminalItem> extends ItemMenuHost<T>
        implements IPortableTerminal, IActionHost, KeyTypeSelectionHost {

    private final BiConsumer<EntityPlayer, ISubMenu> returnToMainMenu;
    @Nullable
    private IWirelessAccessPoint currentAccessPoint;
    /**
     * The distance to the currently connected access point in blocks.
     */
    protected double currentDistanceFromGrid = Double.MAX_VALUE;
    /**
     * How far away are we from losing signal.
     */
    protected double currentRemainingRange = Double.MIN_VALUE;
    private final MEStorage storage;
    private ILinkStatus linkStatus = ILinkStatus.ofDisconnected();

    public WirelessTerminalMenuHost(T item, EntityPlayer player, ItemMenuHostLocator locator,
            BiConsumer<EntityPlayer, ISubMenu> returnToMainMenu) {
        super(item, player, locator);
        this.returnToMainMenu = returnToMainMenu;

        this.storage = new SupplierStorage(new StackDependentSupplier<>(
                this::getItemStack, this::getStorageFromStack));

        updateConnectedAccessPoint();
        updateLinkStatus();
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return linkStatus;
    }

    @Nullable
    private MEStorage getStorageFromStack(ItemStack stack) {
        var targetGrid = getLinkedGrid(stack);
        if (targetGrid != null) {
            return targetGrid.getStorageService().getInventory();
        }
        return NullInventory.of();
    }

    @Nullable
    private IGrid getLinkedGrid(ItemStack stack) {
        return getItem().getLinkedGrid(stack, getPlayer().world, null);
    }

    @Override
    public MEStorage getInventory() {
        return this.storage;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        final double extracted = Math.min(amt, getItem().getAECurrentPower(getItemStack()));

        if (mode == Actionable.SIMULATE) {
            return extracted;
        }

        return getItem().usePower(getPlayer(), extracted, getItemStack()) ? extracted : 0;
    }

    @Override
    public IConfigManager getConfigManager() {
        return getItem().getConfigManager(this::getItemStack);
    }

    @Override
    public KeyTypeSelection getKeyTypeSelection() {
        return KeyTypeSelection.forStack(getItemStack(), keyType -> true);
    }

    @Override
    public IGridNode getActionableNode() {
        if (this.currentAccessPoint != null) {
            return this.currentAccessPoint.getActionableNode();
        }
        return null;
    }

    protected void updateConnectedAccessPoint() {
        this.currentAccessPoint = null;
        this.currentDistanceFromGrid = Double.MAX_VALUE;
        this.currentRemainingRange = Double.MIN_VALUE;

        var targetGrid = getLinkedGrid(getItemStack());
        if (targetGrid != null) {
            @Nullable
            IWirelessAccessPoint bestWap = null;
            double bestSqDistance = Double.MAX_VALUE;
            double bestSqRemainingRange = Double.MIN_VALUE;

            // Find closest WAP
            for (var wap : targetGrid.getMachines(WirelessAccessPointBlockEntity.class)) {
                var signal = getAccessPointSignal(wap);

                // If the WAP is not suitable then MAX_VALUE will be returned and the check will fail
                if (signal.distanceSquared < bestSqDistance) {
                    bestSqDistance = signal.distanceSquared;
                    bestWap = wap;
                }
                // There may be access points with larger range that are farther away,
                // but those would have larger energy consumption
                if (signal.remainingRangeSquared > bestSqRemainingRange) {
                    bestSqRemainingRange = signal.remainingRangeSquared;
                }
            }

            // If no WAP is found this will work too
            this.currentAccessPoint = bestWap;
            this.currentDistanceFromGrid = Math.sqrt(bestSqDistance);
            this.currentRemainingRange = Math.sqrt(bestSqRemainingRange);
        }

    }

    /**
     * @return square distance to WAP if the WAP can be used, or {@link Double#MAX_VALUE} if it cannot be used.
     */
    protected AccessPointSignal getAccessPointSignal(IWirelessAccessPoint wap) {
        double rangeLimit = wap.getRange();
        rangeLimit *= rangeLimit;

        var dc = wap.getLocation();

        if (dc.getLevel() == this.getPlayer().world) {
            var offX = dc.getPos().posX - this.getPlayer().posX;
            var offY = dc.getPos().posY - this.getPlayer().posY;
            var offZ = dc.getPos().posZ - this.getPlayer().posZ;

            double r = offX * offX + offY * offY + offZ * offZ;
            if (r < rangeLimit && wap.isActive()) {
                return new AccessPointSignal(r, rangeLimit - r);
            }
        }

        return new AccessPointSignal(Double.MAX_VALUE, Double.MIN_VALUE);
    }

    public final static class AccessPointSignal {
        private final double distanceSquared;
        private final double remainingRangeSquared;

        public AccessPointSignal(double distanceSquared, double remainingRangeSquared) {
            this.distanceSquared = distanceSquared;
            this.remainingRangeSquared = remainingRangeSquared;
        }

        public double distanceSquared() {
            return distanceSquared;
        }

        public double remainingRangeSquared() {
            return remainingRangeSquared;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AccessPointSignal)) return false;
            AccessPointSignal that = (AccessPointSignal) o;
            return Double.compare(distanceSquared, that.distanceSquared) == 0
                    && Double.compare(remainingRangeSquared, that.remainingRangeSquared) == 0;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(distanceSquared, remainingRangeSquared);
        }

        @Override
        public String toString() {
            return "AccessPointSignal[" + "distanceSquared=" + distanceSquared + ", " + "remainingRangeSquared=" + remainingRangeSquared + "]";
        }

    }

    @Override
    public void tick() {
        updateConnectedAccessPoint();
        consumeIdlePower(Actionable.MODULATE);
        updateLinkStatus();
    }

    /**
     * Recalculate the current {@linkplain #getLinkStatus() link status}.
     */
    protected void updateLinkStatus() {
        // Update the link status after checking for range + power
        if (!consumeIdlePower(Actionable.SIMULATE)) {
            this.linkStatus = ILinkStatus.ofDisconnected(GuiText.OutOfPower.text());
        } else if (currentAccessPoint != null) {
            this.linkStatus = ILinkStatus.ofConnected();
        } else {
            MutableObject<ITextComponent> errorHolder = new MutableObject<>();
            if (getItem().getLinkedGrid(getItemStack(), getPlayer().world, errorHolder::setValue) == null) {
                this.linkStatus = ILinkStatus.ofDisconnected(errorHolder.getValue());
            } else {
                // If a grid exists, but no access point, we're out of range
                this.linkStatus = ILinkStatus.ofDisconnected(PlayerMessages.OutOfRange.text());
            }
        }
    }

    @Override
    protected double getPowerDrainPerTick() {
        if (currentAccessPoint != null && currentDistanceFromGrid < Double.MAX_VALUE) {
            return AEConfig.instance().wireless_getDrainRate(currentDistanceFromGrid);
        } else {
            return 0.0;
        }
    }

    @Override
    public void returnToMainMenu(EntityPlayer player, ISubMenu subMenu) {
        returnToMainMenu.accept(player, subMenu);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getItemStack();
    }

    public String getCloseHotkey() {
        return HotkeyAction.WIRELESS_TERMINAL;
    }

    @Override
    public long insert(EntityPlayer player, AEKey what, long amount, Actionable mode) {
        // We do not know the real link-status on the client-side
        if (isClientSide()) {
            return 0;
        }

        if (getLinkStatus().connected()) {
            return StorageHelper.poweredInsert(this, getInventory(), what, amount, new PlayerSource(player), mode);
        } else {
            var statusText = getLinkStatus().statusDescription();
            if (statusText != null && !mode.isSimulate()) {
                player.sendStatusMessage(statusText, false);
            }
            return 0;
        }
    }
}
