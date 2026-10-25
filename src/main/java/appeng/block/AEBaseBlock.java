/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.block;

import appeng.util.ItemTooltipContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.api.orientation.IOrientableBlock;
import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.core.CreativeTabOutput;
import appeng.hooks.WrenchHook;
import appeng.util.BlockPlaceContext;
import appeng.util.ItemInteractionResult;
import appeng.util.shape.CollisionContext;
import appeng.util.shape.Shapes;
import appeng.util.shape.VoxelShape;

/**
 * Base class of all AE2 blocks. Bridges the Minecraft 1.21 block API that AE2's blocks are written against
 * ({@link #useItemOn}, {@link #useWithoutItem}, {@link #getShape}, {@link #onRemove}, ...) to Minecraft 1.12.2.
 * <p>
 * Block state properties are stored in the metadata if they are returned by {@link #getMetaProperties()} (by default
 * all properties of non-tile-entity blocks); all others must be computed in {@link #getActualState}.
 */
public abstract class AEBaseBlock extends Block implements IOrientableBlock {
    protected final BlockProperties properties;
    @Nullable
    private List<IProperty<?>> metaProperties;
    @Nullable
    private List<List<Comparable<?>>> metaValues;

    protected AEBaseBlock(BlockProperties props) {
        super(props.getMaterial(), props.getMapColor());
        this.properties = props.copy();
        this.properties.applyTo(this);
        setSoundType(this.properties.getSoundType());
        // Recompute values that the Block constructor derived before our fields were initialized
        this.fullBlock = this.getDefaultState().isOpaqueCube();
        this.lightOpacity = this.fullBlock ? 255 : 0;
        this.translucent = !this.properties.canOcclude();
    }

    public BlockProperties getProperties() {
        return properties;
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.none();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Block state definition
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        var builder = new BlockStateContainer.Builder(this);
        createBlockStateDefinition(builder);
        return builder.build();
    }

    protected void createBlockStateDefinition(BlockStateContainer.Builder builder) {
        for (var property : getOrientationStrategy().getProperties()) {
            builder.add(property);
        }
    }

    /**
     * Properties that are persisted in the block metadata. Their combinations must fit into 16 values.
     */
    protected List<IProperty<?>> getMetaProperties() {
        return new ArrayList<>(getBlockState().getProperties());
    }

