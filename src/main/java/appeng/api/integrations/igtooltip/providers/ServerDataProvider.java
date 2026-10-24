package appeng.api.integrations.igtooltip.providers;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

@ApiStatus.NonExtendable
@ApiStatus.OverrideOnly
@FunctionalInterface
public interface ServerDataProvider<T> {
    void provideServerData(EntityPlayer player, T object, NBTTagCompound serverData);
}
