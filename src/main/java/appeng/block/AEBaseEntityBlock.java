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

package appeng.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.IOrientationHolder;
import appeng.api.components.DataComponentMap;
import appeng.block.networking.CableBusBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.client.render.model.AEModelData;
import appeng.core.definitions.BlockEntityType;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import appeng.util.ItemInteractionResult;
import appeng.util.Platform;
import appeng.util.SettingsFrom;

/**
 * Base class of AE2 blocks with a tile entity. On 1.12.2 the orientation (facing + spin) is stored by the tile entity
 * and all properties that modern AE2 derived from the block entity ({@link #updateBlockStateFromBlockEntity}) are
 * computed in {@link #getActualState} instead of being stored in the world. Only {@link #getMetaProperties()} are
 * persisted in the metadata (none by default).
 */
public abstract class AEBaseEntityBlock<T extends AEBaseBlockEntity> extends AEBaseBlock {

    private Class<T> blockEntityClass;

    private BlockEntityType<T> blockEntityType;

    public AEBaseEntityBlock(BlockProperties props) {
        super(props);
    }

    public void setBlockEntity(Class<T> blockEntityClass, BlockEntityType<T> blockEntityType) {
        this.blockEntityClass = blockEntityClass;
        this.blockEntityType = blockEntityType;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        var builder = new BlockStateContainer.Builder(this);
        createBlockStateDefinition(builder);
        builder.add(AEModelData.ORIENTATION, AEModelData.MODEL_DATA);
        return builder.build();
    }

    @Override
    protected List<IProperty<?>> getMetaProperties() {
        return Collections.emptyList();
    }

    @Nullable
    public T getTileEntity(IBlockAccess level, int x, int y, int z) {
        return this.getTileEntity(level, new BlockPos(x, y, z));
    }

    @Nullable
    public T getTileEntity(IBlockAccess level, BlockPos pos) {
        final TileEntity te = level.getTileEntity(pos);
        if (this.blockEntityClass != null && this.blockEntityClass.isInstance(te)) {
            return this.blockEntityClass.cast(te);
        }

        return null;
    }

