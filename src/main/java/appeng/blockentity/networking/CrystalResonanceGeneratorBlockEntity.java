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

package appeng.blockentity.networking;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import appeng.api.networking.energy.IPassiveEnergyGenerator;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.AEConfig;
import appeng.core.definitions.BlockEntityType;
import appeng.core.network.RegistryFriendlyByteBuf;

public class CrystalResonanceGeneratorBlockEntity extends AENetworkedBlockEntity {
    // This needs to be synchronized to allow visual indication / Jade tooltips
    private boolean suppressed;

    public CrystalResonanceGeneratorBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos,
            IBlockState blockState) {
        super();
        // we're supposed to be *generating* power, so no consumption
        getMainNode().setIdlePowerUsage(0);
        getMainNode().addService(IPassiveEnergyGenerator.class, new IPassiveEnergyGenerator() {
            @Override
            public double getRate() {
                return AEConfig.instance().getCrystalResonanceGeneratorRate();
            }

            @Override
            public boolean isSuppressed() {
                return CrystalResonanceGeneratorBlockEntity.this.suppressed;
            }

            @Override
            public void setSuppressed(boolean suppressed) {
                if (suppressed != CrystalResonanceGeneratorBlockEntity.this.suppressed) {
                    CrystalResonanceGeneratorBlockEntity.this.suppressed = suppressed;
                    markForUpdate();
                }
            }
        });
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        super.readFromStream(data);
        this.suppressed = data.readBoolean();
        return false;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.suppressed);
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        data.setBoolean("suppressed", this.suppressed);
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        this.suppressed = data.getBoolean("suppressed");
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(orientation.getSide(RelativeSide.BACK));
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.SMART;
    }
}
