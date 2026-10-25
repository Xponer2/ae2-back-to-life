/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.block;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.util.EnumFacing;

/**
 * The block state properties of modern Minecraft that AE2 uses (1.12.2 has no waterlogging).
 */
public final class BlockStateProperties {
    private BlockStateProperties() {
    }

    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyDirection HORIZONTAL_FACING = PropertyDirection.create("facing",
            EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 15);

}
