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

package appeng.crafting;

import java.util.Map;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

public final class CraftingPlan implements ICraftingPlan {
    private final GenericStack finalOutput;
    private final long bytes;
    private final boolean simulation;
    private final boolean multiplePaths;
    private final KeyCounter usedItems;
    private final KeyCounter emittedItems;
    private final KeyCounter missingItems;
    private final Map<IPatternDetails, Long> patternTimes;

    public CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, boolean multiplePaths, KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems, Map<IPatternDetails, Long> patternTimes) {
        this.finalOutput = finalOutput;
        this.bytes = bytes;
        this.simulation = simulation;
        this.multiplePaths = multiplePaths;
        this.usedItems = usedItems;
        this.emittedItems = emittedItems;
        this.missingItems = missingItems;
        this.patternTimes = patternTimes;
    }

    public GenericStack finalOutput() {
        return finalOutput;
    }

    public long bytes() {
        return bytes;
    }

    public boolean simulation() {
        return simulation;
    }

    public boolean multiplePaths() {
        return multiplePaths;
    }

    public KeyCounter usedItems() {
        return usedItems;
    }

    public KeyCounter emittedItems() {
        return emittedItems;
    }

    public KeyCounter missingItems() {
        return missingItems;
    }

    public Map<IPatternDetails, Long> patternTimes() {
        return patternTimes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftingPlan)) return false;
        CraftingPlan that = (CraftingPlan) o;
        return java.util.Objects.equals(finalOutput, that.finalOutput)
                && bytes == that.bytes
                && simulation == that.simulation
                && multiplePaths == that.multiplePaths
                && java.util.Objects.equals(usedItems, that.usedItems)
                && java.util.Objects.equals(emittedItems, that.emittedItems)
                && java.util.Objects.equals(missingItems, that.missingItems)
                && java.util.Objects.equals(patternTimes, that.patternTimes);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(finalOutput, bytes, simulation, multiplePaths, usedItems, emittedItems, missingItems, patternTimes);
    }

    @Override
    public String toString() {
        return "CraftingPlan[" + "finalOutput=" + finalOutput + ", " + "bytes=" + bytes + ", " + "simulation=" + simulation + ", " + "multiplePaths=" + multiplePaths + ", " + "usedItems=" + usedItems + ", " + "emittedItems=" + emittedItems + ", " + "missingItems=" + missingItems + ", " + "patternTimes=" + patternTimes + "]";
    }

}
