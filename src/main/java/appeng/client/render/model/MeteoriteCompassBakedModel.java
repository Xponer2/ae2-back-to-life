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

package appeng.client.render.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.DelegateBakedModel;
import appeng.hooks.CompassManager;

/**
 * This baked model combines the quads of a compass base and the quads of a compass pointer, which will be rotated
 * around the Y-axis to get the compass to point in the right direction.
 * <p/>
 * 1.21 derives the pointer direction from the full item transform. 1.12.2 passes only the display context to the
 * model, so like the 1.12.2 AE2 the rotation of the player holding the compass is added instead.
 */
@SideOnly(Side.CLIENT)
public class MeteoriteCompassBakedModel implements IDynamicBakedModel {

    // Rotation is expressed as radians
    public static final ModelProperty<Float> ROTATION = new ModelProperty<>();

    private final IBakedModel base;

    private final IBakedModel pointer;

    public MeteoriteCompassBakedModel(IBakedModel base, IBakedModel pointer) {
        this.base = base;
        this.pointer = pointer;
    }

    public IBakedModel getPointer() {
        return pointer;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand,
            ModelData extraData) {
        // Get rotation from the special block state
        Float rotationData = extraData.get(ROTATION);
        float rotation = rotationData != null ? rotationData : 0.0f;

        // Pre-compute the quad count to avoid list resizes
        List<BakedQuad> quads = new ArrayList<>(this.base.getQuads(state, side, rand));

        // We'll add the pointer as "sideless" to the item rendering when state is null
        if (side == null && state == null) {
            // Set up the rotation around the Y-axis for the pointer
            float cos = MathHelper.cos(rotation);
            float sin = MathHelper.sin(rotation);
            for (BakedQuad bakedQuad : this.pointer.getQuads(state, side, rand)) {
                quads.add(rotateY(bakedQuad, cos, sin));
            }
        }

        return quads;
    }

    private static BakedQuad rotateY(BakedQuad quad, float cos, float sin) {
        var format = quad.getFormat();
        var data = quad.getVertexData().clone();
        var tmp = new float[4];
        for (int e = 0; e < format.getElementCount(); e++) {
            var usage = format.getElement(e).getUsage();
            boolean position = usage == VertexFormatElement.EnumUsage.POSITION;
            if (!position && usage != VertexFormatElement.EnumUsage.NORMAL) {
                continue;
            }
            for (int v = 0; v < 4; v++) {
                LightUtil.unpack(data, tmp, format, v, e);
                float x = position ? tmp[0] - 0.5f : tmp[0];
                float z = position ? tmp[2] - 0.5f : tmp[2];
                // Same as JOML rotationY: x' = cos*x + sin*z, z' = -sin*x + cos*z
                float rx = cos * x + sin * z;
                float rz = -sin * x + cos * z;
                tmp[0] = position ? rx + 0.5f : rx;
                tmp[2] = position ? rz + 0.5f : rz;
                LightUtil.pack(tmp, data, format, v, e);
            }
        }
        // After rotation, facing a specific side cannot be guaranteed anymore
        return new BakedQuad(data, quad.getTintIndex(), null, quad.getSprite(), quad.shouldApplyDiffuseLighting(),
                format);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.base.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.base.getItemCameraTransforms();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
            ItemCameraTransforms.TransformType cameraTransformType) {
        return Pair.of(this, this.base.handlePerspective(cameraTransformType).getRight());
    }

    @Override
    public ItemOverrideList getOverrides() {
        /*
         * The entity is given when an item rendered on the hotbar, or when held in hand.
         */
        return new ItemOverrideList(Collections.emptyList()) {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World level,
                    @Nullable EntityLivingBase entity) {
                float rotation;
                if (level != null && entity instanceof EntityPlayer player) {
                    float offRads = (float) (player.rotationYaw / 180.0f * (float) Math.PI + Math.PI);
                    rotation = offRads + getAnimatedRotation(player.getPositionVector(), true, 0);
                } else if (level != null && entity != null) {
                    rotation = getAnimatedRotation(entity.getPositionVector(), true, 0);
                } else {
                    rotation = getAnimatedRotation(null, false, 0);
                }
                return new FixedRotationModel(rotation);
            }
        };
    }

    // Model wrapper that allows us to pass a rotation along to the baked model
    class FixedRotationModel extends DelegateBakedModel {
        private final float rotation;

        public FixedRotationModel(float rotation) {
            super(MeteoriteCompassBakedModel.this);
            this.rotation = rotation;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            var modelData = ModelData.builder().with(ROTATION, rotation).build();
            return MeteoriteCompassBakedModel.this.getQuads(state, side, rand, modelData);
        }
    }

    /**
     * Gets the effective, animated rotation for the compass given the current position of the compass.
     */
    public static float getAnimatedRotation(@Nullable Vec3d pos, boolean prefetch, float playerRotation) {
        // Only query for a meteor position if we know our own position
        if (pos != null) {
            var ourChunkPos = new ChunkPos(new BlockPos(pos));
            var closestMeteorite = CompassManager.INSTANCE.getClosestMeteorite(ourChunkPos, prefetch);

            // No close meteorite was found -> spin slowly
            if (closestMeteorite == null) {
                long timeMillis = System.currentTimeMillis();
                // .5 seconds per full rotation
                timeMillis %= 500;
                return timeMillis / 500.f * (float) Math.PI * 2;
            } else {
                var dx = pos.x - closestMeteorite.getX();
                var dz = pos.z - closestMeteorite.getZ();
                var distanceSq = dx * dx + dz * dz;
                if (distanceSq > 6 * 6) {
                    var x = closestMeteorite.getX();
                    var z = closestMeteorite.getZ();
                    return (float) rad(pos.x, pos.z, x, z) + playerRotation;
                }
            }
        }

        long timeMillis = System.currentTimeMillis();
        // 3 seconds per full rotation
        timeMillis %= 3000;
        return timeMillis / 3000.f * (float) Math.PI * 2;
    }

    private static double rad(double ax, double az, double bx, double bz) {
        var up = bz - az;
        var side = bx - ax;

        return Math.atan2(-up, side) - Math.PI / 2.0;
    }

}
