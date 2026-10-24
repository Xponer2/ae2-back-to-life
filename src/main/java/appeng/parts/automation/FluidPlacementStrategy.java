package appeng.parts.automation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.behaviors.PlacementStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.core.tags.FluidTags;
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

        // We do not support placing fluids with patched components for now
        if (!fluidKey.toStack(1).getComponentsPatch().isEmpty()) {
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
            if (level.dimensionType().ultraWarm() && FluidTags.WATER.contains(fluid)) {
                playEvaporationEffect(level, pos);
            } else if (state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer
                    && fluid == FluidRegistry.WATER) {
                liquidBlockContainer.placeLiquid(level, pos, state, ((FlowingFluid) fluid).getSource(false));
                playEmptySound(level, pos, fluid);
            } else {
                if (state.canBeReplaced(fluid) && !state.liquid()) {
                    level.destroyBlock(pos, true);
                }

                if (!level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE)
                        && !state.getFluidState().isSource()) {
                    return 0;
                } else {
                    playEmptySound(level, pos, fluid);
                }
            }
        }

        return AEFluidKey.AMOUNT_BLOCK;
    }

    private void playEmptySound(World level, BlockPos pos, Fluid fluid) {
        if (throttleEffect()) {
            return;
        }

        SoundEvent soundEvent = FluidTags.LAVA.contains(fluid) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        level.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.FLUID_PLACE, pos, GameEvent.Context.of(null, null));
    }

    private void playEvaporationEffect(World level, BlockPos pos) {
        if (throttleEffect()) {
            return;
        }

        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                2.6F + (level.rand.nextFloat() - level.rand.nextFloat()) * 0.8F);

        for (int l = 0; l < 8; ++l) {
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    (double) pos.getX() + Math.random(),
                    (double) pos.getY() + Math.random(),
                    (double) pos.getZ() + Math.random(),
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    /**
     * Checks from {@link net.minecraft.world.item.BucketItem#emptyContents}
     */
    private boolean canPlace(WorldServer level, IBlockState state, BlockPos pos, Fluid fluid) {
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }

        // This check is in addition to vanilla's checks. If the fluid is already in place,
        // don't place it again. This is for water, since water is otherwise replaceable by water.
        if (state == fluid.defaultFluidState().createLegacyBlock()) {
            return false;
        }

        if (Platform.isAir(state)) {
            return true;
        }

        if (state.canBeReplaced(fluid)) {
            return true;
        }

        if (state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
            var fakePlayer = Platform.getFakePlayer(level, owningPlayerId);
            return liquidBlockContainer.canPlaceLiquid(fakePlayer, level, pos, state, fluid);
        }

        return false;
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
