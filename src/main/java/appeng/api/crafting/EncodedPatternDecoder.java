package appeng.api.crafting;

import net.minecraft.world.World;

import appeng.api.stacks.AEItemKey;

@FunctionalInterface
public interface EncodedPatternDecoder<T extends IPatternDetails> {
    T decode(AEItemKey what, World level);
}
