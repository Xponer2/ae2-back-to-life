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

package appeng.client.render.tesr.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.spatial.SpatialPylonBlockEntity;
import appeng.client.render.cablebus.CubeBuilder;
import appeng.client.render.model.IDynamicBakedModel;
import appeng.client.render.model.ModelData;

/**
 * The baked model that will be used for rendering the spatial pylon.
 */
@SideOnly(Side.CLIENT)
class SpatialPylonBakedModel implements IDynamicBakedModel {

    private final Map<SpatialPylonTextureType, TextureAtlasSprite> textures;

    SpatialPylonBakedModel(Map<SpatialPylonTextureType, TextureAtlasSprite> textures) {
        this.textures = ImmutableMap.copyOf(textures);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState blockState, @Nullable EnumFacing side, long rand,
            ModelData extraData) {
        // The quads are full block faces, so they are returned for their side to be culled like 1.21 does
        if (side == null) {
            return Collections.emptyList();
        }

        var state = getState(extraData);
        CubeBuilder builder = new CubeBuilder();

        if (state.axisPosition() != SpatialPylonBlockEntity.AxisPosition.NONE) {
            EnumFacing ori = null;
            var displayAxis = state.axis();
            var axisPos = state.axisPosition();

            if (displayAxis == EnumFacing.Axis.Y) {
                ori = EnumFacing.UP;

                if (axisPos == SpatialPylonBlockEntity.AxisPosition.END) {
                    builder.setFlipV(EnumFacing.NORTH, true);
                    builder.setFlipV(EnumFacing.SOUTH, true);
                    builder.setFlipV(EnumFacing.WEST, true);
                    builder.setFlipV(EnumFacing.EAST, true);
                }

            } else if (displayAxis == EnumFacing.Axis.X) {
                ori = EnumFacing.EAST;

                builder.setUvRotation(EnumFacing.NORTH, 1);
                builder.setUvRotation(EnumFacing.SOUTH, 1);
                builder.setUvRotation(EnumFacing.UP, 3);
                builder.setUvRotation(EnumFacing.DOWN, 3);

                if (axisPos == SpatialPylonBlockEntity.AxisPosition.START) {
                    builder.setFlipV(EnumFacing.UP, true);
                    builder.setFlipV(EnumFacing.DOWN, true);
                    builder.setFlipV(EnumFacing.NORTH, true);
                } else if (axisPos == SpatialPylonBlockEntity.AxisPosition.END) {
                    builder.setFlipV(EnumFacing.SOUTH, true);
                }

            } else if (displayAxis == EnumFacing.Axis.Z) {
                ori = EnumFacing.NORTH;

                builder.setUvRotation(EnumFacing.WEST, 1);
                builder.setUvRotation(EnumFacing.EAST, 1);

                if (axisPos == SpatialPylonBlockEntity.AxisPosition.START) {
                    builder.setFlipV(EnumFacing.UP, true);
                    builder.setFlipV(EnumFacing.EAST, true);
                } else if (axisPos == SpatialPylonBlockEntity.AxisPosition.END) {
                    builder.setFlipV(EnumFacing.DOWN, true);
                    builder.setFlipV(EnumFacing.WEST, true);
                }
            }

            builder.setTextures(this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.UP)),
                    this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.DOWN)),
                    this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.NORTH)),
                    this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.SOUTH)),
                    this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.EAST)),
                    this.textures.get(getTextureTypeFromSideOutside(state, ori, EnumFacing.WEST)));
            builder.addCube(0, 0, 0, 16, 16, 16);

            if (state.powered()) {
                builder.setEmissiveMaterial(true);
            }

            builder.setTextures(this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.UP)),
                    this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.DOWN)),
                    this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.NORTH)),
                    this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.SOUTH)),
                    this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.EAST)),
                    this.textures.get(getTextureTypeFromSideInside(state, ori, EnumFacing.WEST)));
        } else {
            builder.setTexture(this.textures.get(SpatialPylonTextureType.BASE));
            builder.addCube(0, 0, 0, 16, 16, 16);

            builder.setTexture(this.textures.get(SpatialPylonTextureType.DIM));
        }

        builder.addCube(0, 0, 0, 16, 16, 16);

        // Reset back to default
        builder.setEmissiveMaterial(false);

        var result = new ArrayList<BakedQuad>();
        for (var quad : builder.getOutput()) {
            if (quad.getFace() == side) {
                result.add(quad);
            }
        }
        return result;
    }

    private SpatialPylonBlockEntity.ClientState getState(ModelData modelData) {
        var state = modelData.get(SpatialPylonBlockEntity.STATE);
        return state != null ? state : SpatialPylonBlockEntity.ClientState.DEFAULT;
    }

    private static SpatialPylonTextureType getTextureTypeFromSideOutside(SpatialPylonBlockEntity.ClientState state,
            EnumFacing ori, EnumFacing dir) {
        if (ori == dir || ori.getOpposite() == dir) {
            return SpatialPylonTextureType.BASE;
        }

        if (state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.MIDDLE) {
            return SpatialPylonTextureType.BASE_SPANNED;
        } else if (state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.START
                || state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.END) {
            return SpatialPylonTextureType.BASE_END;
        }

        return SpatialPylonTextureType.BASE;
    }

    private static SpatialPylonTextureType getTextureTypeFromSideInside(SpatialPylonBlockEntity.ClientState state,
            EnumFacing ori, EnumFacing dir) {
        final boolean good = state.online();

        if (ori == dir || ori.getOpposite() == dir) {
            return good ? SpatialPylonTextureType.DIM : SpatialPylonTextureType.RED;
        }

        if (state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.MIDDLE) {
            return good ? SpatialPylonTextureType.DIM_SPANNED : SpatialPylonTextureType.RED_SPANNED;
        } else if (state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.START
                || state.axisPosition() == SpatialPylonBlockEntity.AxisPosition.END) {
            return good ? SpatialPylonTextureType.DIM_END : SpatialPylonTextureType.RED_END;
        }

        return SpatialPylonTextureType.BASE;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.textures.get(SpatialPylonTextureType.DIM);
    }
}
