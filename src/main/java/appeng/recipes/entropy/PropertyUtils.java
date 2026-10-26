/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.recipes.entropy;

import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

final class PropertyUtils {
    private static final Logger LOG = LogManager.getLogger(PropertyUtils.class);

    private PropertyUtils() {
    }

    static IProperty<?> getRequiredProperty(BlockStateContainer stateDefinition, String name) {
        Objects.requireNonNull(stateDefinition, "stateDefinition must not be null");

        IProperty<?> property = stateDefinition.getProperty(name);
        if (property == null) {
            throw new IllegalArgumentException("Unknown property: " + name + " on " + stateDefinition.getBlock());
        }
        return property;
    }

    static <T extends Comparable<T>> T getRequiredPropertyValue(IProperty<T> property, String name) {
        Objects.requireNonNull(property, "property must be not null");

        var value = property.parseValue(name);
        if (!value.isPresent()) {
            throw new IllegalArgumentException("Invalid value '" + name + "' for property " + property.getName());
        }
        return value.get();
    }

    static void validatePropertyMatchers(BlockStateContainer stateDefinition,
            Map<String, PropertyValueMatcher> properties) {
        for (var entry : properties.entrySet()) {
            var property = stateDefinition.getProperty(entry.getKey());
            if (property == null) {
                throw new IllegalArgumentException("State definition " + stateDefinition
                        + " does not have property '" + entry.getKey() + "'");
            }

            // this will throw if it doesnt have the value
            entry.getValue().validate(property);
        }
    }

    public static boolean doPropertiesMatch(BlockStateContainer stateDefinition,
            IBlockState state, Map<String, PropertyValueMatcher> properties) {
        for (var entry : properties.entrySet()) {
            var property = stateDefinition.getProperty(entry.getKey());
            if (property == null) {
                throw new IllegalArgumentException("State definition " + stateDefinition
                        + " does not have property '" + entry.getKey() + "'");
            }

            if (!entry.getValue().matches(property, state)) {
                return false;
            }
        }
        return true;
    }

    static IBlockState applyProperties(BlockStateContainer stateDefinition, IBlockState state,
            Map<String, String> properties) {
        for (var entry : properties.entrySet()) {
            // Get property
            var property = stateDefinition.getProperty(entry.getKey());
            if (property != null) {
                state = applyProperty(state, property, entry.getValue());
            } else {
                LOG.warn("Cannot apply property {} since {} does not have that property", entry.getKey(),
                        stateDefinition);
            }
        }
        return state;
    }

    static <T extends Comparable<T>> IBlockState applyProperty(IBlockState state, IProperty<T> property,
            String value) {
        var parsedValue = property.parseValue(value);
        return parsedValue.isPresent() ? state.withProperty(property, parsedValue.get()) : state;
    }
}
