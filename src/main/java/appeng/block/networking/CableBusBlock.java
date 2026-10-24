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

package appeng.block.networking;

import appeng.core.CreativeTabOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer.Builder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IFacadePart;
import appeng.api.util.AEColor;
import appeng.block.AEBaseEntityBlock;
import appeng.block.BlockStateProperties;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.cablebus.CableBusBakedModel;
import appeng.client.render.cablebus.CableBusBreakingParticle;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.client.render.model.ModelData;
import appeng.integration.abstraction.IAEFacade;
import appeng.parts.ICableBusContainer;
import appeng.parts.NullCableBusContainer;
import appeng.util.BlockPlaceContext;
import appeng.util.ItemInteractionResult;
import appeng.util.Util;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class CableBusBlock extends AEBaseEntityBlock<CableBusBlockEntity> implements IAEFacade, SimpleWaterloggedBlock {

    private static final ICableBusContainer NULL_CABLE_BUS = new NullCableBusContainer();

    private static final PropertyInteger LIGHT_LEVEL = PropertyInteger.create("light_level", 0, 15);
    private static final PropertyBool WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CableBusBlock() {
        super(glassProps().noOcclusion().noLootTable().dynamicShape().forceSolidOn()
                .lightLevel(state -> state.getValue(LIGHT_LEVEL)));
        setDefaultState(getDefaultState().withProperty(LIGHT_LEVEL, 0).withProperty(WATERLOGGED, false));
    }

    @Override
    public boolean propagatesSkylightDown(IBlockState state, IBlockAccess reader, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean isPathfindable(IBlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(IBlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CableBusBlockEntity bus) {
            var drops = new ArrayList<ItemStack>();
            bus.getCableBus().addPartDrops(drops);
            return drops;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void animateTick(IBlockState state, World level, BlockPos pos, Random rand) {
        this.cb(level, pos).animateTick(level, pos, rand);
    }

    @Override
    public int getSignal(IBlockState state, IBlockAccess level, BlockPos pos, EnumFacing side) {
        return this.cb(level, pos).isProvidingWeakPower(side.getOpposite()); // TODO:
        // IS
        // OPPOSITE!?
    }

    @Override
    public boolean isSignalSource(IBlockState state) {
        return true;
    }

    @Override
    public void entityInside(IBlockState state, World level, BlockPos pos, Entity entityIn) {
        this.cb(level, pos).onEntityCollision(entityIn);
    }

    @Override
    public int getDirectSignal(IBlockState state, IBlockAccess level, BlockPos pos,
            EnumFacing side) {
        return this.cb(level, pos).isProvidingStrongPower(side.getOpposite()); // TODO:
        // IS
        // OPPOSITE!?
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, IBlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT_LEVEL, WATERLOGGED);
    }

    @Override
    public boolean isLadder(IBlockState state, World level, BlockPos pos, EntityLivingBase entity) {
        return this.cb(level, pos).isLadder(entity);
    }

    @Override
    public boolean canBeReplaced(IBlockState state, BlockPlaceContext useContext) {
        // FIXME: Potentially check the fluid one too
        return super.canBeReplaced(state, useContext)
                && this.cb(useContext.getLevel(), useContext.getClickedPos()).isEmpty();
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess level, BlockPos pos,
            @Nullable EnumFacing side) {
        if (side == null) {
            return false;
        }

        return this.cb(level, pos).canConnectRedstone(side.getOpposite());
    }

    @Override
    public ItemStack getCloneItemStack(IBlockState state, RayTraceResult target, World level, BlockPos pos,
            EntityPlayer player) {
        var v3 = target.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        var sp = this.cb(level, pos).selectPartLocal(v3);

        if (sp.part != null) {
            return new ItemStack(sp.part.getPartItem());
        } else if (sp.facade != null) {
            return sp.facade.getItemStack();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block blockIn, BlockPos fromPos,
            boolean isMoving) {
        if (!level.isRemote) {
            this.cb(level, pos).onNeighborChanged(level, pos, fromPos);
        }
    }

    private ICableBusContainer cb(IBlockAccess level, BlockPos pos) {
        final TileEntity te = level.getTileEntity(pos);
        ICableBusContainer out = null;

        if (te instanceof CableBusBlockEntity) {
            out = ((CableBusBlockEntity) te).getCableBus();
        }

        return out == null ? NULL_CABLE_BUS : out;
    }

    @Nullable
    private IFacadeContainer fc(IBlockAccess level, BlockPos pos) {
        final TileEntity te = level.getTileEntity(pos);
        IFacadeContainer out = null;

        if (te instanceof CableBusBlockEntity) {
            out = ((CableBusBlockEntity) te).getCableBus().getFacadeContainer();
        }

        return out;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        // Transform from world into block space
        Vec3d hitVec = hit.getLocation();
        Vec3d hitInBlock = new Vec3d(hitVec.x - pos.getX(), hitVec.y - pos.getY(), hitVec.z - pos.getZ());
        return this.cb(level, pos).useItemOn(heldItem, player, hand, hitInBlock)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        // Transform from world into block space
        Vec3d hitVec = hitResult.getLocation();
        Vec3d hitInBlock = new Vec3d(hitVec.x - pos.getX(), hitVec.y - pos.getY(), hitVec.z - pos.getZ());
        return this.cb(level, pos).useWithoutItem(player, hitInBlock)
                ? EnumActionResult.SUCCESS
                : EnumActionResult.PASS;
    }

    public boolean recolorBlock(IBlockAccess level, BlockPos pos, EnumFacing side,
            EnumDyeColor color, EntityPlayer who) {
        try {
            return this.cb(level, pos).recolourBlock(side, AEColor.fromDye(color), who);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        // do nothing
    }

    @Override
    public IBlockState getFacadeState(IBlockAccess level, BlockPos pos, EnumFacing side) {
        if (side != null) {
            IFacadeContainer container = this.fc(level, pos);
            if (container != null) {
                IFacadePart facade = container.getFacade(side);
                if (facade != null) {
                    return facade.getBlockState();
                }
            }
        }
        return level.getBlockState(pos);
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        CableBusBlockEntity te = getTileEntity(level, pos);
        if (te == null) {
            return Shapes.empty();
        } else {
            return te.getCableBus().getShape();
        }
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        CableBusBlockEntity te = getTileEntity(level, pos);
        if (te == null) {
            return Shapes.empty();
        } else {
            return te.getCableBus().getCollisionShape(context);
        }
    }

    @Override
    protected IBlockState updateBlockStateFromBlockEntity(IBlockState currentState, CableBusBlockEntity be) {
        if (currentState.getBlock() != this) {
            return currentState;
        }
        int lightLevel = be.getCableBus().getLightValue();
        return super.updateBlockStateFromBlockEntity(currentState, be).withProperty(LIGHT_LEVEL, lightLevel);
    }

    @Override
    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateForPlacement(context.getLevel(), context.getClickedPos());
    }

    public IBlockState getStateForPlacement(World level, BlockPos pos) {
        var fluidState = level.getFluidState(pos);
        return this.getDefaultState().setValue(WATERLOGGED, fluidState.getType() == FluidRegistry.WATER);
    }

    @Override
    public FluidState getFluidState(IBlockState blockState) {
        return blockState.getValue(WATERLOGGED).booleanValue()
                ? FluidRegistry.WATER.getSource(false)
                : super.getFluidState(blockState);
    }

    @Override
    public IBlockState updateShape(IBlockState blockState, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {
        if (blockState.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, FluidRegistry.WATER, FluidRegistry.WATER.getTickDelay(level));
        }

        this.cb(level, currentPos).onUpdateShape(level, currentPos, facing);

        return super.updateShape(blockState, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void onNeighborChange(IBlockState state, World level, BlockPos pos, BlockPos neighbor) {
        this.cb(level, pos).onNeighborChanged(level, pos, neighbor);
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {

            @Override
            public boolean addHitEffects(IBlockState state, World level, RayTraceResult target,
                    ParticleEngine effectRenderer) {

                // Half the particle rate. Since we're spawning concentrated on a specific spot,
                // our particle effect otherwise looks too strong
                if (level.rand.nextBoolean()) {
                    return true;
                }

                if (target.getType() != Type.BLOCK) {
                    return false;
                }
                BlockPos blockPos = BlockPos.containing(target.getLocation().x, target.getLocation().y,
                        target.getLocation().z);

                ICableBusContainer cb = cb(level, blockPos);

                // Our built-in model has the actual baked sprites we need
                IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
                        .getBlockModel(getDefaultState());

                // We cannot add the effect if we don't have the model
                if (!(model instanceof CableBusBakedModel cableBusModel)) {
                    return true;
                }

                CableBusRenderState renderState = cb.getRenderState();

                // Spawn a particle for one of the particle textures
                var textures = cableBusModel.getParticleTextures(renderState);
                if (!textures.isEmpty()) {
                    var texture = Util.getRandom(textures, level.rand);
                    double x = target.getLocation().x;
                    double y = target.getLocation().y;
                    double z = target.getLocation().z;
                    // FIXME: Check how this looks, probably like shit, maybe provide parts the
                    // ability to supply particle textures???
                    effectRenderer.add(
                            new CableBusBreakingParticle((WorldClient) level, x, y, z, texture).scale(0.8F));
                }

                return true;
            }

            @Override
            public boolean addDestroyEffects(IBlockState state, World level, BlockPos pos,
                    ParticleEngine effectRenderer) {
                ICableBusContainer cb = cb(level, pos);

                // Our built-in model has the actual baked sprites we need
                IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
                        .getBlockModel(getDefaultState());

                // We cannot add the effect if we dont have the model
                if (!(model instanceof CableBusBakedModel cableBusModel)) {
                    return true;
                }

                CableBusRenderState renderState = cb.getRenderState();

                List<TextureAtlasSprite> textures = cableBusModel.getParticleTextures(renderState);

                if (!textures.isEmpty()) {
                    // Shamelessly inspired by ParticleManager.addBlockDestroyEffects
                    for (int j = 0; j < 4; ++j) {
                        for (int k = 0; k < 4; ++k) {
                            for (int l = 0; l < 4; ++l) {
                                // Randomly select one of the textures if the cable bus has more than just one
                                // possibility here
                                var texture = Util.getRandom(textures, level.rand);

                                final double x = pos.getX() + (j + 0.5D) / 4.0D;
                                final double y = pos.getY() + (k + 0.5D) / 4.0D;
                                final double z = pos.getZ() + (l + 0.5D) / 4.0D;

                                // FIXME: Check how this looks, probably like shit, maybe provide parts the
                                // ability to supply particle textures???
                                Particle effect = new CableBusBreakingParticle((WorldClient) level, x, y, z,
                                        x - pos.getX() - 0.5D, y - pos.getY() - 0.5D, z - pos.getZ() - 0.5D, texture);
                                effectRenderer.add(effect);
                            }
                        }
                    }
                }

                return true;
            }
        });
    }

    /**
     * WTF does this do you ask?
     * <p>
     * Well, this is needed to properly handle adjacent facades, as we need to know on which side the source facade is.
     * The cases to handle are:
     * <ul>
     * <li>Not rendering a facade: then we just look at the facade on the requested side.</li>
     * <li>Rendering a facade and the requested side is the opposite: then this is likely an interior quad of the
     * facade, so we actually check the rendering side instead of the requested side.</li>
     * <li>Rendering a facade in other cases: check requested side first, otherwise check rendering side since we might
     * also connect to a facade in another direction due to the 2 extra pixels on the side of a facade.</li>
     * </ul>
     */
    public static ThreadLocal<EnumFacing> RENDERING_FACADE_DIRECTION = new ThreadLocal<>();

    @Override
    public IBlockState getAppearance(IBlockState state, IBlockAccess renderView, BlockPos pos, EnumFacing side,
            @Nullable IBlockState sourceState, @Nullable BlockPos sourcePos) {
        ModelData modelData;
        if (renderView instanceof WorldServer serverLevel) {
            // We're on the server, use BE directly
            TileEntity be = renderView.getTileEntity(pos);
            modelData = be != null ? be.getModelData() : ModelData.EMPTY;
        } else {
            modelData = renderView.getModelData(pos);
        }

        CableBusRenderState cableBusRenderState = modelData.get(CableBusRenderState.PROPERTY);
        if (cableBusRenderState != null) {
            var renderingFacadeDir = RENDERING_FACADE_DIRECTION.get();
            var facades = cableBusRenderState.getFacades();

            if (side.getOpposite() != renderingFacadeDir) {
                var facadeState = facades.get(side);
                if (facadeState != null) {
                    return facadeState.getSourceBlock();
                }
            }

            if (renderingFacadeDir != null && facades.containsKey(renderingFacadeDir)) {
                return facades.get(renderingFacadeDir).getSourceBlock();
            }
        }
        return state;
    }
}
