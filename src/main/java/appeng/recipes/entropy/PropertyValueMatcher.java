package appeng.recipes.entropy;

import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.properties.IProperty;
import net.minecraft.world.level.block.state.StateHolder;

import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;

public interface PropertyValueMatcher {
    Codec<PropertyValueMatcher> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PropertyValueMatcher, T>> decode(DynamicOps<T> ops, T input) {
            // Try single string value first
            var singleValueResult = Codec.STRING.decode(ops, input)
                    .map(pair -> pair.mapFirst(value -> (PropertyValueMatcher) new SingleValue(value)));
            if (singleValueResult.error().isEmpty()) {
                return singleValueResult;
            }

            // Then try list value
            var listValueResult = Codec.STRING.listOf().decode(ops, input)
                    .map(pair -> pair.mapFirst(value -> (PropertyValueMatcher) new MultiValue(value)));
            if (listValueResult.error().isEmpty()) {
                return listValueResult;
            }

            // Then try min/max object
            var rangeValueResult = Range.CODEC.decode(ops, input)
                    .map(pair -> pair.mapFirst(value -> (PropertyValueMatcher) value));
            if (rangeValueResult.error().isEmpty()) {
                return rangeValueResult;
            }

            // If all three fail, combine the errors
            return DataResult.error(
                    () -> "IProperty values need to be strings, list of strings, or objects with min/max properties");
        }

        @Override
        public <T> DataResult<T> encode(PropertyValueMatcher input, DynamicOps<T> ops, T prefix) {
            if (input instanceof SingleValue singleValue) {
                return Codec.STRING.encode(singleValue.value(), ops, prefix);
            } else if (input instanceof MultiValue multiValue) {
                return Codec.STRING.listOf().encode(multiValue.values(), ops, prefix);
            } else if (input instanceof Range range) {
                return Range.CODEC.encode(range, ops, prefix);
            } else {
                throw new IllegalStateException("This cannot happen");
            }
        }
    };

    StreamCodec<RegistryFriendlyByteBuf, PropertyValueMatcher> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PropertyValueMatcher decode(RegistryFriendlyByteBuf buffer) {
            var type = buffer.readByte();
            return switch (type) {
                case 0 -> new SingleValue(buffer.readUtf());
                case 1 -> new MultiValue(buffer.readList(RegistryFriendlyByteBuf::readUtf));
                case 2 -> new Range(buffer.readUtf(), buffer.readUtf());
                default -> throw new IllegalStateException("Invalid property value matcher type: " + type);
            };
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PropertyValueMatcher matcher) {
            matcher.toNetwork(buffer);
        }
    };

    Codec<Map<String, PropertyValueMatcher>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC);

    void toNetwork(RegistryFriendlyByteBuf buffer);

    void validate(IProperty<? extends Comparable<?>> property);

    <T extends Comparable<T>> boolean matches(IProperty<T> property, StateHolder<?, ?> state);

    final static class SingleValue implements PropertyValueMatcher {
        private final String value;

        public SingleValue(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SingleValue)) return false;
            SingleValue that = (SingleValue) o;
            return java.util.Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(value);
        }

        @Override
        public String toString() {
            return "SingleValue[" + "value=" + value + "]";
        }

        @Override
        public void toNetwork(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(0);
            buffer.writeUtf(value);
        }

        @Override
        public void validate(IProperty<? extends Comparable<?>> property) {
            if (property.getValue(value).isEmpty()) {
                throw new IllegalStateException(
                        "IProperty " + property.getName() + " does not have value '" + value + "'");
            }
        }

        @Override
        public <T extends Comparable<T>> boolean matches(IProperty<T> property, StateHolder<?, ?> state) {
            var currentValue = property.getName(state.getValue(property));
            return value.equals(currentValue);
        }
    }

    final static class MultiValue implements PropertyValueMatcher {
        private final List<String> values;

        public MultiValue(List<String> values) {
            this.values = values;
        }

        public List<String> values() {
            return values;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MultiValue)) return false;
            MultiValue that = (MultiValue) o;
            return java.util.Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(values);
        }

        @Override
        public String toString() {
            return "MultiValue[" + "values=" + values + "]";
        }

        @Override
        public void toNetwork(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(1);
            buffer.writeCollection(values, RegistryFriendlyByteBuf::writeUtf);
        }

        @Override
        public void validate(IProperty<? extends Comparable<?>> property) {
            for (String value : values) {
                if (property.getValue(value).isEmpty()) {
                    throw new IllegalStateException(
                            "IProperty " + property.getName() + " does not have value '" + value + "'");
                }
            }
        }

        @Override
        public <T extends Comparable<T>> boolean matches(IProperty<T> property, StateHolder<?, ?> state) {
            var currentValue = property.getName(state.getValue(property));
            for (var value : values) {
                return value.equals(currentValue);
            }
            return true;
        }
    }

    final static class Range implements PropertyValueMatcher {
        private final String min;
        private final String max;

        public Range(String min, String max) {
            this.min = min;
            this.max = max;
        }

        public String min() {
            return min;
        }

        public String max() {
            return max;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Range)) return false;
            Range that = (Range) o;
            return java.util.Objects.equals(min, that.min)
                    && java.util.Objects.equals(max, that.max);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(min, max);
        }

        @Override
        public String toString() {
            return "Range[" + "min=" + min + ", " + "max=" + max + "]";
        }

        static Codec<Range> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codec.STRING.fieldOf("min").forGetter(Range::min),
                Codec.STRING.fieldOf("max").forGetter(Range::max)).apply(builder, Range::new));

        @Override
        public void toNetwork(RegistryFriendlyByteBuf buffer) {
            buffer.writeByte(2);
            buffer.writeUtf(min);
            buffer.writeUtf(max);
        }

        @Override
        public void validate(IProperty<? extends Comparable<?>> property) {
            if (property.getValue(min).isEmpty()) {
                throw new IllegalStateException(
                        "IProperty " + property.getName() + " does not have value '" + min + "'");
            }
            if (property.getValue(max).isEmpty()) {
                throw new IllegalStateException(
                        "IProperty " + property.getName() + " does not have value '" + max + "'");
            }
        }

        @Override
        public <T extends Comparable<T>> boolean matches(IProperty<T> property, StateHolder<?, ?> state) {
            var minValue = property.getValue(min).orElseThrow();
            var maxValue = property.getValue(max).orElseThrow();
            var value = state.getValue(property);
            return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
        }
    }
}
