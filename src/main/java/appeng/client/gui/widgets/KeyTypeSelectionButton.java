package appeng.client.gui.widgets;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import appeng.api.stacks.AEKeyType;
import appeng.api.storage.ISubMenuHost;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.KeyTypeSelectionScreen;
import appeng.menu.AEBaseMenu;
import appeng.menu.interfaces.KeyTypeSelectionMenu;

public class KeyTypeSelectionButton extends IconButton {
    public static <C extends AEBaseMenu & KeyTypeSelectionMenu, P extends AEBaseScreen<C>> KeyTypeSelectionButton create(
            P parentScreen,
            ISubMenuHost subMenuHost,
            ITextComponent title) {
        return new KeyTypeSelectionButton(
                () -> {
                    if (GuiScreen.hasShiftDown()) {
                        handleShiftClick(parentScreen.getMenu());
                    } else {
                        parentScreen.switchToScreen(new KeyTypeSelectionScreen<>(parentScreen, subMenuHost, title));
                    }
                },
                title,
                () -> {
                    return new TextComponentString(
                            parentScreen.getMenu().getClientKeyTypeSelection().enabledSet().stream()
                                    .map(x -> x.getDescription().getString())
                                    .collect(Collectors.joining(", ")));
                });
    }

    private static <C extends AEBaseMenu & KeyTypeSelectionMenu> void handleShiftClick(C menu) {
        // Compute new selection
        Set<AEKeyType> newSelection = getNextSelection(menu.getClientKeyTypeSelection());

        // First enable new keys
        for (var keyType : newSelection) {
            menu.selectKeyType(keyType, true);
        }
        // Only then disable old keys, to avoid disabling all keys
        for (var keyType : menu.getClientKeyTypeSelection().enabledSet()) {
            if (!newSelection.contains(keyType)) {
                menu.selectKeyType(keyType, false);
            }
        }
    }

    private static Set<AEKeyType> getNextSelection(KeyTypeSelectionMenu.SyncedKeyTypes keyTypes) {
        int totalCount = keyTypes.keyTypes().size();
        int enabledCount = keyTypes.enabledSet().size();

        if (totalCount == enabledCount) {
            // From full selection to first key only
            return ImmutableSet.of(keyTypes.keyTypes().keySet().stream().findFirst().orElseThrow());
        } else if (enabledCount > 1) {
            // From mixed to full selection
            return ImmutableSet.copyOf(keyTypes.keyTypes().keySet());
        } else {
            // Switch to next key
            AEKeyType currentKey = keyTypes.enabledSet().get(0);
            boolean foundCurrent = false;

            for (var keyType : keyTypes.keyTypes().keySet()) {
                if (foundCurrent) {
                    return ImmutableSet.of(keyType);
                }

                if (keyType == currentKey) {
                    foundCurrent = true;
                }
            }

            // If it was the last, go back to full selection
            return ImmutableSet.copyOf(keyTypes.keyTypes().keySet());
        }
    }

    private final ITextComponent title;
    private final Supplier<ITextComponent> descriptionSupplier;

    private KeyTypeSelectionButton(Runnable onPress, ITextComponent title, Supplier<ITextComponent> descriptionSupplier) {
        super(btn -> onPress.run());
        this.title = title;
        this.descriptionSupplier = descriptionSupplier;
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        return ImmutableList.of(
                title,
                descriptionSupplier.get());
    }

    @Override
    protected Icon getIcon() {
        return Icon.TYPE_FILTER_ALL;
    }
}
