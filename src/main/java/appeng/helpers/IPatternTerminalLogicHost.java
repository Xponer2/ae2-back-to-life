package appeng.helpers;

import net.minecraft.world.World;

import appeng.parts.encoding.PatternEncodingLogic;

public interface IPatternTerminalLogicHost {
    PatternEncodingLogic getLogic();

    World getLevel();

    void markForSave();
}
