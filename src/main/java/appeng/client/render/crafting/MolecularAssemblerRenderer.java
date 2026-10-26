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

package appeng.client.render.crafting;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.client.render.PoseStack;
import appeng.client.render.effects.AEParticles;
import appeng.client.render.effects.ParticleTypes;
import appeng.client.render.tesr.AEBlockEntityRenderer;
import appeng.core.AppEng;
import appeng.core.AppEngClient;
import appeng.init.client.InitAdditionalModels;

/**
 * Renders the item currently being crafted by the molecular assembler, as well as the light strip when it's powered.
 */
@SideOnly(Side.CLIENT)
public class MolecularAssemblerRenderer extends AEBlockEntityRenderer<MolecularAssemblerBlockEntity> {

    public static final ModelResourceLocation LIGHTS_MODEL = InitAdditionalModels
            .standalone(AppEng.makeId("block/molecular_assembler_lights"));

    private final Random particleRandom = new Random();

    @Override
    public void render(MolecularAssemblerBlockEntity molecularAssembler, float partialTicks, PoseStack ms,
            int combinedLightIn) {

        AssemblerAnimationStatus status = molecularAssembler.getAnimationStatus();
        if (status != null) {
            if (!Minecraft.getMinecraft().isGamePaused()) {
                if (status.isExpired()) {
                    molecularAssembler.setAnimationStatus(null);
                }

                status.setAccumulatedTicks(status.getAccumulatedTicks() + partialTicks);
                status.setTicksUntilParticles(status.getTicksUntilParticles() - partialTicks);
            }

            renderStatus(molecularAssembler, ms, combinedLightIn, status);
        }

        if (molecularAssembler.isPowered()) {
            renderPowerLight();
        }
    }

    private void renderPowerLight() {
        Minecraft minecraft = Minecraft.getMinecraft();
        var lightsModel = minecraft.getBlockRendererDispatcher().getBlockModelShapes().getModelManager()
                .getModel(LIGHTS_MODEL);

        // 1.21 uses the tripwire layer for its shader properties: alpha testing and translucency.
        // The light strip is emissive, so it is drawn full-bright.
        // Restored explicitly, popAttrib would not update the GlStateManager cache
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.disableLighting();
        BlockEntityRenderHelper.setLightmap(BlockEntityRenderHelper.FULL_BRIGHT);
        minecraft.getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(lightsModel, 1, 1,
                1, 1);
        if (lighting) {
            GlStateManager.enableLighting();
        }
        if (!blend) {
            GlStateManager.disableBlend();
        }
    }

    private void renderStatus(MolecularAssemblerBlockEntity molecularAssembler, PoseStack ms,
            int combinedLightIn, AssemblerAnimationStatus status) {
        double centerX = molecularAssembler.getPos().getX() + 0.5f;
        double centerY = molecularAssembler.getPos().getY() + 0.5f;
        double centerZ = molecularAssembler.getPos().getZ() + 0.5f;

        // Spawn crafting FX that fly towards the block's center
        if (status.getTicksUntilParticles() <= 0) {
            status.setTicksUntilParticles(4);
            if (AppEngClient.instance().shouldAddParticles(particleRandom)) {
                for (int x = 0; x < (int) Math.ceil(status.getSpeed() / 5.0); x++) {
                    AEParticles.create(ParticleTypes.CRAFTING, centerX, centerY, centerZ, 0, 0, 0);
                }
            }
        }

        ItemStack is = status.getIs();

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5); // Translate to center of block

        if (!(is.getItem() instanceof ItemBlock)) {
            ms.translate(0, -0.3f, 0);
        } else {
            ms.translate(0, -0.2f, 0);
        }

        BlockEntityRenderHelper.renderItem(is, ItemCameraTransforms.TransformType.GROUND, combinedLightIn);
        ms.popPose();
    }

}
