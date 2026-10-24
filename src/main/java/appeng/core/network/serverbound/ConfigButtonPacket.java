
package appeng.core.network.serverbound;

import net.minecraft.entity.player.EntityPlayerMP;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.core.AELog;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.codec.StreamCodec;
import appeng.menu.AEBaseMenu;
import appeng.util.EnumCycler;

public final class ConfigButtonPacket implements ServerboundPacket {
    private final Setting<?> option;
    private final boolean rotationDirection;

    public ConfigButtonPacket(Setting<?> option, boolean rotationDirection) {
        this.option = option;
        this.rotationDirection = rotationDirection;
    }

    public Setting<?> option() {
        return option;
    }

    public boolean rotationDirection() {
        return rotationDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigButtonPacket)) return false;
        ConfigButtonPacket that = (ConfigButtonPacket) o;
        return java.util.Objects.equals(option, that.option)
                && rotationDirection == that.rotationDirection;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(option, rotationDirection);
    }

    @Override
    public String toString() {
        return "ConfigButtonPacket[" + "option=" + option + ", " + "rotationDirection=" + rotationDirection + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigButtonPacket> STREAM_CODEC = StreamCodec.ofMember(
            ConfigButtonPacket::write,
            ConfigButtonPacket::decode);

    public static final Type<ConfigButtonPacket> TYPE = CustomAppEngPayload.createType("config_button");

    @Override
    public Type<ConfigButtonPacket> type() {
        return TYPE;
    }

    public static ConfigButtonPacket decode(RegistryFriendlyByteBuf stream) {
        var option = Settings.getOrThrow(stream.readUtf());
        var rotationDirection = stream.readBoolean();
        return new ConfigButtonPacket(option, rotationDirection);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUtf(option.getName());
        data.writeBoolean(rotationDirection);
    }

    @Override
    public void handleOnServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AEBaseMenu baseMenu) {
            if (baseMenu.getTarget() instanceof IConfigurableObject configurableObject) {
                var cm = configurableObject.getConfigManager();
                if (cm.hasSetting(option)) {
                    cycleSetting(cm, option);
                } else {
                    AELog.info("Ignoring unsupported setting %s sent by client on %s", option,
                            baseMenu.getTarget());
                }
            }
        }
    }

    private <T extends Enum<T>> void cycleSetting(IConfigManager cm, Setting<T> setting) {
        var currentValue = cm.getSetting(setting);
        var nextValue = EnumCycler.rotateEnum(currentValue, rotationDirection, setting.getValues());
        cm.putSetting(setting, nextValue);
    }
}
