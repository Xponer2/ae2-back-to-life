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

package appeng.recipes.mattercannon;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.api.util.ItemLike;
import appeng.api.util.TagKey;
import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;

/**
 * Defines a type of ammo that can be used for the {@link appeng.items.tools.powered.MatterCannonItem}.
 */
public class MatterCannonAmmo implements IRecipe<RecipeInput> {
    @Deprecated(forRemoval = true, since = "1.21.1")
    public static final ResourceLocation TYPE_ID = AppEng.makeId("matter_cannon");

    @Deprecated(forRemoval = true, since = "1.21.1")
    public static final RecipeType<MatterCannonAmmo> TYPE = AERecipeTypes.MATTER_CANNON_AMMO;

    public static final MapCodec<MatterCannonAmmo> CODEC = RecordCodecBuilder.mapCodec((builder) -> {
        return builder.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ammo").forGetter(MatterCannonAmmo::getAmmo),
                Codec.FLOAT.fieldOf("weight").forGetter(MatterCannonAmmo::getWeight))
                .apply(builder, MatterCannonAmmo::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterCannonAmmo> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            MatterCannonAmmo::getAmmo,
            ByteBufCodecs.FLOAT,
            MatterCannonAmmo::getWeight,
            MatterCannonAmmo::new);

    private final Ingredient ammo;

    private final float weight;

    public MatterCannonAmmo(Ingredient ammo, float weight) {
        Preconditions.checkArgument(weight >= 0, "Weight must not be negative");
        this.ammo = Objects.requireNonNull(ammo, "ammo must not be null");
        this.weight = weight;
    }

    public static void ammo(RecipeOutput consumer, ResourceLocation id, ItemLike item, float weight) {
        consumer.accept(id, new MatterCannonAmmo(Ingredient.of(item), weight), null);
    }

    public static void ammo(RecipeOutput consumer, ResourceLocation id, Ingredient ammo, float weight) {
        consumer.accept(id, new MatterCannonAmmo(ammo, weight), null);
    }

    public static void ammo(RecipeOutput consumer, ResourceLocation id, TagKey<Item> tag, float weight) {
        consumer.accept(id, new MatterCannonAmmo(Ingredient.of(tag), weight), null, new NotCondition(
                new TagEmptyCondition(tag.location())));
    }

    @Override
    public boolean matches(RecipeInput inv, World level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput inv) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MatterCannonAmmoSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    public Ingredient getAmmo() {
        return ammo;
    }

    public float getWeight() {
        return weight;
    }
}
