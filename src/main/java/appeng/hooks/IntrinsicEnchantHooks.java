/*
 * This file is part of AE2: Back to Life, an unofficial port of Applied Energistics 2 to Minecraft 1.12.2.
 * Copyright (c) 2013 - 2026, AlgorithmX2, the Applied Energistics 2 contributors and the AE2: Back to Life
 * contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. See LICENSE.
 */

package appeng.hooks;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Applies the intrinsic fortune and looting levels of {@link IntrinsicEnchantItem}s (1.21 does this in
 * EnchantmentHelper): if the tool has no higher real enchantment, drops are rolled again with the intrinsic level.
 */
public final class IntrinsicEnchantHooks {
    private IntrinsicEnchantHooks() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        var player = event.getHarvester();
        if (player == null || event.isSilkTouching()) {
            return;
        }
        ItemStack tool = player.getHeldItemMainhand();
        if (!(tool.getItem() instanceof IntrinsicEnchantItem item)) {
            return;
        }
        int fortune = item.getIntrinsicEnchantLevel(tool, Enchantments.FORTUNE);
        if (fortune <= event.getFortuneLevel()) {
            return;
        }
        var state = event.getState();
        var drops = NonNullList.<ItemStack>create();
        state.getBlock().getDrops(drops, event.getWorld(), event.getPos(), state, fortune);
        event.getDrops().clear();
        event.getDrops().addAll(drops);
    }

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        var source = event.getDamageSource();
        if (source == null || !(source.getTrueSource() instanceof EntityLivingBase attacker)) {
            return;
        }
        ItemStack weapon = attacker.getHeldItemMainhand();
        if (weapon.getItem() instanceof IntrinsicEnchantItem item) {
            int looting = item.getIntrinsicEnchantLevel(weapon, Enchantments.LOOTING);
            if (looting > event.getLootingLevel()) {
                event.setLootingLevel(looting);
            }
        }
    }
}
