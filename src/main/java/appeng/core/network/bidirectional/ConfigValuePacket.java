package appeng.core.network.bidirectional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;

public final class ConfigValuePacket implements ClientboundPacket, ServerboundPacket {
    private final String name;
    private final String value;

    public ConfigValuePacket(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigValuePacket)) return false;
        ConfigValuePacket that = (ConfigValuePacket) o;
        return java.util.Objects.equals(name, that.name)
                && java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "ConfigValuePacket[" + "name=" + name + ", " + "value=" + value + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigValuePacket> STREAM_CODEC = StreamCodec.ofMember(
            ConfigValuePacket::write,
            ConfigValuePacket::decode);

    public static final Type<ConfigValuePacket> TYPE = CustomAppEngPayload.createType("config_value");

    @Override
    public Type<ConfigValuePacket> type() {
        return TYPE;
    }

    public static ConfigValuePacket decode(RegistryFriendlyByteBuf stream) {
        var name = stream.readUtf();
        var value = stream.readUtf();
        return new ConfigValuePacket(name, value);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUtf(name);
        data.writeUtf(value);
    }

    public <T extends Enum<T>> ConfigValuePacket(Setting<T> setting, T value) {
        this(setting.getName(), value.name());
        if (!setting.getValues().contains(value)) {
            throw new IllegalStateException(value + " not a valid value for " + setting);
        }
    }

    public <T extends Enum<T>> ConfigValuePacket(Setting<T> setting, IConfigManager configManager) {
        this(setting, setting.getValue(configManager));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (player.openContainer instanceof IConfigurableObject configurableObject) {
            loadSetting(configurableObject);
        }
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof IConfigurableObject configurableObject) {
            loadSetting(configurableObject);
        }
    }

    private void loadSetting(IConfigurableObject configurableObject) {
        var cm = configurableObject.getConfigManager();

        for (var setting : cm.getSettings()) {
            if (setting.getName().equals(this.name)) {
                setting.setFromString(cm, value);
                break;
            }
        }
    }

}
