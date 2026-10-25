package appeng.parts.automation;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockLiquid;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;

import appeng.api.behaviors.PickupSink;
import appeng.api.behaviors.PickupStrategy;
import appeng.api.config.Actionable;
import appeng.api.ids.AETags;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEFluidKey;
import appeng.core.AppEng;
import appeng.core.network.clientbound.BlockTransitionEffectPacket;

public class FluidPickupStrategy implements PickupStrategy {
    private final WorldServer level;
    private final BlockPos pos;
    private final EnumFacing side;
    @Nullable
    private final UUID owningPlayerId;

    /**
     * {@link System#currentTimeMillis()} of when the last sound/visual effect was played by this plane.
     */
    private long lastEffect;

    public FluidPickupStrategy(WorldServer level, BlockPos pos, EnumFacing side, TileEntity host,
            Map<Enchantment, Integer> enchantments, @Nullable UUID owningPlayerId) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.owningPlayerId = owningPlayerId;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean canPickUpEntity(Entity entity) {
        return false;
    }

    @Override
    public boolean pickUpEntity(IEnergySource energySource, PickupSink sink, Entity entity) {
        return false;
    }

    @Override
    public Result tryPickup(IEnergySource energySource, PickupSink sink) {
        var blockstate = level.getBlockState(pos);
        // Only fluid blocks (of the game or of Forge fluids), never the tanks of block entities
        if (blockstate.getBlock() instanceof BlockLiquid || blockstate.getBlock() instanceof IFluidBlock) {
            var handler = FluidUtil.getFluidHandler(level, pos, side);
            // Draining a full bucket only works on source blocks
            var source = handler != null ? handler.drain(Fluid.BUCKET_VOLUME, false) : null;
            if (source != null && source.amount >= Fluid.BUCKET_VOLUME) {
                if (isFluidBlacklisted(source.getFluid())) {
                    return Result.CANT_PICKUP;
                }

                // Attempt to store the fluid in the network
                var what = AEFluidKey.of(source);
                if (this.storeFluid(sink, what, source.amount, false)) {
                    // If that would succeed, actually slurp up the liquid as if we were using a
                    // bucket
                    // This _MIGHT_ change the liquid, and if it does, and we dont have enough
                    // space, tough luck. you loose the source block.
                    var drained = handler.drain(Fluid.BUCKET_VOLUME, true);
                    if (drained != null && drained.amount > 0) {
                        this.storeFluid(sink, AEFluidKey.of(drained), drained.amount, true);
                    }

                    if (!throttleEffect()) {
                        AppEng.instance().sendToAllNearExcept(null, pos.getX(), pos.getY(), pos.getZ(), 64, level,
                                new BlockTransitionEffectPacket(pos, blockstate, side,
                                        BlockTransitionEffectPacket.SoundMode.FLUID));
                    }

                    return Result.PICKED_UP;
                }
                return Result.CANT_STORE;
            }
        }

        // nothing to do here :)
        return Result.CANT_PICKUP;
    }

    private boolean storeFluid(PickupSink sink, AEFluidKey what, long amount, boolean modulate) {
        return sink.insert(what, amount, modulate ? Actionable.MODULATE : Actionable.SIMULATE) >= amount;
    }

    private boolean isFluidBlacklisted(Fluid fluid) {
        return AETags.ANNIHILATION_PLANE_FLUID_BLACKLIST.contains(fluid);
    }

    /**
     * Only play the effect every 250ms.
     */
    private boolean throttleEffect() {
        var now = System.currentTimeMillis();
        if (now < lastEffect + 250) {
            return true;
        }
        lastEffect = now;
        return false;
    }

}
