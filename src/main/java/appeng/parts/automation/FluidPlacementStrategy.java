package appeng.parts.automation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;

import appeng.api.behaviors.PlacementStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.util.Platform;

public class FluidPlacementStrategy implements PlacementStrategy {
    private final WorldServer level;
    private final BlockPos pos;
    private final EnumFacing side;
    @Nullable
    private final UUID owningPlayerId;
    /**
     * The fluids that we tried to place unsuccessfully.
     */
    private final Set<Fluid> blocked = new HashSet<>();
    /**
     * {@link System#currentTimeMillis()} of when the last sound/visual effect was played by this plane.
     */
    private long lastEffect;

    public FluidPlacementStrategy(WorldServer level, BlockPos pos, EnumFacing side, TileEntity host,
            @Nullable UUID owningPlayerId) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.owningPlayerId = owningPlayerId;
    }

    @Override
    public void clearBlocked() {
        blocked.clear();
    }

    @Override
    public long placeInWorld(AEKey f, long amount, Actionable type, boolean placeAsEntity) {
        if (placeAsEntity || !(f instanceof AEFluidKey fluidKey)) {
            return 0; // Not supported
        }

        if (amount < AEFluidKey.AMOUNT_BLOCK) {
            // need a full bucket
            return 0;
        }

        var fluid = fluidKey.getFluid();

        // We previously tried placing this fluid unsuccessfully, so don't check it again.
        if (blocked.contains(fluid)) {
            return 0;
        }

        // We do not support placing fluids with NBT for now
        if (fluidKey.getTag() != null) {
            return 0;
        }

        var state = level.getBlockState(pos);

        if (!this.canPlace(level, state, pos, fluid)) {
            // Remember that this fluid cannot be placed right now.
            blocked.add(fluid);
            return 0;
        }

        if (type == Actionable.MODULATE) {
            // Placing water in nether voids the fluid, but plays effects
            if (level.provider.doesWaterVaporize()
                    && fluid.doesVaporize(fluidKey.toStack((int) AEFluidKey.AMOUNT_BLOCK))) {
                playEvaporationEffect(level, pos);
            } else {
                if (state.getBlock().isReplaceable(level, pos) && !state.getMaterial().isLiquid()) {
                    level.destroyBlock(pos, true);
                }

                if (!level.setBlockState(pos, fluid.getBlock().getDefaultState(), 11)) {
                    return 0;
                } else {
                    playEmptySound(level, pos, fluidKey);
                }
            }
        }

        return AEFluidKey.AMOUNT_BLOCK;
    }

    private void playEmptySound(World level, BlockPos pos, AEFluidKey fluid) {
        if (throttleEffect()) {
            return;
        }

        var soundEvent = fluid.getFluid().getEmptySound(fluid.toStack((int) AEFluidKey.AMOUNT_BLOCK));
        level.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private void playEvaporationEffect(WorldServer level, BlockPos pos) {
        if (throttleEffect()) {
            return;
        }

        level.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (level.rand.nextFloat() - level.rand.nextFloat()) * 0.8F);

        // The server version sends the particles to the players around
        level.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8,
                0.25, 0.25, 0.25, 0.0);
    }

    /**
     * Checks like the bucket of the game.
     */
    private boolean canPlace(WorldServer level, IBlockState state, BlockPos pos, Fluid fluid) {
        var fluidBlock = fluid.getBlock();
        if (fluidBlock == null) {
            // Fluids without a block cannot exist in the world
            return false;
        }

        // This check is in addition to vanilla's checks. If the fluid is already in place,
        // don't place it again. This is for water, since water is otherwise replaceable by water.
        if (state == fluidBlock.getDefaultState()) {
            return false;
        }

        if (Platform.isAir(state)) {
            return true;
        }

        return state.getBlock().isReplaceable(level, pos);
    }

    /**
     * Only play the effect every 250ms.
     */
    protected final boolean throttleEffect() {
        long now = System.currentTimeMillis();
        if (now < lastEffect + 250) {
            return true;
        }
        lastEffect = now;
        return false;
    }

}
