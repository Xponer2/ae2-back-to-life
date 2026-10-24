package appeng.api.stacks;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.components.DataComponentType;
import appeng.api.storage.AEKeyFilter;
import appeng.api.util.TagKey;
import appeng.core.AELog;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.util.AECodecs;
import appeng.util.BuiltInRegistries;
import appeng.util.DataResults;
import appeng.util.NbtOps;
import appeng.util.Platform;

/**
 * A fluid key on 1.12.2 is the combination of a Forge fluid and the optional FluidStack NBT.
 */
public final class AEFluidKey extends AEKey {
    public static final MapCodec<AEFluidKey> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    AECodecs.FLUID.fieldOf("id").forGetter(AEFluidKey::getFluid),
                    AECodecs.COMPOUND_TAG.optionalFieldOf("tag").forGetter(key -> Optional.ofNullable(key.tag)))
                    .apply(instance, (fluid, tag) -> new AEFluidKey(fluid, tag.orElse(null))));
    public static final Codec<AEFluidKey> CODEC = MAP_CODEC.codec();

    public static final int AMOUNT_BUCKET = 1000;
    public static final int AMOUNT_BLOCK = 1000;

    private final Fluid fluid;
    @Nullable
    private final NBTTagCompound tag;
    private final int hashCode;

    private AEFluidKey(Fluid fluid, @Nullable NBTTagCompound tag) {
        Preconditions.checkArgument(fluid != null, "fluid was null");
        this.fluid = fluid;
        this.tag = tag == null || tag.isEmpty() ? null : tag.copy();
        this.hashCode = Objects.hash(fluid, this.tag);
    }

    public static AEFluidKey of(Fluid fluid) {
        return new AEFluidKey(fluid, null);
    }

    public static AEFluidKey of(Fluid fluid, @Nullable NBTTagCompound tag) {
        return new AEFluidKey(fluid, tag);
    }

    @Nullable
    public static AEFluidKey of(@Nullable FluidStack fluidVariant) {
        if (fluidVariant == null || fluidVariant.getFluid() == null || fluidVariant.amount <= 0) {
            return null;
        }
        return new AEFluidKey(fluidVariant.getFluid(), fluidVariant.tag);
    }

    public static boolean matches(AEKey what, FluidStack fluid) {
        return what instanceof AEFluidKey fluidKey && fluidKey.matches(fluid);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEFluidKey;
    }

    public static AEKeyFilter filter() {
        return AEFluidKey::is;
    }

    public boolean matches(@Nullable FluidStack variant) {
        if (variant == null || variant.getFluid() != fluid) {
            return false;
        }
        var otherTag = variant.tag == null || variant.tag.isEmpty() ? null : variant.tag;
        return Objects.equals(tag, otherTag);
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.fluids();
    }

    @Override
    public AEFluidKey dropSecondary() {
        return of(fluid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AEFluidKey aeFluidKey = (AEFluidKey) o;
        // The hash code comparison is a fast-fail cheap check
        return hashCode == aeFluidKey.hashCode && fluid == aeFluidKey.fluid && Objects.equals(tag, aeFluidKey.tag);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Nullable
    public static AEFluidKey fromTag(NBTTagCompound tag) {
        try {
            return DataResults.getOrThrow(CODEC.decode(NbtOps.INSTANCE, tag)).getFirst();
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid fluid key from NBT: %s", tag, e);
            return null;
        }
    }

    @Override
    public NBTTagCompound toTag() {
        return (NBTTagCompound) DataResults.getOrThrow(CODEC.encodeStart(NbtOps.INSTANCE, this));
    }

    @Override
    public Object getPrimaryKey() {
        return fluid;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(Platform.getFluidId(fluid));
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World level, BlockPos pos) {
        // Fluids are voided
    }

    @Override
    protected ITextComponent computeDisplayName() {
        return new TextComponentString(toStack(AMOUNT_BUCKET).getLocalizedName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTagged(TagKey<?> tag) {
        // This will just return false for incorrectly cast tags
        return ((TagKey<Fluid>) tag).contains(fluid);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<T> type) {
        return type.get(tag);
    }

    @Override
    public boolean hasComponents() {
        return tag != null;
    }

    public FluidStack toStack(int amount) {
        return new FluidStack(fluid, amount, tag == null ? null : tag.copy());
    }

    public Fluid getFluid() {
        return fluid;
    }

    @Nullable
    public NBTTagCompound getTag() {
        return tag;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        data.writeUtf(net.minecraftforge.fluids.FluidRegistry.getFluidName(fluid));
        data.writeNbt(tag);
    }

    @Nullable
    public static AEFluidKey fromPacket(RegistryFriendlyByteBuf data) {
        var fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(data.readUtf());
        var tag = data.readNbt();
        return fluid != null ? new AEFluidKey(fluid, tag) : null;
    }

    public static boolean is(@Nullable GenericStack stack) {
        return stack != null && stack.what() instanceof AEFluidKey;
    }

    @Override
    public String toString() {
        var id = BuiltInRegistries.FLUID.getKey(getFluid());
        return tag == null ? id.toString() : id + " (+nbt)";
    }
}
