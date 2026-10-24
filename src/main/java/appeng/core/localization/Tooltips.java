package appeng.core.localization;

import appeng.util.Platform;

import appeng.util.TextUtil;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.NotNull;

import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import appeng.api.behaviors.EmptyingAction;
import appeng.api.config.PowerUnit;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.core.AEConfig;

/**
 * Static utilities for constructing tooltips in various places.
 */
public final class Tooltips {

    private static final char SEP;
    static {
        var format = (DecimalFormat) DecimalFormat.getInstance();
        var symbols = format.getDecimalFormatSymbols();
        SEP = symbols.getDecimalSeparator();
    }

    public static final TextFormatting MUTED_COLOR = TextFormatting.DARK_GRAY;

    public static final Style NORMAL_TOOLTIP_TEXT = Style.EMPTY.withColor(TextFormatting.GRAY).withItalic(false);
    public static final Style QUOTE_TEXT = NORMAL_TOOLTIP_TEXT.withItalic(true);
    public static final Style NUMBER_TEXT = Style.EMPTY.withColor(TextColor.fromRgb(0x886eff)).withItalic(false);
    public static final Style UNIT_TEXT = Style.EMPTY.withColor(TextColor.fromRgb(0xffde7d)).withItalic(false);

    public static final Style RED = Style.EMPTY.withColor(TextFormatting.RED);
    public static final Style GREEN = Style.EMPTY.withColor(TextFormatting.GREEN);

    private Tooltips() {
    }

    public static List<ITextComponent> slotTooltip(ITextComponent text) {
        return ImmutableList.of(TextUtil.withStyle(text, MUTED_COLOR));
    }

    public static List<ITextComponent> inputSlot(Side... sides) {
        var sidesText = Arrays.stream(sides).map(Tooltips::side).collect(Collectors.toList());

        return ImmutableList.of(
                TextUtil.withStyle(ButtonToolTips.CanInsertFrom.text(conjunction(sidesText)), MUTED_COLOR));
    }

    public static List<ITextComponent> outputSlot(Side... sides) {
        var sidesText = Arrays.stream(sides).map(Tooltips::side).collect(Collectors.toList());

        return ImmutableList.of(
                TextUtil.withStyle(ButtonToolTips.CanExtractFrom.text(conjunction(sidesText)), MUTED_COLOR));
    }

    public static ITextComponent side(Side side) {
        return switch (side) {
            case BOTTOM -> ButtonToolTips.SideBottom.text();
            case TOP -> ButtonToolTips.SideTop.text();
            case LEFT -> ButtonToolTips.SideLeft.text();
            case RIGHT -> ButtonToolTips.SideRight.text();
            case FRONT -> ButtonToolTips.SideFront.text();
            case BACK -> ButtonToolTips.SideBack.text();
            case ANY -> ButtonToolTips.SideAny.text();
        };
    }

    public static ITextComponent conjunction(List<ITextComponent> components) {
        return list(components, GuiText.And);
    }

    public static ITextComponent disjunction(List<ITextComponent> components) {
        return list(components, GuiText.Or);
    }

    @NotNull
    private static ITextComponent list(List<ITextComponent> components, GuiText lastJoiner) {
        if (components.isEmpty()) {
            return new TextComponentString("");
        }

        if (components.size() == 2) {
            return components.get(0)
                    .createCopy()
                    .appendSibling(" ")
                    .append(lastJoiner.text())
                    .append(" ")
                    .append(components.get(1));
        }

        var current = components.get(0);
        for (int i = 1; i < components.size(); i++) {
            if (i + 1 < components.size()) {
                current = current.createCopy().appendSibling(", ").appendSibling(components.get(i));
            } else {
                current = current.createCopy().appendSibling(", ").appendSibling(lastJoiner.text()).appendSibling(" ").appendSibling(components.get(i));
            }
        }

        return current;
    }

    public static List<ITextComponent> getEmptyingTooltip(ButtonToolTips baseAction, ItemStack carried,
            EmptyingAction emptyingAction) {
        return ImmutableList.of(
                baseAction.text(
                        getMouseButtonText(InputConstants.MOUSE_BUTTON_LEFT),
                        TextUtil.withStyle(Platform.getHoverName(carried).createCopy(), NORMAL_TOOLTIP_TEXT)).withStyle(MUTED_COLOR),
                baseAction.text(
                        getMouseButtonText(InputConstants.MOUSE_BUTTON_RIGHT),
TextUtil.withStyle(                        TextUtil.withStyle(emptyingAction.description().createCopy(), NORMAL_TOOLTIP_TEXT)), MUTED_COLOR));
    }

