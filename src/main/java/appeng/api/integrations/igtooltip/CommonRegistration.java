package appeng.api.integrations.igtooltip;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import appeng.api.integrations.igtooltip.providers.ServerDataProvider;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface CommonRegistration {
    <T extends TileEntity> void addBlockEntityData(ResourceLocation id,
            Class<T> blockEntityClass,
            ServerDataProvider<? super T> provider);

}
