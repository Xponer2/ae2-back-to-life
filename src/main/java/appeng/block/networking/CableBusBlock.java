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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IFacadePart;
import appeng.api.util.AEColor;
import appeng.block.AEBaseEntityBlock;
import appeng.util.BlockPlaceContext;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.cablebus.CableBusBakedModel;
import appeng.client.render.cablebus.CableBusBreakingParticle;
import appeng.client.render.cablebus.CableBusRenderState;
import appeng.client.render.model.AEModelData;
import appeng.core.CreativeTabOutput;
import appeng.hooks.WrenchHook;
import appeng.integration.abstraction.IAEFacade;
import appeng.parts.ICableBusContainer;
import appeng.parts.NullCableBusContainer;
import appeng.util.ItemInteractionResult;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

public class CableBusBlock extends AEBaseEntityBlock<CableBusBlockEntity> implements IAEFacade {

    private static final ICableBusContainer NULL_CABLE_BUS = new NullCableBusContainer();

    public CableBusBlock() {
        // 1.12.2 has no waterlogging; the light level comes from the parts (see getLightValue)
        super(glassProps().noOcclusion().dynamicShape().forceSolidOn());
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.cb(world, pos).getLightValue();
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        // 1.21 isPathfindable = false
        return false;
    }

    @Override
    public List<ItemStack> getDrops(IBlockState state, IBlockAccess level, BlockPos pos, int fortune) {
        if (level.getTileEntity(pos) instanceof CableBusBlockEntity bus) {
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
    public boolean isLadder(IBlockState state, IBlockAccess level, BlockPos pos, EntityLivingBase entity) {
        return this.cb(level, pos).isLadder(entity);
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
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World level, BlockPos pos,
            EntityPlayer player) {
        var v3 = target.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
        var sp = this.cb(level, pos).selectPartLocal(v3);

        if (sp.part != null) {
            return new ItemStack(sp.part.getPartItem().asItem());
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
        Vec3d hitVec = hit.hitVec;
        Vec3d hitInBlock = new Vec3d(hitVec.x - pos.getX(), hitVec.y - pos.getY(), hitVec.z - pos.getZ());
        return this.cb(level, pos).useItemOn(heldItem, player, hand, hitInBlock)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        // Transform from world into block space
        Vec3d hitVec = hitResult.hitVec;
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

    @Override
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
    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateForPlacement(context.getLevel(), context.getClickedPos());
    }

    public IBlockState getStateForPlacement(World level, BlockPos pos) {
        return this.getDefaultState();
    }

    @Override
    public IBlockState updateShape(IBlockState blockState, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {
        this.cb(level, currentPos).onUpdateShape(level, currentPos, facing);

        return super.updateShape(blockState, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void onNeighborChange(IBlockState state, IBlockAccess level, BlockPos pos, BlockPos neighbor) {
        this.cb(level, pos).onNeighborChanged(level, pos, neighbor);
    }

    /**
     * The cable bus model needs the world to look at adjacent facades and glass.
     */
    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        var extended = super.getExtendedState(state, world, pos);
        if (extended instanceof IExtendedBlockState extendedState) {
            var modelData = extendedState.getValue(AEModelData.MODEL_DATA);
            if (modelData != null) {
                var renderState = modelData.get(CableBusRenderState.PROPERTY);
                if (renderState != null) {
                    renderState.setWorld(world);
                }
            }
        }
        return extended;
    }

    /**
     * Cables and parts render in CUTOUT, facades in the layer(s) of their source block.
     */
    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addHitEffects(IBlockState state, World level, RayTraceResult target, ParticleManager effectRenderer) {

        // Half the particle rate. Since we're spawning concentrated on a specific spot,
        // our particle effect otherwise looks too strong
        if (level.rand.nextBoolean()) {
            return true;
        }

        if (target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }
        BlockPos blockPos = target.getBlockPos();

        ICableBusContainer cb = cb(level, blockPos);

        // Our built-in model has the actual baked sprites we need
        var model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(getDefaultState());

        // We cannot add the effect if we don't have the model
        if (!(model instanceof CableBusBakedModel cableBusModel)) {
            return true;
        }

        CableBusRenderState renderState = cb.getRenderState();

        // Spawn a particle for one of the particle textures
        var textures = cableBusModel.getParticleTextures(renderState);
        if (!textures.isEmpty()) {
            var texture = textures.get(level.rand.nextInt(textures.size()));
            double x = target.hitVec.x;
            double y = target.hitVec.y;
            double z = target.hitVec.z;
            // FIXME: Check how this looks, probably like shit, maybe provide parts the
            // ability to supply particle textures???
            effectRenderer.addEffect(
                    new CableBusBreakingParticle(level, x, y, z, texture).multipleParticleScaleBy(0.8F));
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World level, BlockPos pos, ParticleManager effectRenderer) {
        // Suppress break particles when being disassembled by a wrench
        if (WrenchHook.isDisassembling()) {
            return true;
        }

        ICableBusContainer cb = cb(level, pos);

        // Our built-in model has the actual baked sprites we need
        var model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(getDefaultState());

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
                        var texture = textures.get(level.rand.nextInt(textures.size()));

                        final double x = pos.getX() + (j + 0.5D) / 4.0D;
                        final double y = pos.getY() + (k + 0.5D) / 4.0D;
                        final double z = pos.getZ() + (l + 0.5D) / 4.0D;

                        // FIXME: Check how this looks, probably like shit, maybe provide parts the
                        // ability to supply particle textures???
                        effectRenderer.addEffect(new CableBusBreakingParticle(level, x, y, z,
                                x - pos.getX() - 0.5D, y - pos.getY() - 0.5D, z - pos.getZ() - 0.5D, texture));
                    }
                }
            }
        }

        return true;
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
     * 1.12.2 has no {@code getAppearance}; the facade rendering uses this direction directly.
     */
    public static ThreadLocal<EnumFacing> RENDERING_FACADE_DIRECTION = new ThreadLocal<>();
}
