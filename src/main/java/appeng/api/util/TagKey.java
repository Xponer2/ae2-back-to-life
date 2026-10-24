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

import java.util.Objects;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

import appeng.core.tags.TagManager;

/**
 * Backport of Minecraft's tag keys. Minecraft 1.12.2 has no tags, AE2 ships its own minimal tag system which reads the
 * same {@code data/<namespace>/tags/<registry>/<path>.json} files as modern Minecraft (see {@link TagManager}).
 */
public final class TagKey<T> {
    private final Registries.RegistryKey<T> registry;
    private final ResourceLocation location;

    private TagKey(Registries.RegistryKey<T> registry, ResourceLocation location) {
        this.registry = registry;
        this.location = location;
    }

    public static <T> TagKey<T> create(Registries.RegistryKey<T> registry, ResourceLocation location) {
        return new TagKey<>(registry, location);
    }

    public static <T> Codec<TagKey<T>> codec(Registries.RegistryKey<T> registry) {
        return Codec.STRING.xmap(s -> create(registry, new ResourceLocation(s.startsWith("#") ? s.substring(1) : s)),
                t -> t.location.toString());
    }

    public static <T> Codec<TagKey<T>> hashedCodec(Registries.RegistryKey<T> registry) {
        return Codec.STRING.xmap(s -> create(registry, new ResourceLocation(s.substring(1))),
                t -> "#" + t.location);
    }

    public Registries.RegistryKey<T> registry() {
        return registry;
    }

    public ResourceLocation location() {
        return location;
    }

    public boolean isFor(Registries.RegistryKey<?> registry) {
        return this.registry == registry;
    }

    /**
     * Item tags check item + metadata.
     */
    public boolean contains(ItemStack stack) {
        return !stack.isEmpty() && TagManager.isInItemTag(location, stack.getItem(), stack.getMetadata());
    }

    public boolean contains(IBlockState state) {
        if (registry == Registries.ITEM) {
            var item = Item.getItemFromBlock(state.getBlock());
            return TagManager.isInItemTag(location, item, state.getBlock().damageDropped(state));
        }
        return TagManager.isInBlockTag(location, state.getBlock(), state.getBlock().getMetaFromState(state));
    }

    public boolean contains(Block block) {
        return TagManager.isInBlockTag(location, block, -1);
    }

    public boolean contains(Item item) {
        return TagManager.isInItemTag(location, item, -1);
    }

    public boolean contains(Fluid fluid) {
        return TagManager.isInFluidTag(location, fluid);
    }

    /**
     * For tags of registries without items/blocks (data component types): is the entry with this id in the tag?
     */
    public boolean containsId(ResourceLocation id) {
        return TagManager.getIds(registry.name(), location).contains(id);
    }

    /**
     * For tags of registries without items/blocks (data component types): ids of all entries.
     */
    public java.util.Set<ResourceLocation> ids() {
        return TagManager.getIds(registry.name(), location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TagKey<?> other)) {
            return false;
        }
        return registry == other.registry && location.equals(other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registry.name(), location);
    }

    @Override
    public String toString() {
        return "TagKey[" + registry.name() + " / " + location + "]";
    }
}
