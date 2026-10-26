/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.integration.modules.jei;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.gui.IGhostIngredientHandler;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.integration.modules.itemlists.DropTarget;
import appeng.integration.modules.itemlists.DropTargets;

/**
 * Lets items and fluids be dragged from JEI into the filter (fake) slots of AE2 screens, like the drag and drop
 * support of the 1.21 recipe viewer plugins.
 */
@SuppressWarnings("rawtypes")
class GhostIngredientHandler implements IGhostIngredientHandler<AEBaseScreen> {

    @Override
    public <I> List<Target<I>> getTargets(AEBaseScreen screen, I ingredient, boolean doStart) {
        var genericStack = toGenericStack(ingredient);
        if (genericStack == null) {
            return java.util.Collections.emptyList();
        }
        return DropTargets.getTargets((AEBaseScreen<?>) screen)
                .stream()
                .filter(target -> target.canDrop(genericStack))
                .map(target -> new DropTargetAdapter<I>(target))
                .collect(Collectors.toList());
    }

    @Override
    public void onComplete() {
    }

    @Nullable
    private static GenericStack toGenericStack(Object ingredient) {
        if (ingredient instanceof ItemStack itemStack) {
            return GenericStack.fromItemStack(itemStack);
        } else if (ingredient instanceof FluidStack fluidStack) {
            return GenericStack.fromFluidStack(fluidStack);
        }
        return null;
    }

    private static final class DropTargetAdapter<I> implements Target<I> {
        private final DropTarget target;

        private DropTargetAdapter(DropTarget target) {
            this.target = target;
        }

        @Override
        public Rectangle getArea() {
            var area = target.area();
            return new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight());
        }

        @Override
        public void accept(I ingredient) {
            var genericStack = toGenericStack(ingredient);
            if (genericStack != null) {
                target.drop(genericStack);
            }
        }
    }
}
