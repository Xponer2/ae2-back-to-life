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

package appeng.blockentity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.BlockEntityType;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

public abstract class AEBaseInvBlockEntity extends AEBaseBlockEntity implements InternalInventoryHost {

    public AEBaseInvBlockEntity() {
        super();
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        var inv = this.getInternalInventory();
        if (inv != InternalInventory.empty()) {
            var opt = data.getCompoundTag("inv");
            for (int x = 0; x < inv.size(); x++) {
                var item = opt.getCompoundTag("item" + x);
                inv.setItemDirect(x, new ItemStack(item));
            }
        }
    }

    public abstract InternalInventory getInternalInventory();

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        var inv = this.getInternalInventory();
        if (inv != InternalInventory.empty()) {
            final NBTTagCompound opt = new NBTTagCompound();
            for (int x = 0; x < inv.size(); x++) {
                var is = inv.getStackInSlot(x);
                opt.setTag("item" + x, is.writeToNBT(new NBTTagCompound()));
            }
            data.setTag("inv", opt);
        }
    }

    @Override
    public void addAdditionalDrops(World level, BlockPos pos, List<ItemStack> drops) {
        var inv = getInternalInventory();
        for (var stack : inv) {
            var genericStack = GenericStack.unwrapItemStack(stack);
            if (genericStack != null) {
                genericStack.what().addDrops(
                        genericStack.amount(),
                        drops,
                        level,
                        pos);
            } else {
                drops.add(stack);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        getInternalInventory().clear();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.saveChanges();
    }

    protected InternalInventory getExposedInventoryForSide(EnumFacing side) {
        return this.getInternalInventory();
    }

    @Nullable
    public IItemHandler getExposedItemHandler(@Nullable EnumFacing side) {
        if (side == null) {
            return getInternalInventory().toItemHandler();
        } else {
            var exposed = getExposedInventoryForSide(side);
            // If the inventory has 0 slots, it's probably a dummy.
            // Return null to avoid pipe connections to it.
            // isEmpty checks for stacks, use size to only check the slot count.
            // noinspection SizeReplaceableByIsEmpty
            return exposed.size() == 0 ? null : exposed.toItemHandler();
        }
    }

}
