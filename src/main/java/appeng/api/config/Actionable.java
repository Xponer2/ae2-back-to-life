/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
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

package appeng.api.config;

public enum Actionable {
    /**
     * Perform the intended action.
     */
    MODULATE(true),

    /**
     * Pretend to perform the action.
     */
    SIMULATE(false);

    private final boolean fluidAction;

    Actionable(boolean fluidAction) {
        this.fluidAction = fluidAction;
    }

    /**
     * @param doIt The Forge 1.12.2 fluid API "doFill"/"doDrain" flag.
     */
    public static Actionable of(boolean doIt) {
        return doIt ? MODULATE : SIMULATE;
    }

    public static Actionable ofSimulate(boolean simulate) {
        return simulate ? SIMULATE : MODULATE;
    }

    /**
     * @return The value to pass as "doFill"/"doDrain" to Forge 1.12.2 fluid handlers.
     */
    public boolean getFluidAction() {
        return fluidAction;
    }

    public boolean isSimulate() {
        return this == SIMULATE;
    }
}
