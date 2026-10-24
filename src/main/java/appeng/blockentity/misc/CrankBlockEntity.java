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

package appeng.blockentity.misc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import appeng.api.implementations.blockentities.ICrankable;
import appeng.block.misc.CrankBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;

public class CrankBlockEntity extends AEBaseBlockEntity implements ServerTickingBlockEntity, ClientTickingBlockEntity {

    public static final int POWER_PER_CRANK_TURN = 160;
    private final int ticksPerRotation = 18;

    // sided values..
    private float visibleRotation = 0; // This is in degrees
    private int charge = 0;

    private int hits = 0;
    private int rotation = 0;

    public CrankBlockEntity() {
        super();
    }

    @Nullable
    private ICrankable getCrankable() {
        if (isClientSide()) {
            return null;
        }

        var blockState = getBlockState();
        if (blockState.getBlock() instanceof CrankBlock crankBlock) {
            return crankBlock.getCrankable(blockState, level, getPos());
        }
        return null;
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        this.rotation = data.readInt();
        return c;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeInt(this.rotation);
    }

    /**
     * return true if this should count towards stats.
     */
    public boolean power() {
        if (isClientSide()) {
            return false;
        }

        if (this.rotation < 3) {
            var crankable = this.getCrankable();
            if (crankable != null) {
                if (crankable.canTurn()) {
                    this.hits = 0;
                    this.rotation += this.ticksPerRotation;
                    this.markForUpdate();
                    return true;
                } else {
                    this.hits++;
                    if (this.hits > 10) {
                        level.destroyBlock(this.getPos(), false);
                    }
                }
            }
        }

        return false;
    }

    public float getVisibleRotation() {
        return this.visibleRotation;
    }

    private void setVisibleRotation(float visibleRotation) {
        this.visibleRotation = visibleRotation;
    }

    @Override
    public void clientTick() {
        tick();
    }

    @Override
    public void serverTick() {
        tick();
    }

    private void tick() {
        if (this.rotation > 0) {
            this.setVisibleRotation(this.getVisibleRotation() - 360.0f / this.ticksPerRotation);
            this.charge++;
            if (this.charge >= this.ticksPerRotation) {
                this.charge -= this.ticksPerRotation;
                var g = this.getCrankable();
                if (g != null) {
                    g.applyTurn();
                }
            }

            this.rotation--;
        }
    }
}
