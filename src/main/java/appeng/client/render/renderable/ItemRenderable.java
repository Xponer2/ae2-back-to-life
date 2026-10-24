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

package appeng.client.render.renderable;

import java.util.function.Function;

import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;

/**
 * Renders an item, moved by an optional translation (the 1.21 Transformation, of which AE2 only uses the translation).
 */
@SideOnly(Side.CLIENT)
public class ItemRenderable<T extends TileEntity> implements Renderable<T> {

    private final Function<T, Pair<ItemStack, Vector3f>> f;

    public ItemRenderable(Function<T, Pair<ItemStack, Vector3f>> f) {
        this.f = f;
    }

    @Override
    public void renderBlockEntityAt(T be, float partialTicks, PoseStack poseStack, int combinedLight) {
        Pair<ItemStack, Vector3f> pair = this.f.apply(be);
        if (pair != null && pair.getLeft() != null) {
            poseStack.pushPose();
            if (pair.getRight() != null) {
                var translation = pair.getRight();
                poseStack.translate(translation.x, translation.y, translation.z);
            }
            BlockEntityRenderHelper.renderItem(pair.getLeft(), ItemCameraTransforms.TransformType.GROUND,
                    combinedLight);
            poseStack.popPose();
        }
    }

}
