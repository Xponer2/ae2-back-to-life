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
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.core.AppEng;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.ByteBufCodecs;
import appeng.core.network.codec.StreamCodec;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.RecipeInput;
import appeng.recipes.RecipeSerializer;
import appeng.recipes.RecipeType;
import appeng.recipes.AERecipe;
import appeng.recipes.ingredient.IngredientCodecs;

/**
 * Defines a type of ammo that can be used for the {@link appeng.items.tools.powered.MatterCannonItem}.
 */
public class MatterCannonAmmo implements AERecipe<RecipeInput> {
    @Deprecated
    public static final ResourceLocation TYPE_ID = AppEng.makeId("matter_cannon");

    @Deprecated
    public static final RecipeType<MatterCannonAmmo> TYPE = AERecipeTypes.MATTER_CANNON_AMMO;

    public static final MapCodec<MatterCannonAmmo> CODEC = RecordCodecBuilder.mapCodec((builder) -> {
        return builder.group(
                IngredientCodecs.CODEC_NONEMPTY.fieldOf("ammo").forGetter(MatterCannonAmmo::getAmmo),
                Codec.FLOAT.fieldOf("weight").forGetter(MatterCannonAmmo::getWeight))
                .apply(builder, MatterCannonAmmo::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterCannonAmmo> STREAM_CODEC = StreamCodec.composite(
            IngredientCodecs.STREAM_CODEC,
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
