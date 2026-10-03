package Alone818.com.alone_adventure.armor;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class GeoArmorConfig {

    public final String id;

    public final ResourceLocation model;

    public final ResourceLocation texture;

    public final ResourceLocation animation;

    public final boolean dyeable;

    public final int defaultColor;

    /**
     * 普通纹理层：
     *
     * NORMAL
     * DYE
     * EMISSIVE
     */
    public final List<ArmorTextureLayer> layers =
            new ArrayList<>();

    /**
     * Pattern Preset 图案列表。
     *
     * 例如：
     *
     * 1
     * 2
     */
    public final List<PatternPreset> patternPresets =
            new ArrayList<>();

    public GeoArmorConfig(
            String id,
            ResourceLocation model,
            ResourceLocation texture,
            ResourceLocation animation,
            boolean dyeable,
            int defaultColor
    ) {

        this.id = id;

        this.model = model;

        this.texture = texture;

        this.animation = animation;

        this.dyeable = dyeable;

        this.defaultColor = defaultColor;
    }

    // =========================================================
    // Armor Texture Layers
    // =========================================================

    public GeoArmorConfig addLayer(
            ArmorTextureLayer layer
    ) {

        if (layer != null) {
            layers.add(layer);
        }

        return this;
    }

    // =========================================================
    // Pattern Presets
    // =========================================================

    /**
     * 添加一个 PatternPreset。
     */
    public GeoArmorConfig addPattern(
            PatternPreset pattern
    ) {

        if (pattern != null) {
            patternPresets.add(pattern);
        }

        return this;
    }

    /**
     * 判断指定 ID 的图案是否存在。
     *
     * 例如：
     *
     * hasPattern("1")
     * hasPattern("2")
     */
    public boolean hasPattern(
            String patternId
    ) {

        if (patternId == null ||
                patternId.isEmpty()) {

            return false;
        }

        for (PatternPreset pattern :
                patternPresets) {

            if (pattern.id.equals(patternId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 根据 ID 获取 PatternPreset。
     *
     * 找不到时返回 null。
     */
    public PatternPreset getPattern(
            String patternId
    ) {

        if (patternId == null ||
                patternId.isEmpty()) {

            return null;
        }

        for (PatternPreset pattern :
                patternPresets) {

            if (pattern.id.equals(patternId)) {
                return pattern;
            }
        }

        return null;
    }
}