/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 * Copyright (c) 2026 the AE2: Back to Life contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.capabilities;

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Backport of NeoForge's ItemCapability on top of Forge 1.12.2 {@link Capability capabilities}.
 */
public final class ItemCapability<T, C> {
    private final ResourceLocation name;
    private final Class<T> typeClass;
    private final Supplier<Capability<T>> forgeCapability;

    private ItemCapability(ResourceLocation name, Class<T> typeClass, Supplier<Capability<T>> forgeCapability) {
        this.name = name;
        this.typeClass = typeClass;
        this.forgeCapability = forgeCapability;
    }

    public static <T> ItemCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
        return new ItemCapability<>(name, typeClass, () -> AECapabilityRegistration.get(typeClass));
    }

    public static <T, C> ItemCapability<T, C> forge(ResourceLocation name, Class<T> typeClass,
            Supplier<Capability<T>> capability) {
        return new ItemCapability<>(name, typeClass, capability);
    }

    public ResourceLocation name() {
        return name;
    }

    public Class<T> typeClass() {
        return typeClass;
    }

    public Capability<T> forgeCapability() {
        return Objects.requireNonNull(forgeCapability.get(), () -> "Capability " + name + " is not registered");
    }

    @Nullable
    public T getCapability(ItemStack stack) {
        return getCapability(stack, null);
    }

    @Nullable
    public T getCapability(ItemStack stack, @Nullable C context) {
        if (stack.isEmpty()) {
            return null;
        }
        var cap = forgeCapability.get();
        if (cap == null || !stack.hasCapability(cap, null)) {
            return null;
        }
        return stack.getCapability(cap, null);
    }

    public boolean is(@Nullable Capability<?> capability) {
        return capability != null && capability == forgeCapability.get();
    }

    @Override
    public String toString() {
        return "ItemCapability[" + name + "]";
    }
}
