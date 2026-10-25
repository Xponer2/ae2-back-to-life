package appeng.client.gui.me.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.network.clientbound.CraftingJobStatusPacket;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.SearchInventoryEvent;

/**
 * Tracks pending crafting jobs started by this player.
 */
@SideOnly(Side.CLIENT)
public final class PendingCraftingJobs {
    private static final Map<UUID, PendingJob> jobs = new HashMap<>();

    private PendingCraftingJobs() {
    }

    public static boolean hasPendingJob(AEKey what) {
        return jobs.entrySet().stream().anyMatch(s -> s.getValue().what.equals(what));
    }

    public static void clearPendingJobs() {
        jobs.clear();
    }

    public static void jobStatus(UUID id,
            AEKey what,
            long requestedAmount,
            long remainingAmount,
            CraftingJobStatusPacket.Status status) {

        AELog.debug("Crafting job " + id + " for " + requestedAmount
                + "x" + AEKeyRendering.getDisplayName(what).getUnformattedText() + ". State=" + status);

        var existing = jobs.get(id);
        switch (status) {
            case STARTED -> {
                if (existing == null) {
                    jobs.put(id, new PendingJob(id, what, requestedAmount, remainingAmount));
                }
            }
            case CANCELLED -> jobs.remove(id);
            case FINISHED -> {
                jobs.remove(id);
                // Only toast if no terminal is open (i.e. REI/JEI or no screen at all)
                // and a wireless terminal is in the player inv
                var minecraft = Minecraft.getMinecraft();
                if (AEConfig.instance().isNotifyForFinishedCraftingJobs()
                        && !(minecraft.currentScreen instanceof MEStorageScreen<?>)
                        && minecraft.player != null && hasNotificationEnablingItem(minecraft.player)) {
                    minecraft.getToastGui().add(new FinishedJobToast(what, requestedAmount));
                }
            }
        }
    }

    private static boolean hasNotificationEnablingItem(EntityPlayerSP player) {
        for (ItemStack stack : SearchInventoryEvent.getItems(player)) {
            if (!stack.isEmpty()
                    && stack.getItem() instanceof WirelessTerminalItem wirelessTerminal
                    // Should have some power
                    && wirelessTerminal.getAECurrentPower(stack) > 0
                    // Should be linked (we don't know if it's linked to the grid for which we get notifications)
                    && wirelessTerminal.getLinkedPosition(stack) != null) {
                return true;
            }
        }
        return false;
    }

    final static class PendingJob {
        private final UUID jobId;
        private final AEKey what;
        private final long requestedAmount;
        private final long remainingAmount;

        public PendingJob(UUID jobId, AEKey what, long requestedAmount, long remainingAmount) {
            this.jobId = jobId;
            this.what = what;
            this.requestedAmount = requestedAmount;
            this.remainingAmount = remainingAmount;
        }

        public UUID jobId() {
            return jobId;
        }

        public AEKey what() {
            return what;
        }

        public long requestedAmount() {
            return requestedAmount;
        }

        public long remainingAmount() {
            return remainingAmount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PendingJob)) return false;
            PendingJob that = (PendingJob) o;
            return java.util.Objects.equals(jobId, that.jobId)
                    && java.util.Objects.equals(what, that.what)
                    && requestedAmount == that.requestedAmount
                    && remainingAmount == that.remainingAmount;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(jobId, what, requestedAmount, remainingAmount);
        }

        @Override
        public String toString() {
            return "PendingJob[" + "jobId=" + jobId + ", " + "what=" + what + ", " + "requestedAmount=" + requestedAmount + ", " + "remainingAmount=" + remainingAmount + "]";
        }

    }
}
