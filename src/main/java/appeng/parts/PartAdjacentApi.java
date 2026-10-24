package appeng.parts;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;

import appeng.api.capabilities.BlockCapability;
import appeng.api.capabilities.BlockCapabilityCache;
import appeng.api.parts.IPartHost;
import appeng.util.Platform;

/**
 * Utility class to cache an API that is adjacent to a part.
 */
public class PartAdjacentApi<T> {
    private final AEBasePart part;
    private final BlockCapability<T, EnumFacing> capability;
    private final Runnable invalidationListener;
    private BlockCapabilityCache<T, EnumFacing> cache;

    public PartAdjacentApi(AEBasePart part, BlockCapability<T, EnumFacing> capability) {
        this(part, capability, () -> {
        });
    }

    public PartAdjacentApi(AEBasePart part, BlockCapability<T, EnumFacing> capability, Runnable invalidationListener) {
        this.capability = capability;
        this.part = part;
        this.invalidationListener = invalidationListener;
    }

    @Nullable
    public T find() {
        if (!(part.getLevel() instanceof WorldServer serverLevel)) {
            return null;
        }

        var host = part.getHost().getTileEntity();
        var attachedSide = part.getSide();
        var targetPos = host.getPos().offset(attachedSide);

        if (!Platform.areBlockEntitiesTicking(serverLevel, targetPos)) {
            return null;
        }

        if (cache == null) {
            cache = BlockCapabilityCache.create(
                    capability,
                    serverLevel,
                    targetPos,
                    attachedSide.getOpposite(),
                    () -> isPartValid(part),
                    invalidationListener);
        }

        return cache.getCapability();
    }

    public static boolean isPartValid(AEBasePart part) {
        var be = part.getTileEntity();
        return be instanceof IPartHost host && host.getPart(part.getSide()) == part && !be.isInvalid();
    }
}
