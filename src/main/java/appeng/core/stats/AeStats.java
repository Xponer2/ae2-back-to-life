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

package appeng.core.stats;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import appeng.core.AppEng;

public enum AeStats {

    ItemsInserted("items_inserted"),

    ItemsExtracted("items_extracted");

    private final ResourceLocation registryName;
    private StatBase stat;

    AeStats(String id) {
        this.registryName = AppEng.makeId(id);
    }

    public void addToPlayer(EntityPlayer player, int howMany) {
        if (stat != null) {
            player.addStat(stat, howMany);
        }
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    /**
     * Registers the 1.12.2 stat. Its id is the 1.21 lang key {@code stat.<namespace>.<path>}.
     */
    public void register() {
        if (stat == null) {
            var key = "stat." + registryName.getNamespace() + "." + registryName.getPath();
            stat = new StatBasic(key, new TextComponentTranslation(key)).registerStat();
        }
    }

}
