package appeng.api.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.localization.GuiText;

/**
 * Properties shown in the tooltip of an encoded pattern. Used for both valid and invalid encoded patterns. For invalid
 * patterns, only partial information might be given.
 */
public class PatternDetailsTooltip {
    /**
     * The text to use when the pattern uses Vanilla crafting as its method of producing the item. Usually reserved for
     * patterns used in molecular assemblers.
     */
    public static final ITextComponent OUTPUT_TEXT_CRAFTS = GuiText.Crafts.text();

    /**
     * The text to use when the pattern uses some other form of processing to produce the output.
     */
    public static final ITextComponent OUTPUT_TEXT_PRODUCES = GuiText.Produces.text();

    private ITextComponent outputMethod;

    private final List<Property> additionalProperties = new ArrayList<>();

    private final List<GenericStack> inputs = new ArrayList<>();

    private final List<GenericStack> outputs = new ArrayList<>();

    /**
     * @param outputMethod The method of producing the outputs of this pattern ({@link #OUTPUT_TEXT_CRAFTS} or
     *                     {@link #OUTPUT_TEXT_PRODUCES}). Usually this will depend on the type of pattern and not so
     *                     much the individual NBT data.
     */
    public PatternDetailsTooltip(ITextComponent outputMethod) {
        setOutputMethod(outputMethod);
    }

    /**
     * @param outputMethod The method of producing the outputs of this pattern ({@link #OUTPUT_TEXT_CRAFTS} or
     *                     {@link #OUTPUT_TEXT_PRODUCES}). Usually this will depend on the type of pattern and not so
     *                     much the individual NBT data.
     */
    public void setOutputMethod(ITextComponent outputMethod) {
        this.outputMethod = Objects.requireNonNull(outputMethod, "outputMethod");
    }

    public List<Property> getProperties() {
        return additionalProperties;
    }

    public List<GenericStack> getInputs() {
        return inputs;
    }

    public List<GenericStack> getOutputs() {
        return outputs;
    }

    public void addInput(AEKey what, long amount) {
        inputs.add(new GenericStack(what, amount));
    }

    public void addInput(GenericStack stack) {
        inputs.add(new GenericStack(stack.what(), stack.amount()));
    }

    public void addOutput(AEKey what, long amount) {
        outputs.add(new GenericStack(what, amount));
    }

    public void addOutput(GenericStack stack) {
        outputs.add(new GenericStack(stack.what(), stack.amount()));
    }

    public void addProperty(ITextComponent name, ITextComponent value) {
        this.additionalProperties.add(new Property(name, value));
    }

    public void addProperty(ITextComponent description) {
        this.additionalProperties.add(new Property(description, null));
    }

    public void addInputsAndOutputs(IPatternDetails details) {
        for (var input : details.getInputs()) {
            if (input == null) {
                continue;
            }

            addInput(input.getPossibleInputs()[0].what(),
                    input.getPossibleInputs()[0].amount() * input.getMultiplier());
        }

        for (var output : details.getOutputs()) {
            if (output == null) {
                continue;
            }

            addOutput(output.what(), output.amount());
        }
    }

    /**
     * An additional property to display on the patterns tooltip, such as whether substitutions of items was enabled or
     * not.
     */
    public final static class Property {
        private final ITextComponent name;
        private final ITextComponent value;

        public Property(ITextComponent name, @Nullable ITextComponent value) {
            this.name = name;
            this.value = value;
        }

        public ITextComponent name() {
            return name;
        }

        @Nullable public ITextComponent value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Property)) return false;
            Property that = (Property) o;
            return java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "Property[" + "name=" + name + ", " + "value=" + value + "]";
        }

    }

    public ITextComponent getOutputMethod() {
        return outputMethod;
    }
}
