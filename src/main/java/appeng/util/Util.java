/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Subset of Minecraft's {@code net.minecraft.Util}.
 */
public final class Util {
    private Util() {
    }

    public static <T> T make(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
    }

    public static <T> T make(Supplier<T> supplier) {
        return supplier.get();
    }

    public static long getMillis() {
        return System.nanoTime() / 1000000L;
    }

    public static long getNanos() {
        return System.nanoTime();
    }
}
