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
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BakedQuads;
import appeng.client.render.DelegateBakedModel;

@SideOnly(Side.CLIENT)
public class DriveBakedModel extends DelegateBakedModel implements IDynamicBakedModel {
    private final Map<Item, IBakedModel> cellModels;
    private final IBakedModel defaultCellModel;
    private final float[][] slotTranslations;

    public DriveBakedModel(IBakedModel bakedBase, Map<Item, IBakedModel> cellModels,
            IBakedModel defaultCell) {
        super(bakedBase);
        this.defaultCellModel = defaultCell;
        this.slotTranslations = buildSlotTranslations();
        this.cellModels = cellModels;
    }

    /**
     * Calculates the origin of a drive slot for positioning a cell model into it.
     */
    public static void getSlotOrigin(int row, int col, Vector3f translation) {
        // Position this drive model copy at the correct slot. The transform is based on
        // the cell-model being in slot 0,0,0 while the upper left slot's origin is at
        // 9,13,1
        float xOffset = (9 - col * 8) / 16.0f;
        float yOffset = (13 - row * 3) / 16.0f;
        float zOffset = 1 / 16.0f;
        translation.set(xOffset, yOffset, zOffset);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return IDynamicBakedModel.super.getQuads(state, side, rand);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand,
            ModelData extraData) {
        List<BakedQuad> result = new ArrayList<>(super.getQuads(state, side, rand));

        var cells = extraData.get(DriveModelData.STATE);

        // Add cell models on top of the base model, if possible
        if (cells != null) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 2; col++) {
                    int slot = getSlotIndex(row, col);

                    // Add the cell chassis
                    Item cell = slot < cells.length ? cells[slot] : null;
                    IBakedModel cellChassisModel = getCellChassisModel(cell);

                    var translation = slotTranslations[slot];
                    for (BakedQuad quad : cellChassisModel.getQuads(state, side, rand)) {
                        result.add(BakedQuads.translate(quad, translation[0], translation[1], translation[2]));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean isAmbientOcclusion() {
        // We have faces inside the chassis that are facing east, but should not receive
        // ambient occlusion from the east-side, but sadly this cannot be fine-tuned on
        // a face-by-face basis.
        return false;
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return false;
    }

    // Determine which drive chassis to show based on the used cell
    public IBakedModel getCellChassisModel(Item cell) {
        if (cell == null) {
            return cellModels.get(Items.AIR);
        }
        final IBakedModel model = cellModels.get(cell);

        return model != null ? model : defaultCellModel;
    }

    private float[][] buildSlotTranslations() {
        float[][] result = new float[5 * 2][];

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 2; col++) {
                Vector3f translation = new Vector3f();
                getSlotOrigin(row, col, translation);
                result[getSlotIndex(row, col)] = new float[] { translation.x, translation.y, translation.z };
            }
        }

        return result;
    }

    private static int getSlotIndex(int row, int col) {
        return row * 2 + col;
    }
}
