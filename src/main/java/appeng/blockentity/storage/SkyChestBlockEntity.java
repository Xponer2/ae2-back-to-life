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
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.inventories.InternalInventory;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.core.definitions.BlockEntityType;
import appeng.menu.implementations.SkyChestMenu;
import appeng.util.inv.AppEngInternalInventory;

@SuppressWarnings("JavadocReference")
@SideOnly(value = Side.CLIENT, _interface = LidBlockEntity.class)
public class SkyChestBlockEntity extends AEBaseInvBlockEntity implements ClientTickingBlockEntity, LidBlockEntity {

    private final AppEngInternalInventory inv = new AppEngInternalInventory(this, 9 * 4);

    private final ChestLidController chestLidController = new ChestLidController();

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        protected void onOpen(World level, BlockPos pos, IBlockState state) {
            playSound(level, pos, SoundEvents.CHEST_OPEN);
        }

        protected void onClose(World level, BlockPos pos, IBlockState state) {
            playSound(level, pos, SoundEvents.CHEST_CLOSE);
        }

        private void playSound(World level, BlockPos pos, SoundEvent event) {
            var x = pos.getX() + 0.5D;
            var y = pos.getY() + 0.5D;
            var z = pos.getZ() + 0.5D;
            level.playSound(null, x, y, z, event, SoundCategory.BLOCKS,
                    0.5F, level.rand.nextFloat() * 0.1F + 0.9F);
        }

        protected void openerCountChanged(World level, BlockPos pos, IBlockState state, int p_155364_, int x) {
            level.blockEvent(pos, state.getBlock(), 1, x);
        }

        protected boolean isOwnContainer(EntityPlayer player) {
            if (player.openContainer instanceof SkyChestMenu menu) {
                return menu.getChest() == SkyChestBlockEntity.this;
            } else {
                return false;
            }
        }
    };

    public SkyChestBlockEntity(BlockEntityType<? extends SkyChestBlockEntity> type, BlockPos pos,
            IBlockState blockState) {
        super();
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inv;
    }

    public void startOpen(EntityPlayer player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getPos(), this.getBlockState());
        }
    }

    public void stopOpen(EntityPlayer player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getPos(), this.getBlockState());
        }
    }

    @Override
    public void clientTick() {
        chestLidController.tickLid();
    }

    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.chestLidController.shouldBeOpen(type > 0);
            return true;
        } else {
            return super.triggerEvent(id, type);
        }
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return this.chestLidController.getOpenness(partialTicks);
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getPos(), this.getBlockState());
        }
    }

    @Override
    public EnumActionResult disassembleWithWrench(EntityPlayer player, World level, RayTraceResult hitResult,
            ItemStack wrench) {
        return EnumActionResult.FAIL;
    }
}
