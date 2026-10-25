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

package appeng.debug;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.core.AppEng;
import appeng.util.InteractionUtil;

public class CubeGeneratorBlockEntity extends AEBaseBlockEntity implements ServerTickingBlockEntity {

    private int size = 3;
    private ItemStack is = ItemStack.EMPTY;
    private int countdown = 20 * 10;
    private EntityPlayer who = null;

    public CubeGeneratorBlockEntity() {
        super();
    }

    @Override
    public void serverTick() {
        if (!this.is.isEmpty()) {
            this.countdown--;

            if (this.countdown % 20 == 0) {
                AppEng.instance().getPlayers().forEach(p -> {
                    p.sendMessage(new TextComponentString("Spawning in... " + this.countdown / 20));
                });
            }

            if (this.countdown <= 0) {
                this.spawn();
            }
        }
    }

    private void spawn() {
        this.world.setBlockToAir(this.pos);

        final EnumFacing side = EnumFacing.UP;

        final int half = (int) Math.floor(this.size / 2);

        // Items are used by a player holding them, a fake one places the blocks
        var placer = FakePlayerFactory.getMinecraft((WorldServer) this.world);
        for (int y = 0; y < this.size; y++) {
            for (int x = -half; x < half; x++) {
                for (int z = -half; z < half; z++) {
                    final BlockPos p = this.pos.add(x, y - 1, z);
                    var stack = this.is.copy();
                    placer.setHeldItem(EnumHand.MAIN_HAND, stack);
                    // Placing against an air block replaces it, so the block ends up at p
                    stack.onItemUse(placer, this.world, p, EnumHand.MAIN_HAND, side, 0.5f, 1.0f, 0.5f);
                }
            }
        }
        placer.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
    }

    void click(EntityPlayer player) {
        if (!isClientSide()) {
            final ItemStack hand = player.inventory.getCurrentItem();
            this.who = player;

            if (hand.isEmpty()) {
                this.is = ItemStack.EMPTY;

                if (InteractionUtil.isInAlternateUseMode(player)) {
                    this.size--;
                } else {
                    this.size++;
                }

                if (this.size < 3) {
                    this.size = 3;
                }
                if (this.size > 64) {
                    this.size = 64;
                }

                player.sendMessage(new TextComponentString("Size: " + this.size));
            } else {
                this.countdown = 20 * 10;
                this.is = hand;
            }
        }
    }
}