    public static ITextComponent getSetAmountTooltip() {
        return ButtonToolTips.ModifyAmountAction.text(Tooltips.getMouseButtonText(InputConstants.MOUSE_BUTTON_MIDDLE))
                .withStyle(MUTED_COLOR);
    }

    public static ITextComponent getMouseButtonText(int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> ButtonToolTips.LeftClick.text();
            case InputConstants.MOUSE_BUTTON_RIGHT -> ButtonToolTips.RightClick.text();
            case InputConstants.MOUSE_BUTTON_MIDDLE -> ButtonToolTips.MiddleClick.text();
            default -> ButtonToolTips.MouseButton.text(button);
        };
    }

    /**
     * Should the amount be shown in the tooltip of the given stack.
     */
    public static boolean shouldShowAmountTooltip(AEKey what, long amount) {
        // TODO: Now that we can show fractional numbers, this approach of detecting whether the formatted amount has
        // been abbreviated or rounded no longer works
        var bigNumber = AEConfig.instance().isUseLargeFonts() ? 999L : 9999L;
        return amount > bigNumber * what.getAmountPerUnit()
                // Unit symbols are never shown in slots and must be shown in the tooltip instead
                || what.getUnitSymbol() != null
                // Damaged items always get their amount shown in the tooltip because
                // the amount is sometimes hard to read superimposed on the damage bar
                || what instanceof AEItemKey itemKey && itemKey.getReadOnlyStack().isBarVisible();
    }

    public static ITextComponent getAmountTooltip(ButtonToolTips baseText, GenericStack stack) {
        return getAmountTooltip(baseText, stack.what(), stack.amount());
    }

    public static ITextComponent getAmountTooltip(ButtonToolTips baseText, AEKey what, long amount) {
        var amountText = what.formatAmount(amount, AmountFormat.FULL);
        return TextUtil.withStyle(baseText.text(amountText), MUTED_COLOR);
    }

    public final static class Amount {
        private final String digit;
        private final String unit;

        public Amount(String digit, String unit) {
            this.digit = digit;
            this.unit = unit;
        }

        public String digit() {
            return digit;
        }

        public String unit() {
            return unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Amount)) return false;
            Amount that = (Amount) o;
            return java.util.Objects.equals(digit, that.digit)
                    && java.util.Objects.equals(unit, that.unit);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(digit, unit);
        }

        @Override
        public String toString() {
            return digit + unit;
        }
    }

    public final static class MaxedAmount {
        private final String digit;
        private final String maxDigit;
        private final String unit;

        public MaxedAmount(String digit, String maxDigit, String unit) {
            this.digit = digit;
            this.maxDigit = maxDigit;
            this.unit = unit;
        }

        public String digit() {
            return digit;
        }

        public String maxDigit() {
            return maxDigit;
        }

        public String unit() {
            return unit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MaxedAmount)) return false;
            MaxedAmount that = (MaxedAmount) o;
            return java.util.Objects.equals(digit, that.digit)
                    && java.util.Objects.equals(maxDigit, that.maxDigit)
                    && java.util.Objects.equals(unit, that.unit);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(digit, maxDigit, unit);
        }

        @Override
        public String toString() {
            return "MaxedAmount[" + "digit=" + digit + ", " + "maxDigit=" + maxDigit + ", " + "unit=" + unit + "]";
        }

    }

    public static ITextComponent ofDuration(long number, TimeUnit unit) {
        long seconds = TimeUnit.SECONDS.convert(number, unit);
        // We don't have 1s, but not 0 either
        if (seconds == 0) {
            if (number > 0) {
                return TextUtil.withStyle(new TextComponentString("~"), NUMBER_TEXT)
                        .appendSibling(ButtonToolTips.DurationFormatSeconds.text(0));
            } else {
                return TextUtil.withStyle(ButtonToolTips.DurationFormatSeconds.text(0), NUMBER_TEXT);
            }
        }

        ITextComponent durationStr = new TextComponentString("");
        var hours = TimeUnit.HOURS.convert(seconds, TimeUnit.SECONDS);
        if (hours > 0) {
            durationStr.appendSibling(Long.toString(hours)).appendSibling("h");
            seconds -= hours * 60 * 60;
        }
        var minutes = TimeUnit.MINUTES.convert(seconds, TimeUnit.SECONDS);
        if (minutes > 0) {
            durationStr.appendSibling(Long.toString(minutes)).appendSibling("m");
            seconds -= minutes * 60;
        }
        if (seconds > 0) {
            durationStr.appendSibling(Long.toString(seconds)).appendSibling("s");
        }

        return TextUtil.withStyle(durationStr, NUMBER_TEXT);
    }

    private static final String[] units = new String[] { "k", "M", "G", "T", "P", "E" };
    private static final long[] DECIMAL_NUMS = new long[] { 1000L, 1000_000L, 1000_000_000L, 1000_000_000_000L,
            1000_000_000_000_000L,
            1000_000_000_000_000_000L };
    private static final long[] BYTE_NUMS = new long[] { 1024L, 1024 * 1024L, 1024 * 1024 * 1024L,
            1024 * 1024 * 1024 * 1024L,
            1024L * 1024 * 1024 * 1024 * 1024,
            1024L * 1024 * 1024 * 1024 * 1024 * 1024 };

    public static ITextComponent ofAmount(GenericStack stack) {
        return TextUtil.withStyle(new TextComponentString(stack.what().formatAmount(stack.amount(), AmountFormat.FULL)), NUMBER_TEXT);
    }

    public static String getAmount(double amount, long num) {
        double fract = amount / num;
        String returned;
        if (fract < 10) {
            returned = String.format("%.3f", fract);
        } else if (fract < 100) {
            returned = String.format("%.2f", fract);
        } else {
            returned = String.format("%.1f", fract);
        }
        while (returned.endsWith("0")) {
            returned = returned.substring(0, returned.length() - 1);
        }
        if (returned.endsWith(String.valueOf(SEP))) {
            returned = returned.substring(0, returned.length() - 1);
        }
        return returned;

    }

    public static Amount getAmount(double amount) {
        if (amount < 10000) {
            return new Amount(getAmount(amount, 1), "");
        } else {
            int i = 0;
            while (amount / DECIMAL_NUMS[i] >= 1000) {
                i++;
            }
            return new Amount(getAmount(amount, DECIMAL_NUMS[i]), units[i]);
        }
    }

    public static MaxedAmount getMaxedAmount(double amount, double max) {
        if (max < 10000) {
            return new MaxedAmount(getAmount(amount, 1), getAmount(max, 1), "");
        } else {
            int i = 0;
            while (max / DECIMAL_NUMS[i] >= 1000) {
                i++;
            }
            return new MaxedAmount(getAmount(amount, DECIMAL_NUMS[i]), getAmount(max, DECIMAL_NUMS[i]), units[i]);
        }
    }

    public static Amount getByteAmount(long amount) {
        if (amount < BYTE_NUMS[0]) {
            return new Amount(String.valueOf(amount), "");
        } else {
            int i = 0;
            while (i < BYTE_NUMS.length && amount / BYTE_NUMS[i] >= 1000) {
                i++;
            }
            return new Amount(getAmount(amount, BYTE_NUMS[i]), units[i]);
        }
    }

    public static Amount getAmount(long amount) {
        if (amount < 10000) {
            return new Amount(String.valueOf(amount), "");
        } else {
            int i = 0;
            while (i < DECIMAL_NUMS.length && amount / DECIMAL_NUMS[i] >= 1000) {
                i++;
            }
            return new Amount(getAmount(amount, DECIMAL_NUMS[i]), units[i]);
        }
    }

    public static MaxedAmount getMaxedAmount(long amount, long max) {
        if (max < 10000) {
            return new MaxedAmount(String.valueOf(amount), String.valueOf(max), "");
        } else {
            int i = 0;
            while (max / DECIMAL_NUMS[i] >= 1000) {
                i++;
            }
            return new MaxedAmount(getAmount(amount, DECIMAL_NUMS[i]), getAmount(max, DECIMAL_NUMS[i]), units[i]);
        }
    }

    public static ITextComponent of(ITextComponent component) {
        return TextUtil.withStyle(component.createCopy(), NORMAL_TOOLTIP_TEXT);
    }

    public static ITextComponent of(ButtonToolTips buttonToolTips, Object... args) {
        return of(buttonToolTips, NORMAL_TOOLTIP_TEXT, args);
    }

    public static ITextComponent of(ButtonToolTips buttonToolTips, Style style, Object... args) {
        return TextUtil.withStyle(buttonToolTips.text(args).createCopy(), style);
    }

    public static ITextComponent of(GuiText guiText, Object... args) {
        return of(guiText, NORMAL_TOOLTIP_TEXT, args);
    }

    public static ITextComponent of(GuiText guiText, Style style, Object... args) {

        if (args.length > 0 && args[0] instanceof Integer) {
            return TextUtil.withStyle(guiText.text(Arrays.stream(args).map((o) -> ofUnformattedNumber((Integer) o)).toArray()).createCopy(), style);
        } else if (args.length > 0 && args[0] instanceof Long) {
            return TextUtil.withStyle(guiText.text(Arrays.stream(args).map((o) -> ofUnformattedNumber((Long) o)).toArray()).createCopy(), style);
        }
        return TextUtil.withStyle(guiText.text(args).createCopy(), style);

    }

    public static ITextComponent of(String s) {
        return TextUtil.withStyle(new TextComponentString(s), NORMAL_TOOLTIP_TEXT);
    }

    public static ITextComponent of(PowerUnit pU) {
        return TextUtil.withStyle(pU.textComponent().createCopy(), UNIT_TEXT);
    }

    public static ITextComponent ofPercent(double percent, boolean oneIsGreen) {
        return TextUtil.withStyle(new TextComponentString(MessageFormat.format("{0,number,#.##%}", percent)), colorFromRatio(percent, oneIsGreen));
    }

    public static Style colorFromRatio(double ratio, boolean oneIsGreen) {
        double p = ratio;

        if (!oneIsGreen) {
            p = 1 - p;
        }

        int r = (int) (255d * (Math.max(0, Math.min(2 - 2 * p, 1))));
        int g = (int) (255d * (Math.max(0, Math.min(2 * p, 1))));
        int rgb = 0xFF000000 + (r << 16) + (g << 8);

        return Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(rgb));
    }

    public static ITextComponent ofPercent(double percent) {
        return ofPercent(percent, true);

    }

    public static ITextComponent ofUnformattedNumber(long number) {
        return TextUtil.withStyle(new TextComponentString(String.valueOf(number)), NUMBER_TEXT);
    }

    public static ITextComponent ofUnformattedNumberWithRatioColor(long number, double ratio, boolean oneIsGreen) {
        return TextUtil.withStyle(new TextComponentString(String.valueOf(number)), colorFromRatio(ratio, oneIsGreen));
    }

    public static ITextComponent ofBytes(long number) {
        Amount amount = getByteAmount(number);
        return ofNumber(amount);
    }

    public static ITextComponent ofNumber(long number) {
        Amount amount = getAmount(number);
        return ofNumber(amount);
    }

    public static ITextComponent ofNumber(double number) {
        Amount amount = getAmount(number);
        return ofNumber(amount);
    }

    private static ITextComponent ofNumber(Amount number) {
        return TextUtil.withStyle(new TextComponentString(number.digit() + number.unit()), NUMBER_TEXT);
    }

    public static ITextComponent ofNumber(long number, long max) {
        MaxedAmount amount = getMaxedAmount(number, max);
        return ofNumber(amount);
    }

    public static ITextComponent ofNumber(double number, double max) {
        MaxedAmount amount = getMaxedAmount(number, max);
        return ofNumber(amount);
    }

    private static ITextComponent ofNumber(MaxedAmount number) {
        boolean numberUnit = !number.digit().equals("0");
        return TextUtil.withStyle(new TextComponentString(number.digit() + (numberUnit ? number.unit() : "")), NUMBER_TEXT)
                .append(TextUtil.withStyle(new TextComponentString("/"), NORMAL_TOOLTIP_TEXT))
                .append(number.maxDigit() + number.unit()).withStyle(NUMBER_TEXT);
    }

    public static ITextComponent of(ITextComponent... components) {
        ITextComponent s = new TextComponentString("");
        for (var c : components) {
            s = s.appendSibling(c);
        }
        return s;
    }

    public static ITextComponent energyStorageComponent(double energy, double max) {
        return Tooltips.of(
                Tooltips.of(GuiText.StoredEnergy),
                Tooltips.of(": "),
                Tooltips.ofNumber(energy, max),
                Tooltips.of(" "),
                Tooltips.of(PowerUnit.AE),
                Tooltips.of(" ("),
                Tooltips.ofPercent(energy / max),
                Tooltips.of(")"));
    }

    public static ITextComponent bytesUsed(long bytes, long max) {
        return of(
                GuiText.BytesUsed,
                Tooltips.of(
                        ofUnformattedNumberWithRatioColor(bytes, (double) bytes / max, false),
                        of(" "),
                        of(GuiText.Of),
                        of(" "),
                        ofUnformattedNumber(max)));
    }

    public static ITextComponent typesUsed(long types, long max) {
        return Tooltips.of(
                ofUnformattedNumberWithRatioColor(types, (double) types / max, false),
                of(" "),
                of(GuiText.Of),
                of(" "),
                ofUnformattedNumber(max),
                of(" "),
                of(GuiText.Types));
    }

}
