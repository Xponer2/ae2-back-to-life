package appeng.hooks;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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

    public static void onPlayerUseBlockEvent(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            // See https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/6900
            return;
        }
        var result = onPlayerUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
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
            var be = level.getTileEntity(hitResult.getPos());
            if (be instanceof AEBaseBlockEntity baseBlockEntity) {
                IS_DISASSEMBLING.set(true);
                try {
                    if (!Platform.hasPermissions(new DimensionalBlockPos(level, hitResult.getPos()), player)) {
                        return EnumActionResult.FAIL;
                    }

                    var result = baseBlockEntity.disassembleWithWrench(
                            player,
                            level,
                            hitResult,
                            itemStack);
                    if (result.consumesAction()) {
                        SoundEvent soundType = SoundEvents.ITEM_FRAME_REMOVE_ITEM;
                        level.playSound(player, hitResult.getPos(), soundType, SoundCategory.BLOCKS, 0.7F, 1.0F);
                    }
                    return result;
                } finally {
                    IS_DISASSEMBLING.remove();
                }
            }
        } else if (!InteractionUtil.isInAlternateUseMode(player) && InteractionUtil.canWrenchRotate(itemStack)) {
            var pos = hitResult.getPos();
            var state = level.getBlockState(pos);
            var strategy = IOrientationStrategy.get(state);
            if (strategy.allowsPlayerRotation()) {
                var clickedFace = hitResult.sideHit;
                var orientation = BlockOrientation.get(strategy, state);
                orientation = orientation.rotateClockwiseAround(clickedFace);
                var newState = strategy.setOrientation(state, orientation.getSide(RelativeSide.FRONT),
                        orientation.getSpin());

                if (newState != state && newState.canSurvive(level, pos)) {
                    if (!Platform.hasPermissions(new DimensionalBlockPos(level, hitResult.getPos()), player)) {
                        return EnumActionResult.FAIL;
                    }

                    level.setBlockState(pos, newState);
                    return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.PASS;
    }

}
