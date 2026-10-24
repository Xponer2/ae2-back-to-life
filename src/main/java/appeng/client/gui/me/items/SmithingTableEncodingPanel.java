package appeng.client.gui.me.items;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import appeng.api.config.ActionItems;
import appeng.client.Point;
import appeng.client.gui.GuiGraphics;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;

public class SmithingTableEncodingPanel extends EncodingModePanel {
    private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(128, 70, 124, 66);

    private final ActionButton clearBtn;
    private final ToggleButton substitutionsBtn;
    private final Slot resultSlot;

    public SmithingTableEncodingPanel(PatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);

        clearBtn = new ActionButton(ActionItems.S_CLOSE, act -> menu.clear());
        clearBtn.setHalfSize(true);
        clearBtn.setDisableBackground(true);
        widgets.add("smithingTableClearPattern", clearBtn);

        this.substitutionsBtn = createSubstitutionButton(widgets);

        this.resultSlot = new Slot(new InventoryBasic(1), 0, 0, 0);
        menu.addClientSideSlot(resultSlot, SlotSemantics.SMITHING_TABLE_RESULT);
    }

    @Override
    Icon getIcon() {
        return Icon.TAB_SMITHING;
    }

    @Override
    public ITextComponent getTabTooltip() {
        return GuiText.SmithingTablePattern.text();
    }

    private ToggleButton createSubstitutionButton(WidgetContainer widgets) {
        var button = new ToggleButton(
                Icon.S_SUBSTITUTION_ENABLED,
                Icon.S_SUBSTITUTION_DISABLED,
                menu::setSubstitute);
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(ImmutableList.of(
                ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        button.setTooltipOff(ImmutableList.of(
                ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        widgets.add("smithingTableSubstitutions", button);
        return button;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphics guiGraphics, Rect2i bounds, Point mouse) {
        BG.dest(bounds.getX() + 8, bounds.getY() + bounds.getHeight() - 165).blit(guiGraphics);
    }

    @Override
    public void updateBeforeRender() {
        this.substitutionsBtn.setState(this.menu.substitute);

        var recipeInput = new SmithingRecipeInput(
                menu.getSmithingTableTemplateSlot().getItem(),
                menu.getSmithingTableBaseSlot().getItem(),
                menu.getSmithingTableAdditionSlot().getItem());

        var level = menu.getPlayer().world;
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, recipeInput, level)
                .orElse(null);
        if (recipe == null) {
            resultSlot.putStack(ItemStack.EMPTY);
        } else {
            resultSlot.set(recipe.value().assemble(recipeInput));
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        clearBtn.setVisibility(visible);
        substitutionsBtn.setVisibility(visible);

        screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_TEMPLATE, !visible);
        screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_BASE, !visible);
        screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_ADDITION, !visible);
        screen.setSlotsHidden(SlotSemantics.SMITHING_TABLE_RESULT, !visible);
    }
}
