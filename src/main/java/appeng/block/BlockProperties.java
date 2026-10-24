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

import java.util.function.ToIntFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

/**
 * Backport of Minecraft's BlockProperties. 1.12.2 blocks need a {@link Material}; it is derived from the
 * sound type unless set explicitly. Like the modern class, setters mutate and return this object.
 */
public final class BlockProperties {
    private Material material = Material.ROCK;
    private boolean materialExplicit;
    private MapColor mapColor = MapColor.STONE;
    private SoundType soundType = SoundType.STONE;
    private float destroyTime = 0;
    private float explosionResistance = 0;
    private ToIntFunction<IBlockState> lightEmission = s -> 0;
    private boolean canOcclude = true;
    private boolean hasCollision = true;
    private boolean randomTicks;
    private boolean requiresCorrectToolForDrops;
    private boolean noLootTable;
    private boolean isAir;
    private boolean redstoneConductor = true;
    private boolean viewBlocking = true;
    private boolean validSpawn = true;
    private boolean dynamicShape;
    private float friction = 0.6F;
    private boolean pushReactionDestroy;

    private BlockProperties() {
    }

    public static BlockProperties of() {
        return new BlockProperties();
    }

    public static BlockProperties of(Material material) {
        var p = new BlockProperties();
        p.material(material);
        return p;
    }

    public static BlockProperties ofFullCopy(Block block) {
        var p = new BlockProperties();
        var state = block.getDefaultState();
        p.material(state.getMaterial());
        p.soundType = block.getSoundType();
        return p;
    }

    public BlockProperties copy() {
        var p = new BlockProperties();
        p.material = material;
        p.materialExplicit = materialExplicit;
        p.mapColor = mapColor;
        p.soundType = soundType;
        p.destroyTime = destroyTime;
        p.explosionResistance = explosionResistance;
        p.lightEmission = lightEmission;
        p.canOcclude = canOcclude;
        p.hasCollision = hasCollision;
        p.randomTicks = randomTicks;
        p.requiresCorrectToolForDrops = requiresCorrectToolForDrops;
        p.noLootTable = noLootTable;
        p.isAir = isAir;
        p.redstoneConductor = redstoneConductor;
        p.viewBlocking = viewBlocking;
        p.validSpawn = validSpawn;
        p.dynamicShape = dynamicShape;
        p.friction = friction;
        p.pushReactionDestroy = pushReactionDestroy;
        return p;
    }

    public BlockProperties material(Material material) {
        this.material = material;
        this.materialExplicit = true;
        return this;
    }

    public BlockProperties mapColor(MapColor mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public BlockProperties sound(SoundType soundType) {
        this.soundType = soundType;
        if (!materialExplicit) {
            if (soundType == SoundType.METAL || soundType == SoundType.ANVIL) {
                material = Material.IRON;
            } else if (soundType == SoundType.GLASS) {
                material = Material.GLASS;
            } else if (soundType == SoundType.WOOD) {
                material = Material.WOOD;
            } else if (soundType == SoundType.GROUND) {
                material = Material.GROUND;
            } else if (soundType == SoundType.SAND) {
                material = Material.SAND;
            } else if (soundType == SoundType.PLANT) {
                material = Material.PLANTS;
            } else if (soundType == SoundType.CLOTH) {
                material = Material.CLOTH;
            } else {
                material = Material.ROCK;
            }
        }
        return this;
    }

    public BlockProperties strength(float destroyTime, float explosionResistance) {
        this.destroyTime = destroyTime;
        this.explosionResistance = Math.max(0.0F, explosionResistance);
        return this;
    }

    public BlockProperties strength(float strength) {
        return strength(strength, strength);
    }

    public BlockProperties instabreak() {
        return strength(0.0F);
    }

    public BlockProperties destroyTime(float destroyTime) {
        this.destroyTime = destroyTime;
        return this;
    }

    public BlockProperties explosionResistance(float explosionResistance) {
        this.explosionResistance = Math.max(0.0F, explosionResistance);
        return this;
    }

    public BlockProperties lightLevel(ToIntFunction<IBlockState> lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public BlockProperties noOcclusion() {
        this.canOcclude = false;
        return this;
    }

    public BlockProperties noCollission() {
        this.hasCollision = false;
        this.canOcclude = false;
        return this;
    }

    public BlockProperties randomTicks() {
        this.randomTicks = true;
        return this;
    }

    public BlockProperties requiresCorrectToolForDrops() {
        this.requiresCorrectToolForDrops = true;
        return this;
    }

    public BlockProperties noLootTable() {
        this.noLootTable = true;
        return this;
    }

    public BlockProperties air() {
        this.isAir = true;
        return this;
    }

    public BlockProperties forceSolidOn() {
        return this;
    }

    public BlockProperties isRedstoneConductor(Object predicate) {
        this.redstoneConductor = false;
        return this;
    }

    public BlockProperties isViewBlocking(Object predicate) {
        this.viewBlocking = false;
        return this;
    }

    public BlockProperties isValidSpawn(Object predicate) {
        this.validSpawn = false;
        return this;
    }

    public BlockProperties isSuffocating(Object predicate) {
        return this;
    }

    public BlockProperties dynamicShape() {
        this.dynamicShape = true;
        return this;
    }

    public BlockProperties friction(float friction) {
        this.friction = friction;
        return this;
    }

    public BlockProperties pushReactionDestroy() {
        this.pushReactionDestroy = true;
        return this;
    }

    /**
     * Modern PushReaction is not configurable per block on 1.12.2, only DESTROY is supported.
     */
    public BlockProperties pushReaction(@Nullable Object reaction) {
        this.pushReactionDestroy = true;
        return this;
    }

    public Material getMaterial() {
        return material;
    }

    public MapColor getMapColor() {
        return mapColor;
    }

    public SoundType getSoundType() {
        return soundType;
    }

    public float getDestroyTime() {
        return destroyTime;
    }

    public float getExplosionResistance() {
        return explosionResistance;
    }

    public ToIntFunction<IBlockState> getLightEmission() {
        return lightEmission;
    }

    public boolean canOcclude() {
        return canOcclude;
    }

    public boolean hasCollision() {
        return hasCollision;
    }

    public boolean isRandomlyTicking() {
        return randomTicks;
    }

    public boolean isRequiresCorrectToolForDrops() {
        return requiresCorrectToolForDrops;
    }

    public boolean isNoLootTable() {
        return noLootTable;
    }

    public boolean isAir() {
        return isAir;
    }

    public boolean isRedstoneConductor() {
        return redstoneConductor;
    }

    public boolean isViewBlocking() {
        return viewBlocking;
    }

    public boolean isValidSpawn() {
        return validSpawn;
    }

    public float getFriction() {
        return friction;
    }

    public boolean isPushReactionDestroy() {
        return pushReactionDestroy;
    }

    /**
     * Applies the settings that are plain block fields to a 1.12.2 block.
     */
    public void applyTo(Block block) {
        block.setHardness(destroyTime);
        // 1.12.2's setResistance multiplies by 3 and getExplosionResistance divides by 5
        block.setResistance(explosionResistance * 5.0F / 3.0F);
        block.setTickRandomly(randomTicks);
        if (!canOcclude) {
            block.setLightOpacity(0);
        }
        block.slipperiness = friction;
    }
}
