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

package appeng.blockentity.crafting;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import appeng.api.components.DataComponentMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.ids.AEComponents;
import appeng.api.networking.IGridNodeListener;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AECableType;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.BlockEntityType;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.SettingsFrom;

public class PatternProviderBlockEntity extends AENetworkedBlockEntity implements PatternProviderLogicHost {
    protected final PatternProviderLogic logic = createLogic();

    public PatternProviderBlockEntity() {
        super();
    }

    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.logic.onMainNodeStateChanged();
    }

    private PushDirection getPushDirection() {
        return getBlockState().getValue(PatternProviderBlock.PUSH_DIRECTION);
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        // In omnidirectional mode, every side is grid-connectable
        var pushDirection = getPushDirection().getDirection();
        if (pushDirection == null) {
            return EnumSet.allOf(EnumFacing.class);
        }

        // Otherwise all sides *except* the target side are connectable
        return EnumSet.complementOf(EnumSet.of(pushDirection));
    }

    @Override
    public void addAdditionalDrops(World level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.logic.updatePatterns();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.logic.writeToNBT(data);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.logic.readFromNBT(data);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.SMART;
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        var pushDirection = getPushDirection();
        if (pushDirection.getDirection() == null) {
            return EnumSet.allOf(EnumFacing.class);
        } else {
            return EnumSet.of(pushDirection.getDirection());
        }
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(AEBlocks.PATTERN_PROVIDER.stack());
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder,
            @Nullable EntityPlayer player) {
        super.exportSettings(mode, builder, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.exportSettings(builder);

            var pushDirection = getPushDirection();
            AEComponents.EXPORTED_PUSH_DIRECTION.set(builder, pushDirection);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input,
            @Nullable EntityPlayer player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            logic.importSettings(input, player);

            // Restore push direction blockstate
            var pushDirection = AEComponents.EXPORTED_PUSH_DIRECTION.get(input);
            if (pushDirection != null) {
                setBlockStateProperty(PatternProviderBlock.PUSH_DIRECTION, pushDirection);
            }
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return AEBlocks.PATTERN_PROVIDER.stack();
    }

    @Override
    public void setBlockState(IBlockState state) {
        super.setBlockState(state);
        onGridConnectableSidesChanged();
    }
}
