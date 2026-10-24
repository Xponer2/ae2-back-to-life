/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.blockentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

/**
 * 1.12.2: tile entities tick through {@link ITickable}. Dispatches to {@link ServerTickingBlockEntity} and
 * {@link ClientTickingBlockEntity} like the modern block entity tickers did.
 */
public interface AETickingBlockEntity extends ITickable {
    @Override
    default void update() {
        var world = ((TileEntity) this).getWorld();
        if (world == null) {
            return;
        }
        if (world.isRemote) {
            if (this instanceof ClientTickingBlockEntity client) {
                client.clientTick();
            }
        } else if (this instanceof ServerTickingBlockEntity server) {
            server.serverTick();
        }
    }
}
