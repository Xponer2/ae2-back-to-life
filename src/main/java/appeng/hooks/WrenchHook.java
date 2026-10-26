package appeng.hooks;

import appeng.api.orientation.IOrientationHolder;
import appeng.block.AEBaseEntityBlock;
import appeng.block.AEBaseBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.RelativeSide;
import appeng.api.util.DimensionalBlockPos;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.util.InteractionUtil;
import appeng.util.Platform;

/**
 * This hooks listens for items that match a wrench tag being used on our blocks while shift is held to disassemble, and
 * to rotate if shift is not held.
 */
public final class WrenchHook {

    private static final ThreadLocal<Boolean> IS_DISASSEMBLING = new ThreadLocal<>();

    private WrenchHook() {
    }

    public static boolean isDisassembling() {
        return Boolean.TRUE.equals(IS_DISASSEMBLING.get());
    }

    @SubscribeEvent
    public static void onPlayerUseBlockEvent(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            // See https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/6900
            return;
        }
        var hitVec = event.getHitVec();
        var hit = new RayTraceResult(hitVec != null ? hitVec : new Vec3d(event.getPos()),
                event.getFace() != null ? event.getFace() : EnumFacing.UP, event.getPos());
        var result = onPlayerUseBlock(event.getEntityPlayer(), event.getWorld(), event.getHand(), hit);
        if (result != EnumActionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    public static EnumActionResult onPlayerUseBlock(EntityPlayer player,
            World level,
            EnumHand hand,
            RayTraceResult hitResult) {

        // Only handle main hand interactions
        if (player.isSpectator() || hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }

        var itemStack = player.getHeldItem(hand);

        if (InteractionUtil.isInAlternateUseMode(player) && InteractionUtil.canWrenchDisassemble(itemStack)) {
            var be = level.getTileEntity(hitResult.getBlockPos());
            if (be instanceof AEBaseBlockEntity baseBlockEntity) {
                IS_DISASSEMBLING.set(true);
                try {
                    if (!Platform.hasPermissions(new DimensionalBlockPos(level, hitResult.getBlockPos()), player)) {
                        return EnumActionResult.FAIL;
                    }

                    var result = baseBlockEntity.disassembleWithWrench(
                            player,
                            level,
                            hitResult,
                            itemStack);
                    if (result == EnumActionResult.SUCCESS) {
                        SoundEvent soundType = SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM;
                        level.playSound(player, hitResult.getBlockPos(), soundType, SoundCategory.BLOCKS, 0.7F, 1.0F);
                    }
                    return result;
                } finally {
                    IS_DISASSEMBLING.remove();
                }
            }
        } else if (!InteractionUtil.isInAlternateUseMode(player) && InteractionUtil.canWrenchRotate(itemStack)) {
            var pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos).getActualState(level, pos);
            var strategy = IOrientationStrategy.get(state);
            if (strategy.allowsPlayerRotation()) {
                var clickedFace = hitResult.sideHit;
                var orientation = BlockOrientation.get(strategy, state);
                orientation = orientation.rotateClockwiseAround(clickedFace);
                var newState = strategy.setOrientation(state, orientation.getSide(RelativeSide.FRONT),
                        orientation.getSpin());

                if (newState != state && (!(newState.getBlock() instanceof AEBaseBlock aeBlock)
                        || aeBlock.canSurvive(newState, level, pos))) {
                    if (!Platform.hasPermissions(new DimensionalBlockPos(level, pos), player)) {
                        return EnumActionResult.FAIL;
                    }

                    // Blocks with a block entity keep their orientation in it
                    if (level.getTileEntity(pos) instanceof IOrientationHolder holder
                            && newState.getBlock() instanceof AEBaseEntityBlock<?>) {
                        holder.setOrientation(BlockOrientation.get(strategy, newState));
                    } else {
                        level.setBlockState(pos, newState);
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.PASS;
    }

}
