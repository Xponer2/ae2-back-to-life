package appeng.sounds;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

import appeng.core.AppEng;

public final class AppEngSounds {
    public static final ResourceLocation GUIDE_CLICK_ID = AppEng.makeId("guide.click");
    public static SoundEvent GUIDE_CLICK_EVENT = new SoundEvent(GUIDE_CLICK_ID).setRegistryName(GUIDE_CLICK_ID);

    public static void register(IForgeRegistry<SoundEvent> registry) {
        registry.register(GUIDE_CLICK_EVENT);
    }
}
