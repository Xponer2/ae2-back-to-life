package appeng.integration.modules.itemlists;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.Rect2i;

public interface DropTarget {
    Rect2i area();

    boolean canDrop(GenericStack stack);

    boolean drop(GenericStack stack);
}
