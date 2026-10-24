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

package appeng.client.render.model;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import appeng.api.orientation.BlockOrientation;

/**
 * This implementation of IModelData allows us to know precisely which data is part of the model data.
 * <p/>
 * In 1.12.2 the model data reaches the baked models through two unlisted properties of the extended block state,
 * which AE blocks with a block entity expose from {@code getExtendedState}:
 * <ul>
 * <li>{@link #ORIENTATION}: the orientation of the block, which is stored in the block entity. Models of such blocks
 * are wrapped in an {@link AutoRotatingBakedModel}, which rotates the quads accordingly.</li>
 * <li>{@link #MODEL_DATA}: the {@link ModelData} returned by the block entity.</li>
 * </ul>
 * Not client-only, the block classes reference these properties on both sides.
 */
public final class AEModelData {

    public static final ModelProperty<Boolean> SKIP_CACHE = new ModelProperty<>();
    public static final ModelProperty<Byte> SPIN = new ModelProperty<>();

    public static final IUnlistedProperty<BlockOrientation> ORIENTATION = new Unlisted<>("orientation",
            BlockOrientation.class);
    public static final IUnlistedProperty<ModelData> MODEL_DATA = new Unlisted<>("model_data", ModelData.class);

    private AEModelData() {
    }

    public static ModelData.Builder builder() {
        return ModelData.builder();
    }

    public static ModelData create() {
        return builder().build();
    }

    /**
     * @return The model data of the given (extended) block state, or {@link ModelData#EMPTY}.
     */
    public static ModelData getModelData(@Nullable IBlockState state) {
        if (state instanceof IExtendedBlockState extendedState
                && extendedState.getUnlistedNames().contains(MODEL_DATA)) {
            var modelData = extendedState.getValue(MODEL_DATA);
            if (modelData != null) {
                return modelData;
            }
        }
        return ModelData.EMPTY;
    }

    /**
     * @return The orientation stored in the given (extended) block state, or null if it doesn't have one.
     */
    @Nullable
    public static BlockOrientation getOrientation(@Nullable IBlockState state) {
        if (state instanceof IExtendedBlockState extendedState
                && extendedState.getUnlistedNames().contains(ORIENTATION)) {
            return extendedState.getValue(ORIENTATION);
        }
        return null;
    }

    private static final class Unlisted<T> implements IUnlistedProperty<T> {
        private final String name;
        private final Class<T> type;

        private Unlisted(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isValid(T value) {
            return true;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public String valueToString(T value) {
            return String.valueOf(value);
        }
    }
}
