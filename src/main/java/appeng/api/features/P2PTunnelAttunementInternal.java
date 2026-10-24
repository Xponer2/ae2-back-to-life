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

package appeng.api.features;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import appeng.api.capabilities.ItemCapability;
import appeng.api.util.ItemLike;
import appeng.api.util.TagKey;

/**
 * Internal methods that complement {@link P2PTunnelAttunement} and which are not part of the public API.
 */
public final class P2PTunnelAttunementInternal {

    private P2PTunnelAttunementInternal() {
    }

    /**
     * Gets a report which sources of attunement exist for a given tunnel type.
     */
    public static AttunementInfo getAttunementInfo(ItemLike tunnelType) {
        var tunnelItem = tunnelType.asItem();

        Set<ItemCapability<?, Void>> caps = new HashSet<>();

        for (var entry : P2PTunnelAttunement.apiAttunements) {
            if (entry.tunnelType() == tunnelItem) {
                caps.add(entry.capability());
            }
        }

        return new AttunementInfo(caps);
    }

    public static List<Resultant> getApiTunnels() {
        return P2PTunnelAttunement.apiAttunements.stream()
                .map(info -> new Resultant(info.component(), info.tunnelType(), info::hasApi)).collect(Collectors.toList());
    }

    public static Map<TagKey<Item>, Item> getTagTunnels() {
        return P2PTunnelAttunement.tagTunnels;
    }

    public final static class AttunementInfo {
        private final Set<ItemCapability<?, Void>> apis;

        public AttunementInfo(Set<ItemCapability<?, Void>> apis) {
            this.apis = apis;
        }

        public Set<ItemCapability<?, Void>> apis() {
            return apis;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AttunementInfo)) return false;
            AttunementInfo that = (AttunementInfo) o;
            return java.util.Objects.equals(apis, that.apis);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(apis);
        }

        @Override
        public String toString() {
            return "AttunementInfo[" + "apis=" + apis + "]";
        }

    }

    public final static class Resultant {
        private final ITextComponent description;
        private final Item tunnelType;
        private final Predicate<ItemStack> stackPredicate;

        public Resultant(ITextComponent description, Item tunnelType, Predicate<ItemStack> stackPredicate) {
            this.description = description;
            this.tunnelType = tunnelType;
            this.stackPredicate = stackPredicate;
        }

        public ITextComponent description() {
            return description;
        }

        public Item tunnelType() {
            return tunnelType;
        }

        public Predicate<ItemStack> stackPredicate() {
            return stackPredicate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Resultant)) return false;
            Resultant that = (Resultant) o;
            return java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(tunnelType, that.tunnelType)
                    && java.util.Objects.equals(stackPredicate, that.stackPredicate);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(description, tunnelType, stackPredicate);
        }

        @Override
        public String toString() {
            return "Resultant[" + "description=" + description + ", " + "tunnelType=" + tunnelType + ", " + "stackPredicate=" + stackPredicate + "]";
        }

    }
}
