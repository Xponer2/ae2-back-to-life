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

package appeng.block.networking;

import appeng.util.Platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.StringRepresentable;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.networktool.NetworkStatusMenu;
import appeng.util.BlockPlaceContext;

public class ControllerBlock extends AEBaseEntityBlock<ControllerBlockEntity> {

    public enum ControllerBlockState implements StringRepresentable {
        offline, online, conflicted;

        @Override
        public String getSerializedName() {
            return this.name();
        }

    }

    /**
     * Controls the rendering of the controller block (connected texture style). inside_a and inside_b are alternating
     * patterns for a controller that is enclosed by other controllers, and since they are always offline, they do not
     * have the usual sub-states.
     */
    public enum ControllerRenderType implements StringRepresentable {
        block, column_x, column_y, column_z, inside_a, inside_b;

        @Override
        public String getSerializedName() {
            return this.name();
        }

    }

    public static final PropertyEnum<ControllerBlockState> CONTROLLER_STATE = PropertyEnum.create("state",
            ControllerBlockState.class);

    public static final PropertyEnum<ControllerRenderType> CONTROLLER_TYPE = PropertyEnum.create("type",
            ControllerRenderType.class);

    public ControllerBlock() {
        super(metalProps().strength(6));
        this.setDefaultState(this.getDefaultState().withProperty(CONTROLLER_STATE, ControllerBlockState.offline)
                .withProperty(CONTROLLER_TYPE, ControllerRenderType.block));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONTROLLER_STATE);
        builder.add(CONTROLLER_TYPE);
    }

    /**
     * This is called to determine which variant of the controller gets placed based on adjacent blocks.
     */
    @Nullable
    @Override
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return getControllerType(getDefaultState(), context.getLevel(), context.getClickedPos());
    }

    /**
     * 1.21 recomputes the render type when an adjacent block changes; 1.12.2 computes it with the actual state.
     */
    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, ControllerBlockEntity be) {
        var state = super.updateBlockStateFromBlockEntity(currentState, be);
        var level = be.getWorld();
        return level != null ? getControllerType(state, level, be.getPos()) : state;
    }

    private IBlockState getControllerType(IBlockState baseState, IBlockAccess level, BlockPos pos) {
        // Only used for columns, really
        ControllerRenderType type = ControllerRenderType.block;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Detect whether controllers are on both sides of the x, y, and z axes
        final boolean xx = isController(level, x - 1, y, z) && isController(level, x + 1, y, z);
        final boolean yy = isController(level, x, y - 1, z) && isController(level, x, y + 1, z);
        final boolean zz = isController(level, x, y, z - 1) && isController(level, x, y, z + 1);

        if (xx && !yy && !zz) {
            type = ControllerRenderType.column_x;
        } else if (!xx && yy && !zz) {
            type = ControllerRenderType.column_y;
        } else if (!xx && !yy && zz) {
            type = ControllerRenderType.column_z;
        } else if ((xx ? 1 : 0) + (yy ? 1 : 0) + (zz ? 1 : 0) >= 2) {
            final int v = (Math.abs(x) + Math.abs(y) + Math.abs(z)) % 2;

            // While i'd like this to be based on the blockstate randomization feature, this
            // generates an alternating pattern based on level position, so this is not 100% doable with blockstates.
            if (v == 0) {
                type = ControllerRenderType.inside_a;
            } else {
                type = ControllerRenderType.inside_b;
            }
        }

        return baseState.withProperty(CONTROLLER_TYPE, type);
    }

    private static boolean isController(IBlockAccess level, int x, int y, int z) {
        // Do NOT query block entity:
        // - in Spatial IO movement, block entity might have been removed but block might still be there
        // - if we call getBlockEntity a new block entity will be loaded even though it has already been removed (bad!)
        return Platform.is(level.getBlockState(new BlockPos(x, y, z)), AEBlocks.CONTROLLER.block());
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (level.getTileEntity(pos) instanceof ControllerBlockEntity be) {
            if (!level.isRemote) {
                MenuOpener.open(NetworkStatusMenu.CONTROLLER_TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
