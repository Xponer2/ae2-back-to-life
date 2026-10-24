/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.decorative.solid;

import appeng.block.BlockProperties;
import appeng.decorative.AEDecorativeBlock;

/**
 * 1.12.2 only: copper ore, since vanilla 1.12.2 has no copper but AE2's recipes need it. Drops itself; smelting
 * yields {@code ae2:copper_ingot}.
 */
public class CopperOreBlock extends AEDecorativeBlock {
    public CopperOreBlock(BlockProperties props) {
        super(props);
        setHarvestLevel("pickaxe", 1);
    }
}
