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

package appeng.blockentity.storage;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.inventories.InternalInventory;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.core.definitions.BlockEntityType;
import appeng.menu.implementations.SkyChestMenu;
import appeng.util.inv.AppEngInternalInventory;

@SuppressWarnings("JavadocReference")
public class SkyChestBlockEntity extends AEBaseInvBlockEntity implements ClientTickingBlockEntity {

    private static final int EVENT_OPENERS = 1;
    private static final int RECHECK_DELAY = 5;

    private final AppEngInternalInventory inv = new AppEngInternalInventory(this, 9 * 4);

    // Server: players looking into the chest (1.21 ContainerOpenersCounter)
    private int openCount;

    // Client: lid animation (1.21 ChestLidController)
    private boolean shouldBeOpen;
    private float openness;
    private float oOpenness;

    public SkyChestBlockEntity() {
        super();
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    public void startOpen(EntityPlayer player) {
        if (!this.tileEntityInvalid && !player.isSpectator()) {
            if (this.openCount < 0) {
                this.openCount = 0;
            }
            setOpenCount(this.openCount + 1);
        }
    }

    public void stopOpen(EntityPlayer player) {
        if (!this.tileEntityInvalid && !player.isSpectator()) {
            setOpenCount(this.openCount - 1);
        }
    }

    private void setOpenCount(int count) {
        var previous = this.openCount;
        this.openCount = count;
        var level = getLevel();
        if (level == null || level.isRemote || previous == count) {
            return;
        }
        if (previous == 0 && count > 0) {
            playSound(SoundEvents.BLOCK_CHEST_OPEN);
        } else if (previous > 0 && count == 0) {
            playSound(SoundEvents.BLOCK_CHEST_CLOSE);
        }
        level.addBlockEvent(this.pos, getBlockType(), EVENT_OPENERS, count);
        if (count > 0) {
            // Re-check later in case a player leaves without closing the menu
            level.scheduleUpdate(this.pos, getBlockType(), RECHECK_DELAY);
        }
    }

    private void playSound(SoundEvent event) {
        var x = this.pos.getX() + 0.5D;
        var y = this.pos.getY() + 0.5D;
        var z = this.pos.getZ() + 0.5D;
        this.world.playSound(null, x, y, z, event, SoundCategory.BLOCKS, 0.5F,
                this.world.rand.nextFloat() * 0.1F + 0.9F);
    }

    private boolean isOwnContainer(EntityPlayer player) {
        if (player.openContainer instanceof SkyChestMenu menu) {
            return menu.getChest() == this;
        } else {
            return false;
        }
    }

    @Override
    public void clientTick() {
        this.oOpenness = this.openness;
        if (!this.shouldBeOpen && this.openness > 0.0F) {
            this.openness = Math.max(this.openness - 0.1F, 0.0F);
        } else if (this.shouldBeOpen && this.openness < 1.0F) {
            this.openness = Math.min(this.openness + 0.1F, 1.0F);
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == EVENT_OPENERS) {
            this.shouldBeOpen = type > 0;
            return true;
        } else {
            return super.receiveClientEvent(id, type);
        }
    }

    public float getOpenNess(float partialTicks) {
        return this.oOpenness + (this.openness - this.oOpenness) * partialTicks;
    }

    /**
     * Counts the players that still look into this chest (scheduled block tick while it is open).
     */
    public void recheckOpen() {
        if (this.tileEntityInvalid || this.world == null) {
            return;
        }
        var box = new AxisAlignedBB(this.pos).grow(5.0);
        int count = 0;
        for (var player : this.world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
            if (isOwnContainer(player)) {
                count++;
            }
        }
        setOpenCount(count);
    }

    @Override
    public EnumActionResult disassembleWithWrench(EntityPlayer player, World level, RayTraceResult hitResult,
            ItemStack wrench) {
        return EnumActionResult.FAIL;
    }
}
