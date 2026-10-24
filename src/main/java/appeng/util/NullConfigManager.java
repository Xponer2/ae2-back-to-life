package appeng.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;

public final class NullConfigManager implements IConfigManager {
    public static final NullConfigManager INSTANCE = new NullConfigManager();

    @Override
    public Set<Setting<?>> getSettings() {
        return Collections.emptySet();
    }

    @Override
    public <T extends Enum<T>> T getSetting(Setting<T> setting) {
        throw new IllegalStateException("Trying to get unsupported setting " + setting.getName());
    }

    @Override
    public <T extends Enum<T>> void putSetting(Setting<T> setting, T newValue) {
        throw new IllegalStateException("Trying to set unsupported setting " + setting.getName());
    }

    @Override
    public void writeToNBT(NBTTagCompound destination) {
    }

    @Override
    public boolean readFromNBT(NBTTagCompound src) {
        return false;
    }

    @Override
    public boolean importSettings(Map<String, String> settings) {
        return false;
    }

    @Override
    public Map<String, String> exportSettings() {
        return Collections.emptyMap();
    }
}
