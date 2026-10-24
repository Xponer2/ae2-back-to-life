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

import java.util.function.Function;

import com.mojang.serialization.DataResult;

/**
 * Helpers bridging the DataResult API of DFU 4 (Java 8) and the newer one used by AE2 for 1.21.
 */
public final class DataResults {
    private DataResults() {
    }

    public static boolean isError(DataResult<?> result) {
        return result.error().isPresent();
    }

    public static boolean isSuccess(DataResult<?> result) {
        return !result.error().isPresent();
    }

    public static String message(DataResult<?> result) {
        return result.error().map(DataResult.PartialResult::message).orElse("");
    }

    public static <T> T getOrThrow(DataResult<T> result) {
        return getOrThrow(result, IllegalStateException::new);
    }

    public static <T, E extends Throwable> T getOrThrow(DataResult<T> result, Function<String, E> exceptionSupplier)
            throws E {
        var error = result.error();
        if (error.isPresent()) {
            throw exceptionSupplier.apply(error.get().message());
        }
        return result.result().get();
    }
}
