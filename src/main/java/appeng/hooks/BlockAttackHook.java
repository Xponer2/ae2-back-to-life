package appeng.hooks;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import appeng.core.network.PacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.parts.IPartHost;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.PartLeftClickPacket;
import appeng.util.InteractionUtil;

/**
 * Handles the client->server interaction when a player left-clicks on an {@link appeng.api.parts.IPart} attached to a
 * {@link IPartHost}, and that part implements {@link appeng.api.parts.IPart#onClicked(EntityPlayer, Vec3d)} or
 * {@link appeng.api.parts.IPart#onShiftClicked(EntityPlayer, Vec3d)}.
 */
@SideOnly(Side.CLIENT)
public final class BlockAttackHook {
    private BlockAttackHook() {
    }

    public static void install() {
        MinecraftForge.EVENT_BUS.register(BlockAttackHook.class);
    }

    /**
     * We intercept when the player attacks a cable bus and send an interaction handling packet instead.
     */
    @SubscribeEvent
    public static void onBlockAttackedOnClientEvent(PlayerInteractEvent.LeftClickBlock event) {

        var level = event.getWorld();

        // Do not process this event on the server since we're handling the server-side ourselves
        if (!level.isRemote) {
            return;
        }

        var result = onBlockAttackedOnClient(event.getEntityPlayer(), level);
        if (result != EnumActionResult.PASS) {
            event.setCanceled(true);
        }
    }

    private static EnumActionResult onBlockAttackedOnClient(EntityPlayer player, World level) {
        // This shouldn't happen as the attack block logic should only be called if the player is pointing at a block
        // to begin with.
        var hitResult = Minecraft.getMinecraft().objectMouseOver;
        if (hitResult == null || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return EnumActionResult.PASS;
        }

        if (BlockAttackHook.onBlockAttackedOnClient(player, level, hitResult)) {
            // Prevent this from being called again immediately on the next tick
            // Vanilla also uses a 5 tick delay (-> 250ms)
            // PlayerControllerMP.blockHitDelay is private
            ObfuscationReflectionHelper.setPrivateValue(PlayerControllerMP.class,
                    Minecraft.getMinecraft().playerController, 5, "field_78781_i");

            // Consume the action, but do not send a packet to the server,
            // since we're handling that ourselves.
            return EnumActionResult.FAIL;
        }
        return EnumActionResult.PASS;
    }

    private static boolean onBlockAttackedOnClient(EntityPlayer player, World level, RayTraceResult hitResult) {
        var pos = hitResult.getBlockPos();

        // Compute the hit location within the block's space. hitResult should never be null at this point,
        // since the event itself gets the blockPos from it.
        var localPos = hitResult.hitVec.subtract(
                pos.getX(),
                pos.getY(),
                pos.getZ());

        var blockEntity = level.getTileEntity(pos);

        if (!(blockEntity instanceof IPartHost partHost)) {
            return false;
        }

        var p = partHost.selectPartLocal(localPos);
        if (p.part != null) {
            boolean alternateUseMode = InteractionUtil.isInAlternateUseMode(player);
            boolean activated;
            if (alternateUseMode) {
                activated = p.part.onShiftClicked(player, localPos);
            } else {
                activated = p.part.onClicked(player, localPos);
            }

            if (activated) {
                ServerboundPacket message = new PartLeftClickPacket(hitResult, alternateUseMode);
                PacketDistributor.sendToServer(message);
                // Do not perform the default action (of spawning break particles and breaking the block)
                return true;
            }
        } else if (p.facade != null) {
            if (p.facade.onClicked(player, localPos)) {
                ServerboundPacket message = new PartLeftClickPacket(hitResult, false);
                PacketDistributor.sendToServer(message);
                // Do not perform the default action (of spawning break particles and breaking the block)
                return true;
            }
        }

        return false;
    }
}
