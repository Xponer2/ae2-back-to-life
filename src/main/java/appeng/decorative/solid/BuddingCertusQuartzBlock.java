package appeng.decorative.solid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import appeng.block.AEBaseBlock;
import appeng.block.BlockProperties;
import appeng.core.definitions.AEBlocks;
import appeng.util.Platform;

public class BuddingCertusQuartzBlock extends AEBaseBlock {
    public static final int GROWTH_CHANCE = 5;
    public static final int DECAY_CHANCE = 12;
    private static final EnumFacing[] DIRECTIONS = EnumFacing.VALUES;

    public BuddingCertusQuartzBlock(BlockProperties properties) {
        super(properties);
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public void randomTick(IBlockState state, World level, BlockPos pos, Random randomSource) {
        if (randomSource.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }

        // Try to grow cluster
        EnumFacing direction = DIRECTIONS[randomSource.nextInt(DIRECTIONS.length)];
        BlockPos targetPos = pos.offset(direction);
        IBlockState targetState = level.getBlockState(targetPos);
        Block newCluster = null;
        if (canClusterGrowAtState(targetState)) {
            newCluster = AEBlocks.SMALL_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.SMALL_QUARTZ_BUD.block())
                && targetState.getValue(CertusQuartzClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.MEDIUM_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.MEDIUM_QUARTZ_BUD.block())
                && targetState.getValue(CertusQuartzClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.LARGE_QUARTZ_BUD.block();
        } else if (Platform.is(targetState, AEBlocks.LARGE_QUARTZ_BUD.block())
                && targetState.getValue(CertusQuartzClusterBlock.FACING) == direction) {
            newCluster = AEBlocks.QUARTZ_CLUSTER.block();
        }

        if (newCluster == null) {
            return;
        }

        // Grow certus crystal (1.12.2 has no waterlogging: a bud grown in water replaces it)
        IBlockState newClusterState = newCluster.getDefaultState()
                .withProperty(CertusQuartzClusterBlock.FACING, direction);
        level.setBlockState(targetPos, newClusterState);

        // Damage the budding certus block after a successful growth
        if (this == AEBlocks.FLAWLESS_BUDDING_QUARTZ.block() || randomSource.nextInt(DECAY_CHANCE) != 0) {
            return;
        }
        Block newBlock;
        if (this == AEBlocks.FLAWED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.CHIPPED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.CHIPPED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.DAMAGED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.DAMAGED_BUDDING_QUARTZ.block()) {
            newBlock = AEBlocks.QUARTZ_BLOCK.block();
        } else {
            throw new IllegalStateException("Unexpected block: " + this);
        }
        level.setBlockState(pos, newBlock.getDefaultState());
    }

    public static boolean canClusterGrowAtState(IBlockState state) {
        return Platform.isAir(state)
                || state.getBlock() == Blocks.WATER && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /**
     * 1.21 loot table: silk touch keeps the block (except the flawless one), otherwise it degrades one step.
     */
    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return this != AEBlocks.FLAWLESS_BUDDING_QUARTZ.block();
    }

    @Override
    public List<ItemStack> getDrops(IBlockState state, IBlockAccess level, BlockPos pos, int fortune) {
        Block drop;
        if (this == AEBlocks.FLAWLESS_BUDDING_QUARTZ.block()) {
            drop = AEBlocks.FLAWED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.FLAWED_BUDDING_QUARTZ.block()) {
            drop = AEBlocks.CHIPPED_BUDDING_QUARTZ.block();
        } else if (this == AEBlocks.CHIPPED_BUDDING_QUARTZ.block()) {
            drop = AEBlocks.DAMAGED_BUDDING_QUARTZ.block();
        } else {
            drop = AEBlocks.QUARTZ_BLOCK.block();
        }
        var result = new ArrayList<ItemStack>();
        result.add(new ItemStack(drop));
        return result;
    }
}
