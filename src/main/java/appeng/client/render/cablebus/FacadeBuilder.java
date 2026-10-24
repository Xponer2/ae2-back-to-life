/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
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
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.block.BlockBreakable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.util.AEAxisAlignedBB;
import appeng.parts.misc.CableAnchorPart;
import appeng.thirdparty.codechicken.lib.model.CachedFormat;
import appeng.thirdparty.codechicken.lib.model.Quad;
import appeng.thirdparty.codechicken.lib.model.pipeline.BakedPipeline;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadClamper;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadCornerKicker;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadFaceStripper;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadReInterpolator;
import appeng.thirdparty.codechicken.lib.model.pipeline.transformers.QuadTinter;

/**
 * The FacadeBuilder builds for facades..
 *
 * @author covers1624
 */
@SideOnly(Side.CLIENT)
public class FacadeBuilder {

    // Slightly smaller than a pixel to never show the beginning of the second row of pixels of the block's texture.
    public static final double THIN_THICKNESS = 1D / 16D - 2e-3;

    public static final AxisAlignedBB[] THIN_FACADE_BOXES = new AxisAlignedBB[] {
            new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, THIN_THICKNESS, 1.0),
            new AxisAlignedBB(0.0, 1.0 - THIN_THICKNESS, 0.0, 1.0, 1.0, 1.0),
            new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, THIN_THICKNESS),
            new AxisAlignedBB(0.0, 0.0, 1.0 - THIN_THICKNESS, 1.0, 1.0, 1.0),
            new AxisAlignedBB(0.0, 0.0, 0.0, THIN_THICKNESS, 1.0, 1.0),
            new AxisAlignedBB(1.0 - THIN_THICKNESS, 0.0, 0.0, 1.0, 1.0, 1.0) };

    private final ThreadLocal<BakedPipeline> pipelines = ThreadLocal.withInitial(() -> BakedPipeline.builder()
            // Clamper is responsible for clamping the vertex to the bounds specified.
            .addElement("clamper", QuadClamper.FACTORY)
            // Strips faces if they match a mask.
            .addElement("face_stripper", QuadFaceStripper.FACTORY)
            // Kicks the edge inner corners in, solves Z fighting
            .addElement("corner_kicker", QuadCornerKicker.FACTORY)
            // Re-Interpolates the UV's for the quad.
            .addElement("interp", QuadReInterpolator.FACTORY)
            // Tints the quad if we need it to. Disabled by default.
            .addElement("tinter", QuadTinter.FACTORY, false)
            .build());

    private final ThreadLocal<Quad> collectors = ThreadLocal.withInitial(Quad::new);

    // Pre-rotated transparent facade quads
    private final Map<EnumFacing, List<BakedQuad>> transparentFacadeQuads;

    private final Map<EnumFacing, List<BakedQuad>> cableAnchorStilts;

    /**
     * @param modelBaker             Bakes the cable anchor models.
     * @param transparentFacadeModel The border shown in place of facades in transparent facade mode, null for item
     *                               models.
     */
    public FacadeBuilder(Function<ResourceLocation, IBakedModel> modelBaker,
            @Nullable IBakedModel transparentFacadeModel) {
        cableAnchorStilts = buildCableAnchorStems(modelBaker);

        // Pre-rotate the transparent facade model to all possible sides so that we can
        // add it quicker later.
        this.transparentFacadeQuads = new EnumMap<>(EnumFacing.class);
        for (EnumFacing facing : EnumFacing.values()) {
            // This can be null for item models.
            if (transparentFacadeModel != null) {
                List<BakedQuad> partQuads = gatherQuads(transparentFacadeModel, null, 0);
                // Rotate quads accordingly
                this.transparentFacadeQuads.put(facing, QuadRotator.get(facing, 0).transform(partQuads));
            } else {
                // Item models: transparent facades are not a concern
                this.transparentFacadeQuads.put(facing, Collections.emptyList());
            }
        }
    }

    /**
     * Build a map of pre-rotated cable anchor stilts, which are the shortened cable anchors that will still be visible
     * for facades attached to a cable.
     */
    private Map<EnumFacing, List<BakedQuad>> buildCableAnchorStems(Function<ResourceLocation, IBakedModel> baker) {
        Map<EnumFacing, List<BakedQuad>> stems = new EnumMap<>(EnumFacing.class);

        List<BakedQuad> cableAnchorQuads = new ArrayList<>();
        for (ResourceLocation model : CableAnchorPart.FACADE_MODELS.getModels()) {
            IBakedModel cableAnchor = baker.apply(model);
            if (cableAnchor != null) {
                cableAnchorQuads.addAll(gatherQuads(cableAnchor, null, 0));
            }
        }

        // Create pre-rotated variants of the cable anchor stems
        for (EnumFacing side : EnumFacing.values()) {
            stems.put(side, QuadRotator.get(side, 0).transform(cableAnchorQuads));
        }

        return stems;
    }

    /**
     * Adds the quads of all facades of the given cable bus.
     *
     * @param layer The render layer, or null to add the quads for all layers.
     */
    public void buildFacadeQuads(@Nullable BlockRenderLayer layer, CableBusRenderState renderState, long rand,
            List<BakedQuad> quads) {
        BakedPipeline pipeline = this.pipelines.get();
        Quad collectorQuad = this.collectors.get();
        boolean transparent = PartHelper.getCableRenderMode().transparentFacades;
        Map<EnumFacing, FacadeRenderState> facadeStates = renderState.getFacades();
        List<AxisAlignedBB> partBoxes = renderState.getBoundingBoxes();
        Set<EnumFacing> sidesWithParts = renderState.getAttachments().keySet();
        IBlockAccess level = renderState.getWorld();
        BlockPos pos = renderState.getPos();
        BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
        // The stilts and the border of transparent facades are rendered with the cable
        boolean cableLayer = layer == null || layer == BlockRenderLayer.CUTOUT;

        for (Entry<EnumFacing, FacadeRenderState> entry : facadeStates.entrySet()) {
            EnumFacing side = entry.getKey();
            int sideIndex = side.ordinal();
            FacadeRenderState facadeRenderState = entry.getValue();
            boolean renderStilt = !sidesWithParts.contains(side);
            if (renderStilt && cableLayer) {
                quads.addAll(cableAnchorStilts.get(side));
            }

            // When we're forcing transparent facades, add a "border" model that indicates
            // where the facade is,
            // But otherwise skip the rest.
            if (transparent) {
                if (cableLayer) {
                    quads.addAll(transparentFacadeQuads.get(side));
                }
                continue;
            }

            if (level == null || pos == null) {
                continue;
            }

            IBlockState blockState = facadeRenderState.getSourceBlock();

            // If we aren't forcing transparency let the block decide if it should render.
            if (layer != null && !blockState.getBlock().canRenderInLayer(blockState, layer)) {
                continue;
            }

            AxisAlignedBB fullBounds = THIN_FACADE_BOXES[sideIndex];
            AxisAlignedBB facadeBox = fullBounds;
            // If we are a transparent facade, we need to modify out BB.
            if (facadeRenderState.isTransparent()) {
                double offset = THIN_THICKNESS;
                AEAxisAlignedBB tmpBB = null;
                for (EnumFacing face : EnumFacing.values()) {
                    // Only faces that aren't on our axis
                    if (face.getAxis() != side.getAxis()) {
                        FacadeRenderState otherState = facadeStates.get(face);
                        if (otherState != null && !otherState.isTransparent()) {
                            if (tmpBB == null) {
                                tmpBB = AEAxisAlignedBB.fromBounds(facadeBox);
                            }
                            switch (face) {
                                case DOWN -> tmpBB.minY += offset;
                                case UP -> tmpBB.maxY -= offset;
                                case NORTH -> tmpBB.minZ += offset;
                                case SOUTH -> tmpBB.maxZ -= offset;
                                case WEST -> tmpBB.minX += offset;
                                case EAST -> tmpBB.maxX -= offset;
                                default -> throw new RuntimeException("Switch falloff. " + String.valueOf(face));
                            }
                        }
                    }
                }
                if (tmpBB != null) {
                    facadeBox = tmpBB.getBoundingBox();
                }
            }

            // calculate the side mask.
            int facadeMask = 0;
            for (Entry<EnumFacing, FacadeRenderState> ent : facadeStates.entrySet()) {
                EnumFacing s = ent.getKey();
                if (s.getAxis() != side.getAxis()) {
                    FacadeRenderState otherState = ent.getValue();
                    if (!otherState.isTransparent()) {
                        facadeMask |= 1 << s.ordinal();
                    }
                }
            }

            AEAxisAlignedBB cutOutBox = getCutOutBox(facadeBox, partBoxes);
            List<AxisAlignedBB> holeStrips = getBoxes(facadeBox, cutOutBox, side.getAxis());
            var facadeAccess = new FacadeBlockAccess(level, pos, side, blockState);

            var dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            IBlockState facadeRenderBlockState = blockState;
            try {
                facadeRenderBlockState = blockState.getActualState(facadeAccess, pos);
            } catch (Exception ignored) {
            }
            IBakedModel model = dispatcher.getModelForState(facadeRenderBlockState);
            try {
                facadeRenderBlockState = blockState.getBlock().getExtendedState(facadeRenderBlockState, facadeAccess, pos);
            } catch (Exception ignored) {
            }

            // Grab out pipeline elements.
            QuadClamper clamper = pipeline.getElement("clamper", QuadClamper.class);
            QuadFaceStripper faceStripper = pipeline.getElement("face_stripper", QuadFaceStripper.class);
            QuadTinter tinter = pipeline.getElement("tinter", QuadTinter.class);
            QuadCornerKicker kicker = pipeline.getElement("corner_kicker", QuadCornerKicker.class);

            // Setup the face stripper.
            faceStripper.setBounds(fullBounds);
            faceStripper.setMask(facadeMask);

            // Setup the kicker.
            kicker.setSide(sideIndex);
            kicker.setFacadeMask(facadeMask);
            kicker.setBox(fullBounds);
            kicker.setThickness(THIN_THICKNESS);

            for (int cullFaceIdx = 0; cullFaceIdx <= EnumFacing.VALUES.length; cullFaceIdx++) {
                EnumFacing cullFace = cullFaceIdx < EnumFacing.VALUES.length ? EnumFacing.VALUES[cullFaceIdx] : null;

                // Ignore quad if it's not supposed to connect to the adjacent block.
                if (cullFace != null) {
                    BlockPos adjPos = pos.offset(cullFace);
                    IBlockState adjState = getAppearance(level, adjPos, cullFace.getOpposite());
                    if (skipRendering(blockState, adjState)) {
                        continue;
                    }
                    // Faces that are flush with the outer block-face on the side the facade is attached to are culled
                    // by the neighbor, like the 1.21 cull-face.
                    if (cullFace == side && level.getBlockState(adjPos).doesSideBlockRendering(level, adjPos,
                            side.getOpposite())) {
                        continue;
                    }
                }

                List<BakedQuad> modelQuads = getQuads(model, facadeRenderBlockState, cullFace, rand, layer);
                for (BakedQuad quad : modelQuads) {
                    // lookup the format in CachedFormat.
                    CachedFormat format = CachedFormat.lookup(quad.getFormat());

                    // If this quad has a tint index, setup the tinter.
                    // Prebake the color tint into the quad
                    if (quad.hasTintIndex()) {
                        tinter.setTint(blockColors.colorMultiplier(blockState, facadeAccess, pos,
                                quad.getTintIndex()));
                    }

                    for (AxisAlignedBB box : holeStrips) {
                        // setup the clamper for this box
                        clamper.setClampBounds(box);
                        // Reset the pipeline, clears all enabled/disabled states.
                        pipeline.reset(format);
                        // Reset out collector.
                        collectorQuad.reset(format);
                        // Enable / disable the optional elements
                        pipeline.setElementState("tinter", quad.hasTintIndex());
                        // Prepare the pipeline for a quad.
                        pipeline.prepare(collectorQuad);

                        // Pipe our quad into the pipeline.
                        quad.pipe(pipeline);
                        // Check if the collector got any data.
                        if (collectorQuad.full) {
                            // Add the result.
                            quads.add(collectorQuad.bake());
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the quads of the facade's source block for the given layer. Without a layer, the quads of all layers the
     * block renders in are combined.
     */
    private static List<BakedQuad> getQuads(IBakedModel model, IBlockState state, @Nullable EnumFacing side,
            long rand, @Nullable BlockRenderLayer layer) {
        if (layer != null) {
            return model.getQuads(state, side, rand);
        }

        List<BakedQuad> result = new ArrayList<>();
        BlockRenderLayer previousLayer = net.minecraftforge.client.MinecraftForgeClient.getRenderLayer();
        try {
            for (BlockRenderLayer forcedLayer : BlockRenderLayer.values()) {
                // Check if the block renders on the layer we want to force.
                if (state.getBlock().canRenderInLayer(state, forcedLayer)) {
                    // Force the layer and gather quads.
                    ForgeHooksClient.setRenderLayer(forcedLayer);
                    result.addAll(model.getQuads(state, side, rand));
                }
            }
        } finally {
            ForgeHooksClient.setRenderLayer(previousLayer);
        }
        return result;
    }

    /**
     * Replacement for the 1.21 {@code BlockState.getAppearance}: a neighboring cable bus looks like the facade on the
     * side that faces us.
     */
    private static IBlockState getAppearance(IBlockAccess level, BlockPos pos, EnumFacing side) {
        var te = level.getTileEntity(pos);
        if (te instanceof IPartHost partHost) {
            var facade = partHost.getFacadeContainer().getFacade(side);
            if (facade != null) {
                return facade.getBlockState();
            }
        }
        return level.getBlockState(pos);
    }

    /**
     * Replacement for the 1.21 {@code BlockState.skipRendering}: glass-like blocks (HalfTransparentBlock in 1.21,
     * BlockBreakable in 1.12.2) don't render faces towards the same block.
     */
    private static boolean skipRendering(IBlockState state, IBlockState adjacentState) {
        return state.getBlock() instanceof BlockBreakable && adjacentState == state;
    }

    /**
     * This is slow, so should be cached.
     *
     * @return The model.
     */
    public List<BakedQuad> buildFacadeItemQuads(ItemStack textureItem, EnumFacing side) {
        List<BakedQuad> facadeQuads = new ArrayList<>();
        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(textureItem, null,
                null);
        List<BakedQuad> modelQuads = gatherQuads(model, null, 0);

        BakedPipeline pipeline = this.pipelines.get();
        Quad collectorQuad = this.collectors.get();

        // Grab pipeline elements.
        QuadClamper clamper = pipeline.getElement("clamper", QuadClamper.class);
        QuadTinter tinter = pipeline.getElement("tinter", QuadTinter.class);

        for (BakedQuad quad : modelQuads) {
            // Lookup the CachedFormat for this quads format.
            CachedFormat format = CachedFormat.lookup(quad.getFormat());
            // Reset the pipeline.
            pipeline.reset(format);
            // Reset the collector.
            collectorQuad.reset(format);
            // If we have a tint index, setup the tinter and enable it.
            // Prebake the color tint into the quad
            if (quad.hasTintIndex()) {
                tinter.setTint(Minecraft.getMinecraft().getItemColors().colorMultiplier(textureItem,
                        quad.getTintIndex()));
                pipeline.enableElement("tinter");
            }
            // Disable elements we don't need for items.
            pipeline.disableElement("face_stripper");
            pipeline.disableElement("corner_kicker");
            // Setup the clamper
            clamper.setClampBounds(THIN_FACADE_BOXES[side.ordinal()]);
            // Prepare the pipeline.
            pipeline.prepare(collectorQuad);
            // Pipe our quad into the pipeline.
            quad.pipe(pipeline);
            // Check the collector for data and add the quad if there was.
            if (collectorQuad.full) {
                facadeQuads.add(collectorQuad.bake());
            }
        }
        return facadeQuads;
    }

    // Helper to gather all quads from a model into a list.
    private static List<BakedQuad> gatherQuads(IBakedModel model, @Nullable IBlockState state, long rand) {
        List<BakedQuad> modelQuads = new ArrayList<>();
        for (EnumFacing face : EnumFacing.VALUES) {
            modelQuads.addAll(model.getQuads(state, face, rand));
        }
        modelQuads.addAll(model.getQuads(state, null, rand));
        return modelQuads;
    }

    /**
     * Given the actual facade bounding box, and the bounding boxes of all parts, determine the biggest union of AABB
     * that intersect with the facade's bounding box. This AABB will need to be "cut out" when the facade is rendered.
     */
    @Nullable
    private static AEAxisAlignedBB getCutOutBox(AxisAlignedBB facadeBox, List<AxisAlignedBB> partBoxes) {
        AEAxisAlignedBB b = null;
        for (AxisAlignedBB bb : partBoxes) {
            if (bb.intersects(facadeBox)) {
                if (b == null) {
                    b = AEAxisAlignedBB.fromBounds(bb);
                } else {
                    b.maxX = Math.max(b.maxX, bb.maxX);
                    b.maxY = Math.max(b.maxY, bb.maxY);
                    b.maxZ = Math.max(b.maxZ, bb.maxZ);
                    b.minX = Math.min(b.minX, bb.minX);
                    b.minY = Math.min(b.minY, bb.minY);
                    b.minZ = Math.min(b.minZ, bb.minZ);
                }
            }
        }
        return b;
    }

    /**
     * Generates the box segments around the specified hole. If the specified hole is null, a Singleton of the Facade
     * box is returned.
     *
     * @param fb   The Facade's box.
     * @param hole The hole to 'cut'.
     * @param axis The axis the facade is on.
     * @return The box segments.
     */
    private static List<AxisAlignedBB> getBoxes(AxisAlignedBB fb, AEAxisAlignedBB hole, Axis axis) {
        if (hole == null) {
            return Collections.singletonList(fb);
        }
        List<AxisAlignedBB> boxes = new ArrayList<>();
        switch (axis) {
            case Y:
                boxes.add(new AxisAlignedBB(fb.minX, fb.minY, fb.minZ, hole.minX, fb.maxY, fb.maxZ));
                boxes.add(new AxisAlignedBB(hole.maxX, fb.minY, fb.minZ, fb.maxX, fb.maxY, fb.maxZ));

                boxes.add(new AxisAlignedBB(hole.minX, fb.minY, fb.minZ, hole.maxX, fb.maxY, hole.minZ));
                boxes.add(new AxisAlignedBB(hole.minX, fb.minY, hole.maxZ, hole.maxX, fb.maxY, fb.maxZ));

                break;
            case Z:
                boxes.add(new AxisAlignedBB(fb.minX, fb.minY, fb.minZ, fb.maxX, hole.minY, fb.maxZ));
                boxes.add(new AxisAlignedBB(fb.minX, hole.maxY, fb.minZ, fb.maxX, fb.maxY, fb.maxZ));

                boxes.add(new AxisAlignedBB(fb.minX, hole.minY, fb.minZ, hole.minX, hole.maxY, fb.maxZ));
                boxes.add(new AxisAlignedBB(hole.maxX, hole.minY, fb.minZ, fb.maxX, hole.maxY, fb.maxZ));

                break;
            case X:
                boxes.add(new AxisAlignedBB(fb.minX, fb.minY, fb.minZ, fb.maxX, hole.minY, fb.maxZ));
                boxes.add(new AxisAlignedBB(fb.minX, hole.maxY, fb.minZ, fb.maxX, fb.maxY, fb.maxZ));

                boxes.add(new AxisAlignedBB(fb.minX, hole.minY, fb.minZ, fb.maxX, hole.maxY, hole.minZ));
                boxes.add(new AxisAlignedBB(fb.minX, hole.minY, hole.maxZ, fb.maxX, hole.maxY, fb.maxZ));
                break;
            default:
                // should never happen.
                throw new RuntimeException("switch falloff. " + String.valueOf(axis));
        }

        return boxes;
    }
}
