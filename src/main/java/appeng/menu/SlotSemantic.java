package appeng.menu;

/**
 * Used to group {@link net.minecraft.world.inventory.Slot slots} in a menu into specific semantics, which are then
 * positioned on the screen from a {@link appeng.client.gui.style.ScreenStyle}.
 *
 * @param playerSide Indicates whether a slot is considered to be part of the items that a player carries.
 * @see SlotSemantics For a registry of slot semantics.
 */
public final class SlotSemantic {
    private final String id;
    private final boolean playerSide;
    private final int quickMovePriority;

    public SlotSemantic(String id, boolean playerSide, int quickMovePriority) {
        this.id = id;
        this.playerSide = playerSide;
        this.quickMovePriority = quickMovePriority;
    }

    public String id() {
        return id;
    }

    public boolean playerSide() {
        return playerSide;
    }

    public int quickMovePriority() {
        return quickMovePriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotSemantic)) return false;
        SlotSemantic that = (SlotSemantic) o;
        return java.util.Objects.equals(id, that.id)
                && playerSide == that.playerSide
                && quickMovePriority == that.quickMovePriority;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, playerSide, quickMovePriority);
    }

    @Override
    public String toString() {
        return "SlotSemantic[" + "id=" + id + ", " + "playerSide=" + playerSide + ", " + "quickMovePriority=" + quickMovePriority + "]";
    }

}
