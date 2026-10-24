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

package appeng.api.util;

import net.minecraft.item.Item;

/**
 * Backport of Minecraft's ItemLike.
 */
@FunctionalInterface
public interface ItemLike {
    Item asItem();

    /**
     * 1.12.2 items don't implement this interface, wrap them.
     */
    static ItemLike of(Item item) {
        return item instanceof ItemLike itemLike ? itemLike : new ItemWrapper(item);
    }

    static ItemLike of(net.minecraft.block.Block block) {
        return block instanceof ItemLike itemLike ? itemLike : of(Item.getItemFromBlock(block));
    }

    final class ItemWrapper implements ItemLike {
        private final Item item;

        private ItemWrapper(Item item) {
            this.item = item;
        }

        @Override
        public Item asItem() {
            return item;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ItemWrapper other && other.item == item;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item);
        }

        @Override
        public String toString() {
            return String.valueOf(item.getRegistryName());
        }
    }
}
