package appeng.decorative.solid;

import net.minecraft.init.Blocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseBlock;
import appeng.util.BlockPlaceContext;
import appeng.block.BlockProperties;
import appeng.block.BlockStateProperties;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

/**
 * Quartz buds and the quartz cluster (1.21 AmethystClusterBlock). 1.12.2 has no waterlogging.
 */
public class CertusQuartzClusterBlock extends AEBaseBlock {
    public static final PropertyDirection FACING = BlockStateProperties.FACING;
    protected final VoxelShape northAabb;
    protected final VoxelShape southAabb;
    protected final VoxelShape eastAabb;
    protected final VoxelShape westAabb;
    protected final VoxelShape upAabb;
    protected final VoxelShape downAabb;

    public CertusQuartzClusterBlock(int i, int j, BlockProperties properties) {
        super(properties);
        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.UP));
        this.upAabb = Shapes.box16(j, 0.0, j, (16 - j), i, (16 - j));
        this.downAabb = Shapes.box16(j, (16 - i), j, (16 - j), 16.0, (16 - j));
        this.northAabb = Shapes.box16(j, j, (16 - i), (16 - j), (16 - j), 16.0);
        this.southAabb = Shapes.box16(j, j, 0.0, (16 - j), (16 - j), i);
        this.eastAabb = Shapes.box16(0.0, j, j, i, (16 - j), (16 - j));
        this.westAabb = Shapes.box16((16 - i), j, j, 16.0, (16 - j), (16 - j));
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> this.northAabb;
            case SOUTH -> this.southAabb;
            case EAST -> this.eastAabb;
            case WEST -> this.westAabb;
            case DOWN -> this.downAabb;
            case UP -> this.upAabb;
        };
    }

    /**
     * 1.21 loot table: silk touch keeps the block; otherwise buds drop dust and the cluster 4 crystals (+ fortune).
     * Nothing drops without a player, to avoid certus dust and crystals floating around when the supporting block is
     * destroyed.
     */
    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public List<ItemStack> getDrops(IBlockState state, IBlockAccess level, BlockPos pos, int fortune) {
        if (harvesters.get() == null) {
            return Collections.emptyList();
        }
        var result = new ArrayList<ItemStack>();
        if (this == AEBlocks.QUARTZ_CLUSTER.block()) {
            // uniform_bonus_count with a multiplier of 1
            int count = 4 + (fortune > 0 ? RANDOM.nextInt(fortune + 1) : 0);
            result.add(AEItems.CERTUS_QUARTZ_CRYSTAL.stack(count));
        } else {
            result.add(AEItems.CERTUS_QUARTZ_DUST.stack());
        }
        return result;
    }

    @Override
    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        EnumFacing direction = state.getValue(FACING);
        BlockPos blockPos = pos.offset(direction.getOpposite());
        return level.getBlockState(blockPos).getBlockFaceShape(level, blockPos, direction) == BlockFaceShape.SOLID;
    }

    @Override
    public IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighborState, World level,
            BlockPos currentPos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, currentPos)
                ? Blocks.AIR.getDefaultState()
                : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Nullable
    @Override
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).withProperty(FACING, context.getClickedFace());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        builder.add(FACING);
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * The shape is smaller than the block (1.21 only occludes with the shape), so the neighbors' faces must not be
     * culled and light passes through.
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }
}
