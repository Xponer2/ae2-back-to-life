package appeng.client.render.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.crafting.CraftingUnitType;
import appeng.core.AppEng;

@SideOnly(Side.CLIENT)
public class CraftingUnitModelProvider extends AbstractCraftingUnitModelProvider<CraftingUnitType> {

    private static final List<ResourceLocation> MATERIALS = new ArrayList<>();

    protected final static ResourceLocation RING_CORNER = texture("ring_corner");
    protected final static ResourceLocation RING_SIDE_HOR = texture("ring_side_hor");
    protected final static ResourceLocation RING_SIDE_VER = texture("ring_side_ver");
    protected final static ResourceLocation UNIT_BASE = texture("unit_base");
    protected final static ResourceLocation LIGHT_BASE = texture("light_base");
    protected final static ResourceLocation ACCELERATOR_LIGHT = texture("accelerator_light");
    protected final static ResourceLocation STORAGE_1K_LIGHT = texture("1k_storage_light");
    protected final static ResourceLocation STORAGE_4K_LIGHT = texture("4k_storage_light");
    protected final static ResourceLocation STORAGE_16K_LIGHT = texture("16k_storage_light");
    protected final static ResourceLocation STORAGE_64K_LIGHT = texture("64k_storage_light");
    protected final static ResourceLocation STORAGE_256K_LIGHT = texture("256k_storage_light");
    protected final static ResourceLocation MONITOR_BASE = texture("monitor_base");
    protected final static ResourceLocation MONITOR_LIGHT_DARK = texture("monitor_light_dark");
    protected final static ResourceLocation MONITOR_LIGHT_MEDIUM = texture("monitor_light_medium");
    protected final static ResourceLocation MONITOR_LIGHT_BRIGHT = texture("monitor_light_bright");

    public CraftingUnitModelProvider(CraftingUnitType type) {
        super(type);
    }

    @Override
    public List<ResourceLocation> getMaterials() {
        return Collections.unmodifiableList(MATERIALS);
    }

    public TextureAtlasSprite getLightMaterial(Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        return switch (this.type) {
            case ACCELERATOR -> textureGetter.apply(ACCELERATOR_LIGHT);
            case STORAGE_1K -> textureGetter.apply(STORAGE_1K_LIGHT);
            case STORAGE_4K -> textureGetter.apply(STORAGE_4K_LIGHT);
            case STORAGE_16K -> textureGetter.apply(STORAGE_16K_LIGHT);
            case STORAGE_64K -> textureGetter.apply(STORAGE_64K_LIGHT);
            case STORAGE_256K -> textureGetter.apply(STORAGE_256K_LIGHT);
            default -> throw new IllegalArgumentException(
                    "Crafting unit type " + this.type + " does not use a light texture.");
        };
    }

    @Override
    public IBakedModel getBakedModel(Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
        TextureAtlasSprite ringCorner = spriteGetter.apply(RING_CORNER);
        TextureAtlasSprite ringSideHor = spriteGetter.apply(RING_SIDE_HOR);
        TextureAtlasSprite ringSideVer = spriteGetter.apply(RING_SIDE_VER);

        return switch (type) {
            case UNIT -> new UnitBakedModel(ringCorner, ringSideHor, ringSideVer,
                    spriteGetter.apply(UNIT_BASE));
            case ACCELERATOR, STORAGE_1K, STORAGE_4K, STORAGE_16K, STORAGE_64K, STORAGE_256K -> new LightBakedModel(
                    ringCorner, ringSideHor, ringSideVer, spriteGetter.apply(LIGHT_BASE),
                    this.getLightMaterial(spriteGetter));
            case MONITOR -> new MonitorBakedModel(ringCorner, ringSideHor, ringSideVer,
                    spriteGetter.apply(UNIT_BASE), spriteGetter.apply(MONITOR_BASE),
                    spriteGetter.apply(MONITOR_LIGHT_DARK),
                    spriteGetter.apply(MONITOR_LIGHT_MEDIUM),
                    spriteGetter.apply(MONITOR_LIGHT_BRIGHT));
        };
    }

    private static ResourceLocation texture(String name) {
        var mat = AppEng.makeId("block/crafting/" + name);
        MATERIALS.add(mat);
        return mat;
    }
}
