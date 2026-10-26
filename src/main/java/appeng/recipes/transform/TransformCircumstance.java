package appeng.recipes.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.util.Registries;
import appeng.api.util.TagKey;
import appeng.core.network.RegistryFriendlyByteBuf;
import appeng.core.network.codec.StreamCodec;
import appeng.util.ResourceLocations;

public class TransformCircumstance {

    public static final TransformCircumstance EXPLOSION = new TransformCircumstance("explosion");

    /**
     * The fluid tag {@code minecraft:water} (the default circumstance).
     */
    public static final TagKey<Fluid> WATER_TAG = TagKey.create(Registries.FLUID,
            new ResourceLocation("minecraft", "water"));

    private static final MapCodec<TransformCircumstance> EXPLOSION_CODEC = MapCodec.unit(EXPLOSION);

    private static final MapCodec<FluidType> FLUID_CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            TagKey.codec(Registries.FLUID).fieldOf("tag").forGetter(FluidType::getFluidTag))
            .apply(builder, FluidType::new));

    public static final Codec<TransformCircumstance> CODEC = Codec.STRING.<TransformCircumstance>dispatch(
            t -> t.type, type -> switch (type) {
                case "explosion" -> EXPLOSION_CODEC.codec();
                case "fluid" -> FLUID_CODEC.codec();
                default -> throw new IllegalStateException("Invalid type: " + type);
            });

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformCircumstance> STREAM_CODEC = StreamCodec.ofMember(
            TransformCircumstance::toNetwork,
            TransformCircumstance::fromNetwork);

    private final String type;

    public TransformCircumstance(String type) {
        this.type = type;
    }

    static TransformCircumstance fromJson(JsonObject obj) {
        String type = obj.get("type").getAsString();
        if (type.equals("explosion"))
            return explosion();
        else if (type.equals("fluid")) {
            return fluid(TagKey.create(Registries.FLUID, ResourceLocations.parse(obj.get("tag").getAsString())));
        } else
            throw new JsonParseException("Invalid transform recipe type " + type);
    }

    static TransformCircumstance fromNetwork(RegistryFriendlyByteBuf buf) {
        String type = buf.readUtf();
        if (type.equals("explosion"))
            return explosion();
        else if (type.equals("fluid")) {
            return fluid(TagKey.create(Registries.FLUID, buf.readResourceLocation()));
        } else
            throw new DecoderException("Invalid transform recipe type " + type);
    }

    public static TransformCircumstance fluid(TagKey<Fluid> tag) {
        return new FluidType(tag);
    }

    public static TransformCircumstance explosion() {
        return EXPLOSION;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", this.type);
        return obj;
    }

    void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(type);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TransformCircumstance other && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public boolean isExplosion() {
        return type.equals("explosion");
    }

    public boolean isFluid() {
        return false;
    }

    public boolean isFluidTag(TagKey<Fluid> tag) {
        return false;
    }

    public boolean isFluid(Fluid fluid) {
        return false;
    }

    public List<Fluid> getFluidsForRendering() {
        return Collections.emptyList();
    }

    private static class FluidType extends TransformCircumstance {
        public final TagKey<Fluid> fluidTag;

        public FluidType(TagKey<Fluid> fluidTag) {
            super("fluid");
            this.fluidTag = fluidTag;
        }

        public TagKey<Fluid> getFluidTag() {
            return fluidTag;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FluidType other && Objects.equals(fluidTag, other.fluidTag);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(fluidTag);
        }

        @Override
        public JsonObject toJson() {
            JsonObject obj = super.toJson();
            obj.addProperty("tag", fluidTag.location().toString());
            return obj;
        }

        @Override
        void toNetwork(RegistryFriendlyByteBuf buf) {
            super.toNetwork(buf);
            buf.writeResourceLocation(fluidTag.location());
        }

        @Override
        public boolean isFluid() {
            return true;
        }

        @Override
        public boolean isFluid(Fluid fluid) {
            return fluidTag.contains(fluid);
        }

        @Override
        public boolean isFluidTag(TagKey<Fluid> tag) {
            return fluidTag.equals(tag);
        }

        @Override
        public List<Fluid> getFluidsForRendering() {
            var result = new ArrayList<Fluid>();
            for (var fluid : FluidRegistry.getRegisteredFluids().values()) {
                if (fluidTag.contains(fluid)) {
                    result.add(fluid);
                }
            }
            return result;
        }
    }
}
