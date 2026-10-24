package appeng.api.storage;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;

public final class LinkStatus implements ILinkStatus {
    private final boolean connected;
    private final ITextComponent statusDescription;

    public LinkStatus(boolean connected, @Nullable ITextComponent statusDescription) {
        this.connected = connected;
        this.statusDescription = statusDescription;
    }

    public boolean connected() {
        return connected;
    }

    @Nullable public ITextComponent statusDescription() {
        return statusDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinkStatus)) return false;
        LinkStatus that = (LinkStatus) o;
        return connected == that.connected
                && java.util.Objects.equals(statusDescription, that.statusDescription);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(connected, statusDescription);
    }

    @Override
    public String toString() {
        return "LinkStatus[" + "connected=" + connected + ", " + "statusDescription=" + statusDescription + "]";
    }

    static final LinkStatus CONNECTED = new LinkStatus(true, null);
}
