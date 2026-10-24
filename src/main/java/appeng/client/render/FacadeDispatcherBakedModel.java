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

package appeng.client.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.cablebus.FacadeBuilder;
import appeng.items.parts.FacadeItem;

/**
 * This baked model class is used as a dispatcher to redirect the renderer to the *real* model that should be used based
 * on the item stack. A custom Item Override List is used to accomplish this.
 */
@SideOnly(Side.CLIENT)
public class FacadeDispatcherBakedModel extends DelegateBakedModel {
    private final FacadeBuilder facadeBuilder;
    private final Map<StackKey, FacadeBakedItemModel> cache = new HashMap<>();

    public FacadeDispatcherBakedModel(IBakedModel baseModel, FacadeBuilder facadeBuilder) {
        super(baseModel);
        this.facadeBuilder = facadeBuilder;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return new ItemOverrideList(Collections.emptyList()) {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world,
                    @Nullable EntityLivingBase entity) {
                return getFacadeModel(stack);
            }
        };
    }

    private synchronized IBakedModel getFacadeModel(ItemStack stack) {
        if (!(stack.getItem() instanceof FacadeItem itemFacade)) {
            return this;
        }

        var textureItem = itemFacade.getTextureItem(stack);
        if (textureItem.isEmpty()) {
            return this;
        }

        return cache.computeIfAbsent(new StackKey(textureItem),
                key -> new FacadeBakedItemModel(getBaseModel(), textureItem, facadeBuilder));
    }

    /**
     * Item type, damage and tag, the 1.21 type-and-tag item stack map.
     */
    private static final class StackKey {
        private final Item item;
        private final int damage;
        @Nullable
        private final NBTTagCompound tag;

        StackKey(ItemStack stack) {
            this.item = stack.getItem();
            this.damage = stack.getItemDamage();
            this.tag = stack.getTagCompound() != null ? stack.getTagCompound().copy() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StackKey other)) {
                return false;
            }
            return item == other.item && damage == other.damage && Objects.equals(tag, other.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, damage, tag);
        }
    }
}
