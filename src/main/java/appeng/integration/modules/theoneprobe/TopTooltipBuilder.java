package appeng.integration.modules.theoneprobe;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import mcjty.theoneprobe.api.IProbeInfo;

import appeng.api.integrations.igtooltip.TooltipBuilder;

public class TopTooltipBuilder implements TooltipBuilder {
    private final IProbeInfo probeInfo;

    public TopTooltipBuilder(IProbeInfo probeInfo) {
        this.probeInfo = probeInfo;
    }

    @Override
    public void addLine(ITextComponent line) {
        if (TextComponentElement.isRegistered()) {
            probeInfo.element(new TextComponentElement(line));
        } else {
            probeInfo.text(line.getFormattedText());
        }
    }

    @Override
    public void addLine(ITextComponent line, ResourceLocation id) {
        addLine(line);
    }
}
