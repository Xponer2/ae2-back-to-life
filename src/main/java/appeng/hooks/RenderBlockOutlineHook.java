package appeng.hooks;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.implementations.items.IFacadeItem;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.core.AEConfig;
import appeng.core.definitions.AEParts;
import appeng.items.parts.FacadeItem;
import appeng.parts.BusCollisionHelper;
import appeng.parts.PartPlacement;

@SideOnly(Side.CLIENT)
public class RenderBlockOutlineHook {
    private RenderBlockOutlineHook() {
    }

    public static void install() {
        MinecraftForge.EVENT_BUS.register(RenderBlockOutlineHook.class);
    }

    @SubscribeEvent
    public static void handleEvent(DrawBlockHighlightEvent evt) {
        var level = Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }

        var blockHitResult = evt.getTarget();
        if (blockHitResult == null || blockHitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        var player = evt.getPlayer();
        float partialTicks = evt.getPartialTicks();
        // The camera position (1.21 camera.getPosition())
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        if (replaceBlockOutline(player, camX, camY, camZ, blockHitResult)) {
            evt.setCanceled(true);
        }
    }

    /*
     * Changes block outline rendering such that it renders only for individual parts, not for the entire part host.
     */
    private static boolean replaceBlockOutline(EntityPlayer player, double camX, double camY, double camZ,
            RayTraceResult hitResult) {
        var level = player.world;

        if (AEConfig.instance().isPlacementPreviewEnabled()) {
            var itemInHand = player.getHeldItem(EnumHand.MAIN_HAND);
            // Render without depth test to also have a preview for parts inside blocks.
            showPartPlacementPreview(player, camX, camY, camZ, hitResult, itemInHand, true);
            showPartPlacementPreview(player, camX, camY, camZ, hitResult, itemInHand, false);
        }

        // Hit test against all attached parts to highlight the part that is relevant
        var pos = hitResult.getBlockPos();
        if (level.getTileEntity(pos) instanceof IPartHost partHost) {
            // Rendering a preview of what is currently in hand has priority
            // If the item in hand is a facade and a block is hit, attempt facade placement
            if (AEConfig.instance().isPlacementPreviewEnabled()) {
                var itemInHand = player.getHeldItem(EnumHand.MAIN_HAND);
                // Use same rendering inside blocks as part preview.
                showFacadePlacementPreview(camX, camY, camZ, hitResult, partHost, itemInHand, true);
                showFacadePlacementPreview(camX, camY, camZ, hitResult, partHost, itemInHand, false);
            }

            var selectedPart = partHost.selectPartWorld(hitResult.hitVec);
            if (selectedPart.facade != null) {
                renderFacade(camX, camY, camZ, pos, selectedPart.facade, selectedPart.side, false, false);
                return true;
            }
            if (selectedPart.part != null) {
                renderPart(camX, camY, camZ, pos, selectedPart.part, selectedPart.side, false, false);
                return true;
            }
        }

        return false;
    }

    private static boolean showFacadePlacementPreview(double camX, double camY, double camZ,
            RayTraceResult blockHitResult,
            IPartHost partHost,
            ItemStack itemInHand,
            boolean insideBlock) {
        var pos = blockHitResult.getBlockPos();
        if (itemInHand.getItem() instanceof IFacadeItem facadeItem) {
            var side = blockHitResult.sideHit;
            var facade = facadeItem.createPartFromItemStack(itemInHand, side);
            if (facade != null && FacadeItem.canPlaceFacade(partHost, facade)) {
                // Maybe a bit hacky, but if there's no part on the side to support the facade
                // We would render a cable anchor implicitly
                if (partHost.getPart(side) == null) {
                    var cableAnchor = AEParts.CABLE_ANCHOR.get().createPart();
                    renderPart(camX, camY, camZ, pos, cableAnchor, side, true, insideBlock);
                }

                renderFacade(camX, camY, camZ, pos, facade, side, true, insideBlock);
                return true;
            }
        }
        return false;
    }

    private static void showPartPlacementPreview(
            EntityPlayer player,
            double camX, double camY, double camZ,
            RayTraceResult blockHitResult,
            ItemStack itemInHand,
            boolean insideBlock) {
        if (itemInHand.getItem() instanceof IPartItem<?> partItem) {
            var placement = PartPlacement.getPartPlacement(player,
                    player.world,
                    itemInHand,
                    blockHitResult.getBlockPos(),
                    blockHitResult.sideHit,
                    blockHitResult.hitVec);

            if (placement != null) {
                var part = partItem.createPart();
                renderPart(camX, camY, camZ, placement.pos(), part, placement.side(), true, insideBlock);
            }
        }
    }

    private static void renderPart(double camX, double camY, double camZ,
            BlockPos pos,
            IPart part,
            EnumFacing side,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AxisAlignedBB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        part.getBoxes(helper);
        renderBoxes(camX, camY, camZ, pos, boxes, preview, insideBlock);
    }

    private static void renderFacade(double camX, double camY, double camZ,
            BlockPos pos,
            IFacadePart facade,
            EnumFacing side,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AxisAlignedBB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        facade.getBoxes(helper, false);
        renderBoxes(camX, camY, camZ, pos, boxes, preview, insideBlock);
    }

    /**
     * Draws the outlines like the vanilla selection box. Inside blocks the depth test is inverted (1.21
     * LINES_BEHIND_BLOCK), so the part of the outline that is hidden by blocks is drawn faintly.
     */
    private static void renderBoxes(double camX, double camY, double camZ,
            BlockPos pos,
            List<AxisAlignedBB> boxes,
            boolean preview,
            boolean insideBlock) {
        float alpha = insideBlock ? 0.2f : preview ? 0.6f : 0.4f;
        float color = preview ? 1 : 0;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (insideBlock) {
            GlStateManager.depthFunc(GL11.GL_GREATER);
        }

        for (var box : boxes) {
            var worldBox = box.offset(pos).grow(0.002).offset(-camX, -camY, -camZ);
            RenderGlobal.drawSelectionBoundingBox(worldBox, color, color, color, alpha);
        }

        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
