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

package appeng.block.storage;

import appeng.block.BlockProperties;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import appeng.block.RenderShape;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.storage.SkyChestBlockEntity;
import appeng.core.definitions.AEBlockEntities;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.SkyChestMenu;
import appeng.menu.locator.MenuLocators;
import appeng.util.BlockPlaceContext;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class SkyChestBlock extends AEBaseEntityBlock<SkyChestBlockEntity> {

    private static final double AABB_OFFSET_BOTTOM = 0.00;
    private static final double AABB_OFFSET_SIDES = 0.06;
    private static final double AABB_OFFSET_TOP = 0.0625;

    // Precomputed bounding boxes of the chest, sorted into the map by the UP
    // direction
    private static final Map<EnumFacing, VoxelShape> SHAPES = new EnumMap<>(EnumFacing.class);


    static {
        for (EnumFacing up : EnumFacing.VALUES) {
            AxisAlignedBB aabb = computeAABB(up);
            SHAPES.put(up, Shapes.create(aabb));
        }
    }

    public enum SkyChestType {
        STONE, BLOCK
    }

    public final SkyChestType type;

    public SkyChestBlock(SkyChestType type, BlockProperties props) {
        super(props);
        this.type = type;
        setDefaultState(getDefaultState());
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        super.createBlockStateDefinition(builder);
            }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(IBlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        if (level.getTileEntity(pos) instanceof SkyChestBlockEntity be) {
            if (!level.isRemote) {
                MenuOpener.open(SkyChestMenu.TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return EnumActionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void tick(IBlockState state, World level, BlockPos pos, Random random) {
        var chest = AEBlockEntities.SKY_CHEST.getTileEntity(level, pos);
        if (chest != null) {
            chest.recheckOpen();
        }
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        final SkyChestBlockEntity sk = this.getTileEntity(level, pos);
        EnumFacing up = sk != null ? sk.getTop() : EnumFacing.UP;
        return SHAPES.get(up);
    }

    private static AxisAlignedBB computeAABB(EnumFacing up) {
        final double offsetX = up.getXOffset() == 0 ? AABB_OFFSET_SIDES : 0.0;
        final double offsetY = up.getYOffset() == 0 ? AABB_OFFSET_SIDES : 0.0;
        final double offsetZ = up.getZOffset() == 0 ? AABB_OFFSET_SIDES : 0.0;

        // for x/z top and bottom is swapped
        final double minX = Math.max(0.0,
                offsetX + (up.getXOffset() < 0 ? AABB_OFFSET_BOTTOM : up.getXOffset() * AABB_OFFSET_TOP));
        final double minY = Math.max(0.0,
                offsetY + (up.getYOffset() < 0 ? AABB_OFFSET_TOP : up.getYOffset() * AABB_OFFSET_BOTTOM));
        final double minZ = Math.max(0.0,
                offsetZ + (up.getZOffset() < 0 ? AABB_OFFSET_BOTTOM : up.getZOffset() * AABB_OFFSET_TOP));

        final double maxX = Math.min(1.0,
                1.0 - offsetX - (up.getXOffset() < 0 ? AABB_OFFSET_TOP : up.getXOffset() * AABB_OFFSET_BOTTOM));
        final double maxY = Math.min(1.0,
                1.0 - offsetY - (up.getYOffset() < 0 ? AABB_OFFSET_BOTTOM : up.getYOffset() * AABB_OFFSET_TOP));
        final double maxZ = Math.min(1.0,
                1.0 - offsetZ - (up.getZOffset() < 0 ? AABB_OFFSET_TOP : up.getZOffset() * AABB_OFFSET_BOTTOM));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }


    @Override
    public IBlockState updateShape(IBlockState blockState, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {

        return super.updateShape(blockState, facing, facingState, level, currentPos, facingPos);
    }
}
