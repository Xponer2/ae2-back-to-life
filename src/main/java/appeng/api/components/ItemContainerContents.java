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

package appeng.api.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import appeng.util.AECodecs;
import appeng.util.Platform;

/**
 * Backport of Minecraft 1.20.5's ItemContainerContents: an immutable, slot indexed list of item stacks.
 */
public final class ItemContainerContents {
    public static final ItemContainerContents EMPTY = new ItemContainerContents(Collections.emptyList());

    private static final class Slot {
        final int index;
        final ItemStack item;

        Slot(int index, ItemStack item) {
            this.index = index;
            this.item = item;
        }
    }

    private static final Codec<Slot> SLOT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 255).fieldOf("slot").forGetter(s -> s.index),
            AECodecs.ITEM_STACK.fieldOf("item").forGetter(s -> s.item)).apply(i, Slot::new));

    public static final Codec<ItemContainerContents> CODEC = SLOT_CODEC.listOf()
            .xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);

    public static final Object STREAM_CODEC = null;

    private final List<ItemStack> items;

    private ItemContainerContents(List<ItemStack> items) {
        this.items = items;
    }

    public static ItemContainerContents fromItems(List<ItemStack> items) {
        int last = items.size() - 1;
        while (last >= 0 && items.get(last).isEmpty()) {
            last--;
        }
        if (last < 0) {
            return EMPTY;
        }
        var copy = new ArrayList<ItemStack>(last + 1);
        for (int i = 0; i <= last; i++) {
            copy.add(items.get(i).copy());
        }
        return new ItemContainerContents(copy);
    }

    private static ItemContainerContents fromSlots(List<Slot> slots) {
        int size = 0;
        for (var s : slots) {
            size = Math.max(size, s.index + 1);
        }
        var list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (var s : slots) {
            list.set(s.index, s.item);
        }
        return new ItemContainerContents(list);
    }

    private List<Slot> asSlots() {
        var result = new ArrayList<Slot>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                result.add(new Slot(i, items.get(i)));
            }
        }
        return result;
    }

    public void copyInto(NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        }
    }

    public ItemStack copyOne() {
        return items.isEmpty() ? ItemStack.EMPTY : items.get(0).copy();
    }

    public Stream<ItemStack> stream() {
        return items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return items.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy);
    }

    public Iterable<ItemStack> nonEmptyItems() {
        return () -> items.stream().filter(s -> !s.isEmpty()).iterator();
    }

    public int getSlots() {
        return items.size();
    }

    public ItemStack getStackInSlot(int slot) {
        return slot < items.size() ? items.get(slot).copy() : ItemStack.EMPTY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemContainerContents other) || other.items.size() != items.size()) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.areItemStacksEqual(items.get(i), other.items.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (var s : items) {
            h = 31 * h + Platform.hashItemAndComponents(s);
        }
        return h;
    }
}
