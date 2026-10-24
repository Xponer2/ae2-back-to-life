/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.client.render.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.client.render.BakedQuads;
import appeng.client.render.DelegateBakedModel;
import appeng.core.AppEng;

/**
 * The faces that 1.21 renders with full brightness ({@code neoforge_data: {block_light: 15, sky_light: 15}} in the
 * model JSON). 1.12.2 can't read that from the JSON, so the faces are identified by their sprite: in every affected
 * model the emissive sprites are never used by a non-emissive face.
 */
@SideOnly(Side.CLIENT)
public final class EmissiveModels {

    /**
     * Part models: every quad of these models is emissive, unless a sprite set is given.
     */
    private static final Map<ResourceLocation, Set<ResourceLocation>> PART_MODELS = new HashMap<>();

    /**
     * Block models by block id: quads with one of the sprites are emissive.
     */
    private static final Map<ResourceLocation, Set<ResourceLocation>> BLOCK_MODELS = new HashMap<>();

    static {
        for (var name : new String[] { "conversion_monitor_locked_on", "conversion_monitor_on",
                "crafting_terminal_on", "display_status_has_channel", "display_status_on", "export_bus_has_channel",
                "export_bus_on", "import_bus_has_channel", "import_bus_on", "interface_has_channel", "interface_on",
                "level_emitter_status_has_channel", "level_emitter_status_on", "monitor_bright_on",
                "monitor_dark_on", "monitor_medium_on", "pattern_access_terminal_on", "pattern_encoding_terminal_on",
                "storage_bus_has_channel", "storage_bus_on", "storage_monitor_locked_on", "storage_monitor_on",
                "terminal_on", "toggle_bus_status_has_channel", "toggle_bus_status_on",
                "p2p/p2p_tunnel_status_has_channel", "p2p/p2p_tunnel_status_on" }) {
            PART_MODELS.put(AppEng.makeId("part/" + name), null);
        }
        PART_MODELS.put(AppEng.makeId("part/transition_plane_has_channel"),
                sprites("part/monitor_sides_status_has_channel"));
        PART_MODELS.put(AppEng.makeId("part/transition_plane_on"), sprites("part/monitor_sides_status_on"));

        BLOCK_MODELS.put(AppEng.makeId("controller"), sprites("block/controller_conflict",
                "block/controller_lights", "block/controller_column_conflict", "block/controller_column_lights"));
        BLOCK_MODELS.put(AppEng.makeId("mysterious_cube"), sprites("block/mysterious_cube_bottom",
                "block/mysterious_cube_side", "block/mysterious_cube_top"));
        BLOCK_MODELS.put(AppEng.makeId("molecular_assembler"), sprites("block/molecular_assembler_lights"));
    }

    private EmissiveModels() {
    }

    private static Set<ResourceLocation> sprites(String... names) {
        var result = new HashSet<ResourceLocation>();
        for (var name : names) {
            result.add(AppEng.makeId(name));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Makes the emissive faces of the given baked part model full-bright, if it has any.
     */
    public static IBakedModel wrapPartModel(ResourceLocation modelId, IBakedModel model) {
        if (!PART_MODELS.containsKey(modelId)) {
            return model;
        }
        return new EmissiveBakedModel(model, PART_MODELS.get(modelId));
    }

    /**
     * Makes the emissive faces of the given baked block model full-bright, if the block has any.
     */
    public static IBakedModel wrapBlockModel(ResourceLocation blockId, IBakedModel model) {
        var sprites = BLOCK_MODELS.get(blockId);
        if (sprites == null) {
            return model;
        }
        return new EmissiveBakedModel(model, sprites);
    }

    private static class EmissiveBakedModel extends DelegateBakedModel {
        @Nullable
        private final Set<ResourceLocation> sprites;
        // The quads of JSON models don't change, so the converted quads are cached per source quad (by identity)
        private final Map<BakedQuad, BakedQuad> converted = new ConcurrentHashMap<>();

        EmissiveBakedModel(IBakedModel base, @Nullable Set<ResourceLocation> sprites) {
            super(base);
            this.sprites = sprites;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            var quads = super.getQuads(state, side, rand);
            if (quads.isEmpty()) {
                return quads;
            }
            BakedQuad[] result = null;
            for (int i = 0; i < quads.size(); i++) {
                var quad = quads.get(i);
                if (isEmissive(quad)) {
                    if (result == null) {
                        result = quads.toArray(new BakedQuad[0]);
                    }
                    result[i] = converted.computeIfAbsent(quad, BakedQuads::makeEmissive);
                }
            }
            return result != null ? java.util.Arrays.asList(result) : quads;
        }

        private boolean isEmissive(BakedQuad quad) {
            if (sprites == null) {
                return true;
            }
            var sprite = quad.getSprite();
            return sprite != null && sprites.contains(new ResourceLocation(sprite.getIconName()));
        }
    }
}
