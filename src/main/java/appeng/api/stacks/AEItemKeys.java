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

package appeng.api.stacks;

import java.util.Objects;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import appeng.api.util.TagKey;
import appeng.core.AppEng;
import appeng.core.localization.GuiText;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.util.BuiltInRegistries;

final class AEItemKeys extends AEKeyType {
    private static final ResourceLocation ID = AppEng.makeId("i");

    static final AEItemKeys INSTANCE = new AEItemKeys();

    private AEItemKeys() {
        super(ID, AEItemKey.class, GuiText.Items.text());
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return AEItemKey.MAP_CODEC;
    }

    @Override
    public AEItemKey readFromPacket(RegistryFriendlyByteBuf input) {
        Objects.requireNonNull(input);

        return AEItemKey.fromPacket(input);
    }

    @Override
    public AEItemKey loadKeyFromTag(NBTTagCompound tag) {
        return AEItemKey.fromTag(tag);
    }

    @Override
    public boolean supportsFuzzyRangeSearch() {
        return true;
    }

    @Override
    public Stream<TagKey<?>> getTagNames() {
        return appeng.core.tags.TagManager.getTagIds("item").stream()
                .map(id -> (TagKey<?>) TagKey.create(appeng.api.util.Registries.ITEM, id));
    }
}
