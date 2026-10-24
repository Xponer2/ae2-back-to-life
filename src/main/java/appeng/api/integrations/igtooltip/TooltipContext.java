package appeng.api.integrations.igtooltip;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

@ApiStatus.Experimental
public final class TooltipContext {
    private final NBTTagCompound serverData;
    private final Vec3d hitLocation;
    private final EntityPlayer player;

    public TooltipContext(NBTTagCompound serverData, Vec3d hitLocation, EntityPlayer player) {
        this.serverData = serverData;
        this.hitLocation = hitLocation;
        this.player = player;
    }

    public NBTTagCompound serverData() {
        return serverData;
    }

    public Vec3d hitLocation() {
        return hitLocation;
    }

    public EntityPlayer player() {
        return player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TooltipContext)) return false;
        TooltipContext that = (TooltipContext) o;
        return java.util.Objects.equals(serverData, that.serverData)
                && java.util.Objects.equals(hitLocation, that.hitLocation)
                && java.util.Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(serverData, hitLocation, player);
    }

    @Override
    public String toString() {
        return "TooltipContext[" + "serverData=" + serverData + ", " + "hitLocation=" + hitLocation + ", " + "player=" + player + "]";
    }

}
