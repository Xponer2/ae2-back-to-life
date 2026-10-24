/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.core.config;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Minimal backport of NeoForge's ModConfigSpec on top of Forge 1.12.2 {@link Configuration}. Keys, categories
 * (push/pop) and comments are the same as on 1.21.
 */
public final class ModConfigSpec {
    private final List<ConfigValue<?>> values;
    @Nullable
    private Configuration configuration;

    private ModConfigSpec(List<ConfigValue<?>> values) {
        this.values = values;
    }

    public void load(File file) {
        configuration = new Configuration(file);
        configuration.load();
        for (var value : values) {
            value.bind(configuration);
        }
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public void save() {
        if (configuration != null) {
            for (var value : values) {
                value.writeBack();
            }
            configuration.save();
        }
    }

    @Nullable
    public Configuration getConfiguration() {
        return configuration;
    }

    public static final class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final Deque<String> path = new ArrayDeque<>();
        private String pendingComment = "";

        public Builder push(String category) {
            path.addLast(category);
            if (!pendingComment.isEmpty()) {
                pendingCategoryComments.add(new String[] { category(), pendingComment });
                pendingComment = "";
            }
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        public Builder comment(String... comment) {
            this.pendingComment = String.join("\n", comment);
            return this;
        }

        private final List<String[]> pendingCategoryComments = new ArrayList<>();

        private String category() {
            return path.isEmpty() ? "general" : String.join(".", path);
        }

        private String takeComment() {
            var c = pendingComment;
            pendingComment = "";
            return c;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            var v = new BooleanValue(category(), name, defaultValue, takeComment());
            values.add(v);
            return v;
        }

        public IntValue defineInRange(String name, int defaultValue, int min, int max) {
            var v = new IntValue(category(), name, defaultValue, min, max, takeComment());
            values.add(v);
            return v;
        }

        public DoubleValue defineInRange(String name, double defaultValue, double min, double max) {
            var v = new DoubleValue(category(), name, defaultValue, min, max, takeComment());
            values.add(v);
            return v;
        }

        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T defaultValue) {
            var v = new EnumValue<>(category(), name, defaultValue, takeComment());
            values.add(v);
            return v;
        }

        public StringValue define(String name, String defaultValue) {
            var v = new StringValue(category(), name, defaultValue, takeComment());
            values.add(v);
            return v;
        }

        public ModConfigSpec build() {
            var spec = new ModConfigSpec(new ArrayList<>(values));
            spec.categoryComments.addAll(pendingCategoryComments);
            return spec;
        }
    }

    private final List<String[]> categoryComments = new ArrayList<>();

    public abstract static class ConfigValue<T> implements Supplier<T> {
        protected final String category;
        protected final String name;
        protected final T defaultValue;
        protected final String comment;
        protected T value;
        @Nullable
        protected Property property;

        ConfigValue(String category, String name, T defaultValue, String comment) {
            this.category = category;
            this.name = name;
            this.defaultValue = defaultValue;
            this.comment = comment;
            this.value = defaultValue;
        }

        abstract void bind(Configuration configuration);

        abstract void writeBack();

        @Override
        public T get() {
            return value;
        }

        public T getDefault() {
            return defaultValue;
        }

        public void set(T value) {
            this.value = value;
        }

        public void save() {
            writeBack();
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String category, String name, boolean defaultValue, String comment) {
            super(category, name, defaultValue, comment);
        }

        @Override
        void bind(Configuration configuration) {
            property = configuration.get(category, name, defaultValue, comment);
            value = property.getBoolean();
        }

        @Override
        void writeBack() {
            if (property != null) {
                property.set(value);
            }
        }

        public boolean getAsBoolean() {
            return value;
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(String category, String name, int defaultValue, int min, int max, String comment) {
            super(category, name, defaultValue, comment);
            this.min = min;
            this.max = max;
        }

        @Override
        void bind(Configuration configuration) {
            property = configuration.get(category, name, defaultValue, comment, min, max);
            value = Math.max(min, Math.min(max, property.getInt()));
        }

        @Override
        void writeBack() {
            if (property != null) {
                property.set(value);
            }
        }

        public int getAsInt() {
            return value;
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String category, String name, double defaultValue, double min, double max, String comment) {
            super(category, name, defaultValue, comment);
            this.min = min;
            this.max = max;
        }

        @Override
        void bind(Configuration configuration) {
            property = configuration.get(category, name, defaultValue, comment, min, max);
            value = Math.max(min, Math.min(max, property.getDouble()));
        }

        @Override
        void writeBack() {
            if (property != null) {
                property.set(value);
            }
        }

        public double getAsDouble() {
            return value;
        }
    }

    public static final class StringValue extends ConfigValue<String> {
        StringValue(String category, String name, String defaultValue, String comment) {
            super(category, name, defaultValue, comment);
        }

        @Override
        void bind(Configuration configuration) {
            property = configuration.get(category, name, defaultValue, comment);
            value = property.getString();
        }

        @Override
        void writeBack() {
            if (property != null) {
                property.set(value);
            }
        }
    }

    public static final class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(String category, String name, T defaultValue, String comment) {
            super(category, name, defaultValue, comment);
        }

        @Override
        void bind(Configuration configuration) {
            var constants = defaultValue.getDeclaringClass().getEnumConstants();
            var names = Arrays.stream(constants).map(Enum::name).toArray(String[]::new);
            var fullComment = (comment.isEmpty() ? "" : comment + "\n") + "Allowed values: " + String.join(", ", names);
            property = configuration.get(category, name, defaultValue.name(), fullComment, names);
            value = defaultValue;
            for (var c : constants) {
                if (c.name().equalsIgnoreCase(property.getString())) {
                    value = c;
                }
            }
        }

        @Override
        void writeBack() {
            if (property != null) {
                property.set(value.name());
            }
        }
    }
}