    private void initMeta() {
        if (metaProperties != null) {
            return;
        }
        var props = new ArrayList<IProperty<?>>();
        var values = new ArrayList<List<Comparable<?>>>();
        int combinations = 1;
        for (var prop : getMetaProperties()) {
            var allowed = new ArrayList<Comparable<?>>(prop.getAllowedValues());
            if (combinations * allowed.size() > 16) {
                appeng.core.AELog.error("Block %s: property %s does not fit into the metadata anymore", this,
                        prop.getName());
                continue;
            }
            combinations *= allowed.size();
            props.add(prop);
            values.add(allowed);
        }
        metaValues = values;
        metaProperties = props;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        initMeta();
        int meta = 0;
        int radix = 1;
        for (int i = 0; i < metaProperties.size(); i++) {
            var values = metaValues.get(i);
            int index = values.indexOf(state.getValue(metaProperties.get(i)));
            meta += Math.max(0, index) * radix;
            radix *= values.size();
        }
        return meta;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IBlockState getStateFromMeta(int meta) {
        initMeta();
        IBlockState state = getDefaultState();
        int rest = meta;
        for (int i = 0; i < metaProperties.size(); i++) {
            var values = metaValues.get(i);
            int index = rest % values.size();
            rest /= values.size();
            state = state.withProperty((IProperty) metaProperties.get(i), (Comparable) values.get(index));
        }
        return state;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Factories for properties
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Utility function to create block properties with some sensible defaults for AE blocks.
     */
    public static BlockProperties defaultProps(MapColor mapColor, SoundType soundType) {
        return BlockProperties.of()
                // These values previously were encoded in AEBaseBlock
                .sound(soundType)
                .strength(2.2f, 11.f)
                .mapColor(mapColor);
    }

    public static BlockProperties stoneProps() {
        return defaultProps(MapColor.STONE, SoundType.STONE).forceSolidOn();
    }

    public static BlockProperties metalProps() {
        return defaultProps(MapColor.IRON, SoundType.METAL).forceSolidOn();
    }

    public static BlockProperties glassProps() {
        return defaultProps(MapColor.AIR, SoundType.GLASS);
    }

    public static BlockProperties fixtureProps() {
        return defaultProps(MapColor.IRON, SoundType.GLASS)
                .noCollission()
                .noOcclusion()
                .pushReactionDestroy();
    }

    public void addToMainCreativeTab(CreativeTabOutput.DisplayParameters parameters, CreativeTabOutput output) {
        output.accept(this);
    }

    @Override
    public String toString() {
        String regName = this.getRegistryName() != null ? this.getRegistryName().getPath() : "unregistered";
        return this.getClass().getSimpleName() + "[" + regName + "]";
    }

    /**
     * Modern translation key ({@code block.ae2.<path>}), used directly by AE2's lang files.
     */
    public String getDescriptionId() {
        ResourceLocation id = getRegistryName();
        return id == null ? "block.ae2.unknown" : "block." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    @Override
    public String getTranslationKey() {
        return getDescriptionId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getLocalizedName() {
        return net.minecraft.util.text.translation.I18n.translateToLocal(getDescriptionId());
    }

    public ITextComponent getName() {
        return new net.minecraft.util.text.TextComponentTranslation(getDescriptionId());
    }

    /**
     * Tooltip lines for the block item.
     */
    @SideOnly(Side.CLIENT)
    public void appendHoverText(ItemStack stack, ItemTooltipContext context, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Placement
    // ---------------------------------------------------------------------------------------------------------------

    @Nullable
    public IBlockState getStateForPlacement(BlockPlaceContext context) {
        var state = this.getDefaultState();
        return getOrientationStrategy().getStateForPlacement(state, context);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        var player = placer instanceof EntityPlayer p ? p : null;
        var stack = player != null ? player.getHeldItem(hand) : ItemStack.EMPTY;
        var context = BlockPlaceContext.at(world, player, hand, stack, pos, facing, hitX, hitY, hitZ);
        var state = getStateForPlacement(context);
        return state != null ? state : getDefaultState();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer,
            ItemStack stack) {
        setPlacedBy(worldIn, pos, state, placer, stack);
    }

    public void setPlacedBy(World level, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer,
            ItemStack stack) {
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        onPlace(state, worldIn, pos, Blocks.AIR.getDefaultState(), false);
    }

    protected void onPlace(IBlockState state, World level, BlockPos pos, IBlockState oldState, boolean movedByPiston) {
    }

    /**
     * Modern canSurvive: whether the block can stay at its position.
     */
    /**
     * 1.21 Block.canSupportCenter: the face of the block at pos can hold something attached to its center (torches,
     * fixtures).
     */
    public static boolean canSupportCenter(IBlockAccess level, BlockPos pos, EnumFacing face) {
        var shape = level.getBlockState(pos).getBlockFaceShape(level, pos, face);
        return shape == BlockFaceShape.SOLID || shape == BlockFaceShape.CENTER || shape == BlockFaceShape.CENTER_BIG
                || shape == BlockFaceShape.MIDDLE_POLE || shape == BlockFaceShape.MIDDLE_POLE_THICK;
    }

    public boolean canSurvive(IBlockState state, World level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Interaction
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        var hit = new RayTraceResult(new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), facing,
                pos);
        var actualState = state.getActualState(worldIn, pos);
        var heldItem = player.getHeldItem(hand);
        var result = useItemOn(heldItem, actualState, worldIn, pos, player, hand, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == EnumHand.MAIN_HAND) {
            return useWithoutItem(actualState, worldIn, pos, player, hit) == EnumActionResult.SUCCESS;
        }
        return result.consumesAction();
    }

    protected ItemInteractionResult useItemOn(ItemStack heldItem, IBlockState state, World level, BlockPos pos,
            EntityPlayer player, EnumHand hand, RayTraceResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected EnumActionResult useWithoutItem(IBlockState state, World level, BlockPos pos, EntityPlayer player,
            RayTraceResult hitResult) {
        return EnumActionResult.PASS;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        attack(worldIn.getBlockState(pos), worldIn, pos, playerIn);
    }

    protected void attack(IBlockState state, World level, BlockPos pos, EntityPlayer player) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
            EntityPlayer player) {
        return getCloneItemStack(world, pos, state.getActualState(world, pos));
    }

    public ItemStack getCloneItemStack(World level, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Shapes
    // ---------------------------------------------------------------------------------------------------------------

    public VoxelShape getShape(IBlockState state, IBlockAccess level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess level, BlockPos pos,
            CollisionContext context) {
        return properties.hasCollision() ? getShape(state, level, pos, context) : Shapes.empty();
    }

    protected VoxelShape getShapeSafe(IBlockState state, IBlockAccess level, BlockPos pos) {
        try {
            return getShape(state.getActualState(level, pos), level, pos, CollisionContext.empty());
        } catch (RuntimeException e) {
            return Shapes.block();
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        var shape = getShapeSafe(state, source, pos);
        return shape.isEmpty() ? FULL_BLOCK_AABB : shape.bounds();
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        var shape = getCollisionShape(state.getActualState(worldIn, pos), worldIn, pos, CollisionContext.empty());
        return shape.isEmpty() ? NULL_AABB : shape.bounds();
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox,
            List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        var actual = isActualState ? state : state.getActualState(worldIn, pos);
        var shape = getCollisionShape(actual, worldIn, pos, CollisionContext.of(entityIn));
        for (var box : shape.toAabbs()) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World worldIn, BlockPos pos, Vec3d start,
            Vec3d end) {
        var shape = getShapeSafe(blockState, worldIn, pos);
        if (shape.isEmpty()) {
            return null;
        }
        RayTraceResult best = null;
        double bestDist = Double.MAX_VALUE;
        for (var box : shape.toAabbs()) {
            var hit = rayTrace(pos, start, end, box);
            if (hit != null) {
                double dist = hit.hitVec.squareDistanceTo(start);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = hit;
                }
            }
        }
        return best;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return getBoundingBox(state, worldIn, pos).offset(pos);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rendering / opacity
    // ---------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return properties == null || properties.canOcclude();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(IBlockState state) {
        return properties == null || properties.canOcclude();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isNormalCube(IBlockState state) {
        return properties == null || (properties.canOcclude() && properties.isRedstoneConductor());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return isOpaqueCube(state) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    public RenderShape getRenderShape(IBlockState state) {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return getRenderShape(state).toVanilla();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getLightValue(IBlockState state) {
        return properties == null ? 0 : properties.getLightEmission().applyAsInt(state);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state.getActualState(world, pos));
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos,
            net.minecraft.entity.EntityLiving.SpawnPlacementType type) {
        return properties.isValidSpawn() && super.canCreatureSpawn(state, world, pos, type);
    }

    @SideOnly(Side.CLIENT)
    public void animateTick(IBlockState state, World level, BlockPos pos, Random random) {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        animateTick(stateIn.getActualState(worldIn, pos), worldIn, pos, rand);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Redstone
    // ---------------------------------------------------------------------------------------------------------------

    public boolean isSignalSource(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(IBlockState state) {
        return isSignalSource(state);
    }

    public int getSignal(IBlockState state, IBlockAccess level, BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getSignal(blockState.getActualState(blockAccess, pos), blockAccess, pos, side);
    }

    public int getDirectSignal(IBlockState state, IBlockAccess level, BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return getDirectSignal(blockState.getActualState(blockAccess, pos), blockAccess, pos, side);
    }

    public boolean hasAnalogOutputSignal(IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return hasAnalogOutputSignal(state);
    }

    public int getAnalogOutputSignal(IBlockState state, World level, BlockPos pos) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        return getAnalogOutputSignal(blockState.getActualState(worldIn, pos), worldIn, pos);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Neighbor updates / ticking
    // ---------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        neighborChanged(state, worldIn, pos, blockIn, fromPos, false);

        // Emulate modern updateShape: blocks can return air to break themselves (e.g. attached fixtures)
        var direction = facingTo(pos, fromPos);
        if (direction != null && worldIn.getBlockState(pos).getBlock() == this) {
            var current = worldIn.getBlockState(pos);
            var updated = updateShape(current, direction, worldIn.getBlockState(fromPos), worldIn, pos, fromPos);
            if (updated.getBlock() == Blocks.AIR) {
                worldIn.destroyBlock(pos, true);
            } else if (updated != current && getMetaFromState(updated) != getMetaFromState(current)) {
                worldIn.setBlockState(pos, updated, 3);
            }
        }
    }

    @Nullable
    private static EnumFacing facingTo(BlockPos pos, BlockPos other) {
        for (var facing : EnumFacing.VALUES) {
            if (pos.offset(facing).equals(other)) {
                return facing;
            }
        }
        return null;
    }

    public void neighborChanged(IBlockState state, World level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
    }

    /**
     * Modern updateShape. Return {@link Blocks#AIR} to break the block.
     */
    public IBlockState updateShape(IBlockState state, EnumFacing facing, IBlockState facingState, World level,
            BlockPos currentPos, BlockPos facingPos) {
        return state;
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        onNeighborChange(world.getBlockState(pos), world, pos, neighbor);
    }

    public void onNeighborChange(IBlockState state, IBlockAccess level, BlockPos pos, BlockPos neighbor) {
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        tick(state, worldIn, pos, rand);
    }

    public void tick(IBlockState state, World level, BlockPos pos, Random random) {
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
        randomTick(state, worldIn, pos, random);
    }

    public void randomTick(IBlockState state, World level, BlockPos pos, Random random) {
        tick(state, level, pos, random);
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        entityInside(state, worldIn, pos, entityIn);
    }

    public void entityInside(IBlockState state, World level, BlockPos pos, Entity entity) {
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        stepOn(worldIn, pos, worldIn.getBlockState(pos), entityIn);
    }

    public void stepOn(World level, BlockPos pos, IBlockState state, Entity entity) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param) {
        return triggerEvent(state, worldIn, pos, id, param);
    }

    public boolean triggerEvent(IBlockState state, World level, BlockPos pos, int id, int param) {
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Removal
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        onRemove(state, worldIn, pos, worldIn.getBlockState(pos), false);
        super.breakBlock(worldIn, pos, state);
    }

    /**
     * Modern onRemove: called when this block is replaced by another block (newState) at pos.
     */
    public void onRemove(IBlockState state, World level, BlockPos pos, IBlockState newState, boolean isMoving) {
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        super.onBlockHarvested(worldIn, pos, state, player);
        playerWillDestroy(worldIn, pos, state, player);
    }

    public IBlockState playerWillDestroy(World level, BlockPos pos, IBlockState state, EntityPlayer player) {
        return state;
    }

    @Override
    public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
        wasExploded(worldIn, pos, explosionIn);
    }

    public void wasExploded(World level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
            IBlockState state, int fortune) {
        if (properties.isNoLootTable()) {
            return;
        }
        drops.addAll(getDrops(state, world, pos, fortune));
    }

    /**
     * Drops of this block (modern loot table equivalent). Defaults to the block itself.
     */
    public List<ItemStack> getDrops(IBlockState state, IBlockAccess level, BlockPos pos, int fortune) {
        var item = getItemDropped(state, RANDOM, fortune);
        if (item == net.minecraft.init.Items.AIR) {
            return Collections.emptyList();
        }
        List<ItemStack> result = new ArrayList<>();
        result.add(new ItemStack(item, quantityDropped(state, fortune, RANDOM), damageDropped(state)));
        return result;
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!WrenchHook.isDisassembling()) {
            super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean addDestroyEffects(World world, BlockPos pos, net.minecraft.client.particle.ParticleManager manager) {
        // Suppress break particles & sound when being disassembled by a wrench
        return WrenchHook.isDisassembling();
    }
}
