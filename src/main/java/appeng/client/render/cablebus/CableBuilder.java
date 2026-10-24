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

package appeng.client.render.cablebus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.core.AppEng;

/**
 * A helper class that builds quads for cable connections.
 */
@SideOnly(Side.CLIENT)
class CableBuilder {

    // Textures for the cable core types, one per type/color pair
    private final EnumMap<CableCoreType, EnumMap<AEColor, TextureAtlasSprite>> coreTextures;

    // Textures for rendering the actual connection cubes, one per type/color pair
    private final EnumMap<AECableType, EnumMap<AEColor, TextureAtlasSprite>> connectionTextures;

    private final SmartCableTextures smartCableTextures;

    CableBuilder(Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        this.coreTextures = new EnumMap<>(CableCoreType.class);

        for (CableCoreType type : CableCoreType.values()) {
            EnumMap<AEColor, TextureAtlasSprite> colorTextures = new EnumMap<>(AEColor.class);

            for (AEColor color : AEColor.values()) {
                colorTextures.put(color, bakedTextureGetter.apply(type.getTexture(color)));
            }

            this.coreTextures.put(type, colorTextures);
        }

        this.connectionTextures = new EnumMap<>(AECableType.class);

        for (AECableType type : AECableType.VALIDCABLES) {
            EnumMap<AEColor, TextureAtlasSprite> colorTextures = new EnumMap<>(AEColor.class);

            for (AEColor color : AEColor.values()) {
                colorTextures.put(color, bakedTextureGetter.apply(getConnectionTexture(type, color)));
            }

            this.connectionTextures.put(type, colorTextures);
        }

        this.smartCableTextures = new SmartCableTextures(bakedTextureGetter);
    }

    /**
     * @return All textures used by cables, to be stitched into the block atlas.
     */
    static Collection<ResourceLocation> getTextures() {
        var result = new ArrayList<ResourceLocation>();
        for (CableCoreType type : CableCoreType.values()) {
            for (AEColor color : AEColor.values()) {
                result.add(type.getTexture(color));
            }
        }
        for (AECableType type : AECableType.VALIDCABLES) {
            for (AEColor color : AEColor.values()) {
                result.add(getConnectionTexture(type, color));
            }
        }
        result.addAll(SmartCableTextures.getTextures());
        return result;
    }

    static ResourceLocation getConnectionTexture(AECableType cableType, AEColor color) {
        String textureFolder = switch (cableType) {
            case GLASS -> "part/cable/glass/";
            case COVERED -> "part/cable/covered/";
            case SMART -> "part/cable/smart/";
            case DENSE_COVERED -> "part/cable/dense_covered/";
            case DENSE_SMART -> "part/cable/dense_smart/";
            default -> throw new IllegalStateException("Cable type " + cableType + " does not support connections.");
        };

        return AppEng.makeId(textureFolder + color.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Adds the core of a cable to the given list of quads.
     * <p>
     * The type of cable core is automatically deduced from the given cable type.
     */
    public void addCableCore(AECableType cableType, AEColor color, List<BakedQuad> quadsOut) {
        switch (cableType) {
            case GLASS:
                this.addCableCore(CableCoreType.GLASS, color, quadsOut);
                break;
            case COVERED:
            case SMART:
                this.addCableCore(CableCoreType.COVERED, color, quadsOut);
                break;
            case DENSE_COVERED:
            case DENSE_SMART:
                this.addCableCore(CableCoreType.DENSE, color, quadsOut);
                break;
            default:
        }
    }

    public void addCableCore(CableCoreType coreType, AEColor color, List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        TextureAtlasSprite texture = this.coreTextures.get(coreType).get(color);
        cubeBuilder.setTexture(texture);

        switch (coreType) {
            case GLASS -> cubeBuilder.addCube(6, 6, 6, 10, 10, 10);
            case COVERED -> cubeBuilder.addCube(5, 5, 5, 11, 11, 11);
            case DENSE -> cubeBuilder.addCube(3, 3, 3, 13, 13, 13);
        }
    }

    public void addGlassConnection(EnumFacing facing, AEColor cableColor, AECableType connectionType,
            boolean cableBusAdjacent, List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the one on the connection side
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));

        // For to-machine connections, use a thicker end-cap for the connection
        if (connectionType != AECableType.GLASS && !cableBusAdjacent) {
            TextureAtlasSprite texture = this.connectionTextures.get(AECableType.COVERED).get(cableColor);
            cubeBuilder.setTexture(texture);

            this.addBigCoveredCableSizedCube(facing, cubeBuilder);
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.GLASS).get(cableColor);
        cubeBuilder.setTexture(texture);

        switch (facing) {
            case DOWN -> cubeBuilder.addCube(6, 0, 6, 10, 6, 10);
            case EAST -> cubeBuilder.addCube(10, 6, 6, 16, 10, 10);
            case NORTH -> cubeBuilder.addCube(6, 6, 0, 10, 10, 6);
            case SOUTH -> cubeBuilder.addCube(6, 6, 10, 10, 10, 16);
            case UP -> cubeBuilder.addCube(6, 10, 6, 10, 16, 10);
            case WEST -> cubeBuilder.addCube(0, 6, 6, 6, 10, 10);
        }
    }

