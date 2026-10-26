/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.theoneprobe;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.apiimpl.client.ElementTextRender;

/**
 * 1.12.2 only: a line of The One Probe that shows a text component. The One Probe 1.12.2 creates its tooltip on the
 * server and its text elements can only translate keys without arguments; this element sends the component as JSON
 * and translates it when the client renders it, like {@code IProbeInfo.mcText} of later versions.
 */
public final class TextComponentElement implements IElement {
    private static int elementId = -1;

    private final ITextComponent text;

    @Nullable
    private String formattedText;

    public TextComponentElement(ITextComponent text) {
        this.text = text;
    }

    public TextComponentElement(ByteBuf buf) {
        var parsed = ITextComponent.Serializer.jsonToComponent(ByteBufUtils.readUTF8String(buf));
        this.text = parsed != null ? parsed : new TextComponentString("");
    }

    static void setElementId(int id) {
        elementId = id;
    }

    static boolean isRegistered() {
        return elementId != -1;
    }

    /**
     * Only called on the client.
     */
    private String getFormattedText() {
        if (formattedText == null) {
            formattedText = text.getFormattedText();
        }
        return formattedText;
    }

    @Override
    public void render(int x, int y) {
        ElementTextRender.render(getFormattedText(), x, y);
    }

    @Override
    public int getWidth() {
        return ElementTextRender.getWidth(getFormattedText());
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, ITextComponent.Serializer.componentToJson(text));
    }

    @Override
    public int getID() {
        return elementId;
    }
}
