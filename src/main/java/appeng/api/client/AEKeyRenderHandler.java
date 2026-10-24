/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 TeamAppliedEnergistics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.client;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.stacks.AEKey;
import appeng.client.gui.GuiGraphics;
import appeng.client.render.PoseStack;
import appeng.util.Platform;

/**
 * Client-side rendering of AE stacks. Must be registered in {@link AEKeyRendering} for each storage channel!
 */
@SideOnly(Side.CLIENT)
public interface AEKeyRenderHandler<T extends AEKey> {
    /**
     * Draw the stack, for example the item or the fluid sprite, but not the amount.
     */
    void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, T stack);

    /**
     * Draw the representation of a key in-world on the face of a block. Used for displaying it on screens and monitors.
     */
    void drawOnBlockFace(PoseStack poseStack, T what, float scale, int combinedLight, World level);

    /**
     * Name of the stack, ignoring the amount.
     */
    ITextComponent getDisplayName(T stack);

    /**
     * Return the full tooltip, with the name of the stack and any additional lines.
     */
    default List<ITextComponent> getTooltip(T stack) {
        return ImmutableList.of(
                getDisplayName(stack),
                // Append the name of the mod by default as mods such as REI would also add that
                new TextComponentString(Platform.formatModName(stack.getModId())));
    }
}