    public void addStraightGlassConnection(EnumFacing facing, AEColor cableColor, List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the connection caps. We can do this because the
        // glass cable is the smallest one
        // and its ends will always be covered by something
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing, facing.getOpposite())));

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.GLASS).get(cableColor);
        cubeBuilder.setTexture(texture);

        switch (facing) {
            case DOWN, UP -> cubeBuilder.addCube(6, 0, 6, 10, 16, 10);
            case NORTH, SOUTH -> cubeBuilder.addCube(6, 6, 0, 10, 10, 16);
            case EAST, WEST -> cubeBuilder.addCube(0, 6, 6, 16, 10, 10);
        }
    }

    public void addConstrainedGlassConnection(EnumFacing facing, AEColor cableColor, int distanceFromEdge,
            List<BakedQuad> quadsOut) {

        // Glass connections reach only 6 voxels from the edge
        if (distanceFromEdge >= 6) {
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.GLASS).get(cableColor);
        cubeBuilder.setTexture(texture);

        switch (facing) {
            case DOWN -> cubeBuilder.addCube(6, distanceFromEdge, 6, 10, 6, 10);
            case EAST -> cubeBuilder.addCube(10, 6, 6, 16 - distanceFromEdge, 10, 10);
            case NORTH -> cubeBuilder.addCube(6, 6, distanceFromEdge, 10, 10, 6);
            case SOUTH -> cubeBuilder.addCube(6, 6, 10, 10, 10, 16 - distanceFromEdge);
            case UP -> cubeBuilder.addCube(6, 10, 6, 10, 16 - distanceFromEdge, 10);
            case WEST -> cubeBuilder.addCube(distanceFromEdge, 6, 6, 6, 10, 10);
        }
    }

    public void addCoveredConnection(EnumFacing facing, AEColor cableColor, AECableType connectionType,
            boolean cableBusAdjacent, List<BakedQuad> quadsOut) {

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the one on the connection side
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.COVERED).get(cableColor);
        cubeBuilder.setTexture(texture);

        // Draw a covered connection, if anything but glass is requested
        if (connectionType != AECableType.GLASS && !cableBusAdjacent) {
            this.addBigCoveredCableSizedCube(facing, cubeBuilder);
        }

        addCoveredCableSizedCube(facing, cubeBuilder);
    }

    public void addStraightCoveredConnection(EnumFacing facing, AEColor cableColor, List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.COVERED).get(cableColor);
        cubeBuilder.setTexture(texture);

        setStraightCableUVs(cubeBuilder, facing, 5 / 16f, 11 / 16f);

        addStraightCoveredCableSizedCube(facing, cubeBuilder);
    }

    private static void setStraightCableUVs(CubeBuilder cubeBuilder, EnumFacing facing, float x, float y) {
        switch (facing) {
            case DOWN, UP -> {
                cubeBuilder.setCustomUv(EnumFacing.NORTH, x, 0, y, x);
                cubeBuilder.setCustomUv(EnumFacing.EAST, x, 0, y, x);
                cubeBuilder.setCustomUv(EnumFacing.SOUTH, x, 0, y, x);
                cubeBuilder.setCustomUv(EnumFacing.WEST, x, 0, y, x);
            }
            case EAST, WEST -> {
                cubeBuilder.setCustomUv(EnumFacing.UP, 0, x, x, y);
                cubeBuilder.setCustomUv(EnumFacing.DOWN, 0, x, x, y);
                cubeBuilder.setCustomUv(EnumFacing.NORTH, 0, x, x, y);
                cubeBuilder.setCustomUv(EnumFacing.SOUTH, 0, x, x, y);
            }
            case NORTH, SOUTH -> {
                cubeBuilder.setCustomUv(EnumFacing.UP, x, 0, y, x);
                cubeBuilder.setCustomUv(EnumFacing.DOWN, x, 0, y, x);
                cubeBuilder.setCustomUv(EnumFacing.EAST, 0, x, x, y);
                cubeBuilder.setCustomUv(EnumFacing.WEST, 0, x, x, y);
            }
        }
    }

    public void addConstrainedCoveredConnection(EnumFacing facing, AEColor cableColor, int distanceFromEdge,
            List<BakedQuad> quadsOut) {
        // The core of a covered cable reaches up to 5 voxels from the block edge, so
        // drawing a connection can only occur from there onwards
        if (distanceFromEdge >= 5) {
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.COVERED).get(cableColor);
        cubeBuilder.setTexture(texture);

        addCoveredCableSizedCube(facing, distanceFromEdge, cubeBuilder);
    }

    public void addSmartConnection(EnumFacing facing, AEColor cableColor, AECableType connectionType,
            boolean cableBusAdjacent, int channels, List<BakedQuad> quadsOut) {
        if (connectionType == AECableType.COVERED || connectionType == AECableType.GLASS) {
            this.addCoveredConnection(facing, cableColor, connectionType, cableBusAdjacent, quadsOut);
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the one on the connection side
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));

        // Flip end or side texture as needed depending on direction
        switch (facing) {
            case DOWN -> {
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
                cubeBuilder.setFlipU(EnumFacing.NORTH, true);
            }
            case UP -> {
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
                cubeBuilder.setFlipU(EnumFacing.NORTH, true);
                cubeBuilder.setFlipV(EnumFacing.DOWN, true);
            }
            case SOUTH -> cubeBuilder.setFlipU(EnumFacing.NORTH, true);
            case WEST -> {
                cubeBuilder.setFlipV(EnumFacing.DOWN, true);
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
            }
            case EAST -> cubeBuilder.setFlipV(EnumFacing.DOWN, true);
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.SMART).get(cableColor);
        cubeBuilder.setTexture(texture);

        TextureAtlasSprite oddChannel = this.smartCableTextures.getOddTextureForChannels(channels);
        TextureAtlasSprite evenChannel = this.smartCableTextures.getEvenTextureForChannels(channels);

        // For to-machine connections, use a thicker end-cap for the connection
        if (connectionType != AECableType.GLASS && !cableBusAdjacent) {
            this.addBigCoveredCableSizedCube(facing, cubeBuilder);

            // Render the channel indicators brightly lit at night
            cubeBuilder.setEmissiveMaterial(true);

            cubeBuilder.setTexture(oddChannel);
            cubeBuilder.setColorRGB(cableColor.blackVariant);
            this.addBigCoveredCableSizedCube(facing, cubeBuilder);

            cubeBuilder.setTexture(evenChannel);
            cubeBuilder.setColorRGB(cableColor.whiteVariant);
            this.addBigCoveredCableSizedCube(facing, cubeBuilder);

            // Reset back to normal rendering for the rest
            cubeBuilder.setEmissiveMaterial(false);
            cubeBuilder.setTexture(texture);
        }

        addCoveredCableSizedCube(facing, cubeBuilder);

        // Render the channel indicators brightly lit at night
        cubeBuilder.setEmissiveMaterial(true);

        cubeBuilder.setTexture(oddChannel);
        cubeBuilder.setColorRGB(cableColor.blackVariant);
        addCoveredCableSizedCube(facing, cubeBuilder);

        cubeBuilder.setTexture(evenChannel);
        cubeBuilder.setColorRGB(cableColor.whiteVariant);
        addCoveredCableSizedCube(facing, cubeBuilder);

        // Reset back to default
        cubeBuilder.setEmissiveMaterial(false);
    }

    public void addStraightSmartConnection(EnumFacing facing, AEColor cableColor, int channels,
            List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // Flip side texture as needed depending on direction
        switch (facing) {
            case EAST, WEST -> cubeBuilder.setFlipV(EnumFacing.DOWN, true);
            case UP, DOWN -> cubeBuilder.setFlipU(EnumFacing.NORTH, true);
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.SMART).get(cableColor);
        cubeBuilder.setTexture(texture);

        setStraightCableUVs(cubeBuilder, facing, 5 / 16f, 11 / 16f);

        addStraightCoveredCableSizedCube(facing, cubeBuilder);

        TextureAtlasSprite oddChannel = this.smartCableTextures.getOddTextureForChannels(channels);
        TextureAtlasSprite evenChannel = this.smartCableTextures.getEvenTextureForChannels(channels);

        // Render the channel indicators brightly lit at night
        cubeBuilder.setEmissiveMaterial(true);

        cubeBuilder.setTexture(oddChannel);
        cubeBuilder.setColorRGB(cableColor.blackVariant);
        addStraightCoveredCableSizedCube(facing, cubeBuilder);

        cubeBuilder.setTexture(evenChannel);
        cubeBuilder.setColorRGB(cableColor.whiteVariant);
        addStraightCoveredCableSizedCube(facing, cubeBuilder);

        // Reset back to default
        cubeBuilder.setEmissiveMaterial(false);
    }

    public void addConstrainedSmartConnection(EnumFacing facing, AEColor cableColor, int distanceFromEdge, int channels,
            List<BakedQuad> quadsOut) {
        // Same as with covered cables, the smart cable's core extends up to 5 voxels
        // away from the edge.
        // Drawing a connection to any point before that point is fruitless
        if (distanceFromEdge >= 5) {
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // Flip side texture as needed depending on direction
        switch (facing) {
            case UP, DOWN:
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
                cubeBuilder.setFlipU(EnumFacing.NORTH, true);
                break;
            case EAST, WEST:
                cubeBuilder.setFlipV(EnumFacing.DOWN, true);
                break;
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.SMART).get(cableColor);
        cubeBuilder.setTexture(texture);

        addCoveredCableSizedCube(facing, distanceFromEdge, cubeBuilder);

        TextureAtlasSprite oddChannel = this.smartCableTextures.getOddTextureForChannels(channels);
        TextureAtlasSprite evenChannel = this.smartCableTextures.getEvenTextureForChannels(channels);

        // Render the channel indicators brightly lit at night
        cubeBuilder.setEmissiveMaterial(true);

        cubeBuilder.setTexture(oddChannel);
        cubeBuilder.setColorRGB(cableColor.blackVariant);
        addCoveredCableSizedCube(facing, distanceFromEdge, cubeBuilder);

        cubeBuilder.setTexture(evenChannel);
        cubeBuilder.setColorRGB(cableColor.whiteVariant);
        addCoveredCableSizedCube(facing, distanceFromEdge, cubeBuilder);

        // Reset back to default
        cubeBuilder.setEmissiveMaterial(false);
    }

    public void addDenseCoveredConnection(EnumFacing facing, AEColor cableColor, AECableType connectionType,
            boolean cableBusAdjacent, List<BakedQuad> quadsOut) {
        // Dense cables only render their connections as dense if the adjacent blocks
        // actually wants that
        if (connectionType == AECableType.COVERED || connectionType == AECableType.SMART
                || connectionType == AECableType.GLASS) {
            this.addCoveredConnection(facing, cableColor, connectionType, cableBusAdjacent, quadsOut);
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the one on the connection side
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.DENSE_COVERED).get(cableColor);
        cubeBuilder.setTexture(texture);

        addDenseCableSizedCube(facing, cubeBuilder);

        // Reset back to normal rendering for the rest
        cubeBuilder.setEmissiveMaterial(false);
        cubeBuilder.setTexture(texture);
    }

    public void addDenseSmartConnection(EnumFacing facing, AEColor cableColor, AECableType connectionType,
            boolean cableBusAdjacent, int channels, List<BakedQuad> quadsOut) {
        // Dense cables only render their connections as dense if the adjacent blocks
        // actually wants that
        if (connectionType == AECableType.SMART) {
            this.addSmartConnection(facing, cableColor, connectionType, cableBusAdjacent, channels, quadsOut);
            return;
        } else if (connectionType == AECableType.COVERED || connectionType == AECableType.GLASS) {
            this.addCoveredConnection(facing, cableColor, connectionType, cableBusAdjacent, quadsOut);
            return;
        } else if (connectionType == AECableType.DENSE_COVERED) {
            this.addDenseCoveredConnection(facing, cableColor, connectionType, cableBusAdjacent, quadsOut);
            return;
        }

        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // We render all faces except the one on the connection side
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(facing)));

        // Flip end or side texture as needed depending on direction
        switch (facing) {
            case WEST, EAST -> cubeBuilder.setFlipV(EnumFacing.DOWN, true);
            case UP, DOWN -> {
                cubeBuilder.setFlipU(EnumFacing.NORTH, true);
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
            }
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.DENSE_SMART).get(cableColor);
        cubeBuilder.setTexture(texture);

        addDenseCableSizedCube(facing, cubeBuilder);

        // Dense cables show used channels in groups of 4, rounded up
        channels = (channels + 3) / 4;

        TextureAtlasSprite oddChannel = this.smartCableTextures.getOddTextureForDenseChannels(channels);
        TextureAtlasSprite evenChannel = this.smartCableTextures.getEvenTextureForDenseChannels(channels);

        // Render the channel indicators brightly lit at night
        cubeBuilder.setEmissiveMaterial(true);

        cubeBuilder.setTexture(oddChannel);
        cubeBuilder.setColorRGB(cableColor.blackVariant);
        addDenseCableSizedCube(facing, cubeBuilder);

        cubeBuilder.setTexture(evenChannel);
        cubeBuilder.setColorRGB(cableColor.whiteVariant);
        addDenseCableSizedCube(facing, cubeBuilder);

        // Reset back to normal rendering for the rest
        cubeBuilder.setEmissiveMaterial(false);
        cubeBuilder.setTexture(texture);
    }

    public void addStraightDenseCoveredConnection(EnumFacing facing, AEColor cableColor, List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.DENSE_COVERED).get(cableColor);
        cubeBuilder.setTexture(texture);

        setStraightCableUVs(cubeBuilder, facing, 3 / 16f, 13 / 16f);

        addStraightDenseCableSizedCube(facing, cubeBuilder);
    }

    public void addStraightDenseSmartConnection(EnumFacing facing, AEColor cableColor, int channels,
            List<BakedQuad> quadsOut) {
        CubeBuilder cubeBuilder = new CubeBuilder(quadsOut);

        // Flip end or side texture as needed depending on direction
        switch (facing) {
            case NORTH -> cubeBuilder.setFlipU(EnumFacing.NORTH, true);
            case WEST -> {
                cubeBuilder.setFlipV(EnumFacing.DOWN, true);
                cubeBuilder.setFlipU(EnumFacing.EAST, true);
            }
            case DOWN -> {
                cubeBuilder.setFlipU(EnumFacing.NORTH, true);
                cubeBuilder.setFlipV(EnumFacing.DOWN, true);
            }
        }

        TextureAtlasSprite texture = this.connectionTextures.get(AECableType.DENSE_SMART).get(cableColor);
        cubeBuilder.setTexture(texture);

        setStraightCableUVs(cubeBuilder, facing, 3 / 16f, 13 / 16f);

        addStraightDenseCableSizedCube(facing, cubeBuilder);

        // Dense cables show used channels in groups of 4, rounded up
        channels = (channels + 3) / 4;

        TextureAtlasSprite oddChannel = this.smartCableTextures.getOddTextureForDenseChannels(channels);
        TextureAtlasSprite evenChannel = this.smartCableTextures.getEvenTextureForDenseChannels(channels);

        // Render the channel indicators brightly lit at night
        cubeBuilder.setEmissiveMaterial(true);

        cubeBuilder.setTexture(oddChannel);
        cubeBuilder.setColorRGB(cableColor.blackVariant);
        addStraightDenseCableSizedCube(facing, cubeBuilder);

        cubeBuilder.setTexture(evenChannel);
        cubeBuilder.setColorRGB(cableColor.whiteVariant);
        addStraightDenseCableSizedCube(facing, cubeBuilder);

        // Reset back to default
        cubeBuilder.setEmissiveMaterial(false);
    }

    private static void addDenseCableSizedCube(EnumFacing facing, CubeBuilder cubeBuilder) {
        switch (facing) {
            case DOWN -> cubeBuilder.addCube(4, 0, 4, 12, 5, 12);
            case EAST -> cubeBuilder.addCube(11, 4, 4, 16, 12, 12);
            case NORTH -> cubeBuilder.addCube(4, 4, 0, 12, 12, 5);
            case SOUTH -> cubeBuilder.addCube(4, 4, 11, 12, 12, 16);
            case UP -> cubeBuilder.addCube(4, 11, 4, 12, 16, 12);
            case WEST -> cubeBuilder.addCube(0, 4, 4, 5, 12, 12);
        }
    }

    // Adds a cube to the given cube builder that has the size of a dense cable
    // connection and spans the entire block
    // for the given direction
    private static void addStraightDenseCableSizedCube(EnumFacing facing, CubeBuilder cubeBuilder) {
        // Go slightly beyond the block (-0.01f and 16.01f) to prevent z-fighting with facades.
        // See https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/6889
        switch (facing) {
            case DOWN, UP -> {
                cubeBuilder.setUvRotation(EnumFacing.EAST, 2);
                cubeBuilder.addCube(3, -0.01f, 3, 13, 16.01f, 13);
                cubeBuilder.setUvRotation(EnumFacing.EAST, 0);
            }
            case EAST, WEST -> {
                cubeBuilder.setUvRotation(EnumFacing.SOUTH, 2);
                cubeBuilder.setUvRotation(EnumFacing.NORTH, 2);
                cubeBuilder.addCube(-0.01f, 3, 3, 16.01f, 13, 13);
                cubeBuilder.setUvRotation(EnumFacing.SOUTH, 0);
                cubeBuilder.setUvRotation(EnumFacing.NORTH, 0);
            }
            case NORTH, SOUTH -> {
                cubeBuilder.setUvRotation(EnumFacing.EAST, 2);
                cubeBuilder.setUvRotation(EnumFacing.WEST, 2);
                cubeBuilder.addCube(3, 3, -0.01f, 13, 13, 16.01f);
                cubeBuilder.setUvRotation(EnumFacing.EAST, 0);
                cubeBuilder.setUvRotation(EnumFacing.WEST, 0);
            }
        }
    }

    // Adds a cube to the given cube builder that has the size of a covered cable
    // connection from the core of the cable
    // to the given face
    private static void addCoveredCableSizedCube(EnumFacing facing, CubeBuilder cubeBuilder) {
        switch (facing) {
            case DOWN -> cubeBuilder.addCube(6, 0, 6, 10, 5, 10);
            case EAST -> cubeBuilder.addCube(11, 6, 6, 16, 10, 10);
            case NORTH -> cubeBuilder.addCube(6, 6, 0, 10, 10, 5);
            case SOUTH -> cubeBuilder.addCube(6, 6, 11, 10, 10, 16);
            case UP -> cubeBuilder.addCube(6, 11, 6, 10, 16, 10);
            case WEST -> cubeBuilder.addCube(0, 6, 6, 5, 10, 10);
        }
    }

    // Adds a cube to the given cube builder that has the size of a covered cable
    // connection and spans the entire block
    // for the given direction
    private static void addStraightCoveredCableSizedCube(EnumFacing facing, CubeBuilder cubeBuilder) {
        switch (facing) {
            case DOWN, UP -> {
                cubeBuilder.setUvRotation(EnumFacing.EAST, 2);
                cubeBuilder.addCube(5, 0, 5, 11, 16, 11);
                cubeBuilder.setUvRotation(EnumFacing.EAST, 0);
            }
            case EAST, WEST -> {
                cubeBuilder.setUvRotation(EnumFacing.SOUTH, 2);
                cubeBuilder.setUvRotation(EnumFacing.NORTH, 2);
                cubeBuilder.addCube(0, 5, 5, 16, 11, 11);
                cubeBuilder.setUvRotation(EnumFacing.SOUTH, 0);
                cubeBuilder.setUvRotation(EnumFacing.NORTH, 0);
            }
            case NORTH, SOUTH -> {
                cubeBuilder.setUvRotation(EnumFacing.EAST, 2);
                cubeBuilder.setUvRotation(EnumFacing.WEST, 2);
                cubeBuilder.addCube(5, 5, 0, 11, 11, 16);
                cubeBuilder.setUvRotation(EnumFacing.EAST, 0);
                cubeBuilder.setUvRotation(EnumFacing.WEST, 0);
            }
        }
    }

    private static void addCoveredCableSizedCube(EnumFacing facing, int distanceFromEdge, CubeBuilder cubeBuilder) {
        switch (facing) {
            case DOWN -> cubeBuilder.addCube(6, distanceFromEdge, 6, 10, 5, 10);
            case EAST -> cubeBuilder.addCube(11, 6, 6, 16 - distanceFromEdge, 10, 10);
            case NORTH -> cubeBuilder.addCube(6, 6, distanceFromEdge, 10, 10, 5);
            case SOUTH -> cubeBuilder.addCube(6, 6, 11, 10, 10, 16 - distanceFromEdge);
            case UP -> cubeBuilder.addCube(6, 11, 6, 10, 16 - distanceFromEdge, 10);
            case WEST -> cubeBuilder.addCube(distanceFromEdge, 6, 6, 5, 10, 10);
        }
    }

    /**
     * This renders a slightly bigger covered cable connection to the specified side. This is used to connect cable
     * cores with adjacent machines that do not want to be connected to using a glass cable connection. This applies to
     * most machines (interfaces, etc.)
     */
    private void addBigCoveredCableSizedCube(EnumFacing facing, CubeBuilder cubeBuilder) {
        switch (facing) {
            case DOWN -> cubeBuilder.addCube(5, 0, 5, 11, 4, 11);
            case EAST -> cubeBuilder.addCube(12, 5, 5, 16, 11, 11);
            case NORTH -> cubeBuilder.addCube(5, 5, 0, 11, 11, 4);
            case SOUTH -> cubeBuilder.addCube(5, 5, 12, 11, 11, 16);
            case UP -> cubeBuilder.addCube(5, 12, 5, 11, 16, 11);
            case WEST -> cubeBuilder.addCube(0, 5, 5, 4, 11, 11);
        }
    }

    public TextureAtlasSprite getCoreTexture(CableCoreType coreType, AEColor color) {
        return this.coreTextures.get(coreType).get(color);
    }
}
