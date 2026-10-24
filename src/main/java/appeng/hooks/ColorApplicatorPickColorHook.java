package appeng.hooks;

import appeng.core.network.PacketDistributor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.ColorApplicatorSelectColorPacket;

public final class ColorApplicatorPickColorHook {
    private ColorApplicatorPickColorHook() {
    }

    public static boolean onPickColor(EntityPlayer player, RayTraceResult hitResult) {
        if (!AEItems.COLOR_APPLICATOR.is(player.getHeldItemOffhand())
                && !AEItems.COLOR_APPLICATOR.is(player.getHeldItemMainhand())) {
            return false;
        }

        // Figure out which color...
        var be = player.world.getTileEntity(hitResult.getPos());
        if (be instanceof IColorableBlockEntity colorableBlockEntity) {
            ServerboundPacket message = new ColorApplicatorSelectColorPacket(colorableBlockEntity.getColor());
            PacketDistributor.sendToServer(message);
            return true;
        }

        return false;
    }
}