    public BlockEntityType<T> getBlockEntityType() {
        return blockEntityType;
    }

    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return blockEntityType.create();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        var be = getTileEntity(worldIn, pos);
        if (be == null) {
            return state;
        }
        var strategy = getOrientationStrategy();
        if (!strategy.getProperties().isEmpty()) {
            var orientation = be.getOrientation();
            state = strategy.setOrientation(state, orientation.getSide(appeng.api.orientation.RelativeSide.FRONT),
                    orientation.getSpin());
        }
        return updateBlockStateFromBlockEntity(state, be);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state instanceof IExtendedBlockState extended) {
            var be = getTileEntity(world, pos);
            if (be != null) {
                return extended.withProperty(AEModelData.ORIENTATION, be.getOrientation())
                        .withProperty(AEModelData.MODEL_DATA, be.getModelData());
            }
            return extended.withProperty(AEModelData.ORIENTATION, BlockOrientation.NORTH_UP);
        }
        return state;
    }

    @Override
    public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean isMoving) {
        // Drop internal BE content
        if (!level.isRemote && newState.getBlock() != state.getBlock()) {
            var be = this.getTileEntity(level, pos);
            if (be != null) {
                var drops = new ArrayList<ItemStack>();
                be.addAdditionalDrops(level, pos, drops);
                Platform.spawnDrops(level, pos, drops);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(IBlockState state) {
        return blockEntityClass != null && AEBaseInvBlockEntity.class.isAssignableFrom(blockEntityClass);
    }

    @Override
    public int getAnalogOutputSignal(IBlockState state, World level, BlockPos pos) {
        var te = this.getTileEntity(level, pos);
        if (te instanceof AEBaseInvBlockEntity invBlockEntity) {
            if (!invBlockEntity.getInternalInventory().isEmpty()) {
                return invBlockEntity.getInternalInventory().getRedstoneSignal();
            }
        }
        return 0;
    }

    @Override
    public boolean triggerEvent(IBlockState state, World level, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, level, pos, eventID, eventParam);
        final TileEntity blockEntity = level.getTileEntity(pos);
        return blockEntity != null && blockEntity.receiveClientEvent(eventID, eventParam);
    }

    @Override
    public void setPlacedBy(World level, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer,
            ItemStack is) {
        var blockEntity = this.getTileEntity(level, pos);

        if (blockEntity == null) {
            return;
        }

        if (blockEntity instanceof IOwnerAwareBlockEntity ownerAware && placer instanceof EntityPlayer player) {
            ownerAware.setOwner(player);
        }

        // Inherit the item stack's display name, but only if it is a user defined name
        if (is.hasDisplayName()) {
            blockEntity.setName(is.getDisplayName());
        }

        EntityPlayer player = null;
        if (placer instanceof EntityPlayer) {
            player = (EntityPlayer) placer;
        }
        blockEntity.importSettings(SettingsFrom.DISMANTLE_ITEM, DataComponentMap.of(is), player);
    }

    /**
     * Called by {@link AEBaseBlockItem} right after placement with the state computed by
     * {@link #getStateForPlacement}, which (unlike the stored state) still contains the orientation.
     */
    public void onPlacedWithState(World level, BlockPos pos, IBlockState placementState) {
        var be = getTileEntity(level, pos);
        if (be instanceof IOrientationHolder holder && !getOrientationStrategy().getProperties().isEmpty()) {
            holder.setOrientation(BlockOrientation.get(getOrientationStrategy(), placementState));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        if (heldItem.getItem() instanceof IMemoryCard memoryCard && !(this instanceof CableBusBlock)) {
            final AEBaseBlockEntity blockEntity = this.getTileEntity(level, pos);

            if (blockEntity == null) {
                return ItemInteractionResult.FAIL;
            }

            if (InteractionUtil.isInAlternateUseMode(player)) {
                var builder = DataComponentMap.builder();
                blockEntity.exportSettings(SettingsFrom.MEMORY_CARD, builder, player);
                var settings = builder.build();
                if (!settings.isEmpty()) {
                    MemoryCardItem.clearCard(heldItem);
                    settings.applyTo(heldItem);
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                }
            } else {
                var savedName = AEComponents.EXPORTED_SETTINGS_SOURCE.get(heldItem);

                if (savedName != null && this.getName().getUnformattedText().equals(savedName.getUnformattedText())) {
                    blockEntity.importSettings(SettingsFrom.MEMORY_CARD, DataComponentMap.of(heldItem), player);
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                } else {
                    MemoryCardItem.importGenericSettingsAndNotify(blockEntity, DataComponentMap.of(heldItem), player);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    /**
     * Returns the IBlockState based on the given IBlockState while considering the state of the given block entity.
     * <p>
     * If the given block entity is not of the right type for this block, the state is returned unchanged, this is also
     * the case if the given block state does not belong to this block.
     */
    public final IBlockState getBlockEntityBlockState(IBlockState current, TileEntity te) {
        if (current.getBlock() != this || !blockEntityClass.isInstance(te)) {
            return current;
        }

        return updateBlockStateFromBlockEntity(current, blockEntityClass.cast(te));
    }

    /**
     * Reimplement this in subclasses to derive the visual state of the block from its block entity. On 1.12.2 this is
     * applied in {@link #getActualState} (it is not stored in the world).
     * <p>
     * It is guaranteed that be is not-null and the block of the given block state is this exact block instance.
     */
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, T be) {
        return currentState;
    }

    // Keep the tile entity around until the drops have been computed (Forge pattern)
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
            boolean willHarvest) {
        if (willHarvest) {
            onBlockHarvested(world, pos, state, player);
            return true;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state,
            @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    /**
     * Exports settings of the block entity when it is broken.
     */
    @Override
    public List<ItemStack> getDrops(IBlockState state, IBlockAccess level, BlockPos pos, int fortune) {
        var drops = super.getDrops(state, level, pos, fortune);
        var be = getTileEntity(level, pos);
        if (be != null) {
            for (var drop : drops) {
                if (drop.getItem() instanceof net.minecraft.item.ItemBlock blockItem && blockItem.getBlock() == this) {
                    var settings = be.exportSettings(SettingsFrom.DISMANTLE_ITEM, null);
                    settings.applyTo(drop);
                    // Export settings at most for one item
                    break;
                }
            }
        }
        return drops;
    }
}
