/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.decorative;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import appeng.block.BlockProperties;

/**
 * Stairs of an AE2 decorative block. The vanilla 1.12.2 stairs have the same properties (facing, half, shape) as the
 * modern ones.
 */
public class AEStairBlock extends BlockStairs {
    private final BlockProperties properties;

    public AEStairBlock(IBlockState modelState, BlockProperties props) {
        super(modelState);
        this.properties = props.copy();
        this.properties.applyTo(this);
        setSoundType(this.properties.getSoundType());
        this.useNeighborBrightness = true;
    }

    public String getDescriptionId() {
        ResourceLocation id = getRegistryName();
        return id == null ? "block.ae2.unknown" : "block." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public String getTranslationKey() {
        return getDescriptionId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getLocalizedName() {
        return net.minecraft.util.text.translation.I18n.translateToLocal(getDescriptionId());
    }
}
