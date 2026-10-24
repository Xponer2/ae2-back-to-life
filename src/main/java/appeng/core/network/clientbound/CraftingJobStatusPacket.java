
package appeng.core.network.clientbound;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.PendingCraftingJobs;
import appeng.client.gui.me.common.PinnedKeys;
import appeng.core.AEConfig;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

/**
 * Confirms to the player that a crafting job has started.
 */
public final class CraftingJobStatusPacket implements ClientboundPacket {
    private final UUID jobId;
    private final AEKey what;
    private final long requestedAmount;
    private final long remainingAmount;
    private final Status status;

    public CraftingJobStatusPacket(UUID jobId, AEKey what, long requestedAmount, long remainingAmount, Status status) {
        this.jobId = jobId;
        this.what = what;
        this.requestedAmount = requestedAmount;
        this.remainingAmount = remainingAmount;
        this.status = status;
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

    public Status status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftingJobStatusPacket)) return false;
        CraftingJobStatusPacket that = (CraftingJobStatusPacket) o;
        return java.util.Objects.equals(jobId, that.jobId)
                && java.util.Objects.equals(what, that.what)
                && requestedAmount == that.requestedAmount
                && remainingAmount == that.remainingAmount
                && java.util.Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(jobId, what, requestedAmount, remainingAmount, status);
    }

    @Override
    public String toString() {
        return "CraftingJobStatusPacket[" + "jobId=" + jobId + ", " + "what=" + what + ", " + "requestedAmount=" + requestedAmount + ", " + "remainingAmount=" + remainingAmount + ", " + "status=" + status + "]";
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingJobStatusPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    CraftingJobStatusPacket::write,
                    CraftingJobStatusPacket::decode);

    public static final Type<CraftingJobStatusPacket> TYPE = CustomAppEngPayload.createType("crafting_job_status");

    @Override
    public Type<CraftingJobStatusPacket> type() {
        return TYPE;
    }

    public static CraftingJobStatusPacket decode(RegistryFriendlyByteBuf stream) {
        var jobId = stream.readUUID();
        var status = stream.readEnum(Status.class);
        var what = AEKey.readKey(stream);
        var requestedAmount = stream.readLong();
        var remainingAmount = stream.readLong();
        return new CraftingJobStatusPacket(jobId, what, requestedAmount, remainingAmount, status);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeUUID(jobId);
        data.writeEnum(status);
        AEKey.writeKey(data, what);
        data.writeLong(requestedAmount);
        data.writeLong(remainingAmount);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleOnClient(EntityPlayer player) {
        if (status == Status.STARTED) {
            if (AEConfig.instance().isPinAutoCraftedItems()) {
                PinnedKeys.pinKey(what, PinnedKeys.PinReason.CRAFTING);
            }
        }

        PendingCraftingJobs.jobStatus(jobId, what, requestedAmount, remainingAmount, status);
    }

    public enum Status {
        STARTED,
        CANCELLED,
        FINISHED
    }
}
