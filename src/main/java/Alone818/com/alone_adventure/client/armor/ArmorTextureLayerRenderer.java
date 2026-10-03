package Alone818.com.alone_adventure.client.armor;

import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.ArmorTextureLayer;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;
import Alone818.com.alone_adventure.armor.LayerType;
import Alone818.com.alone_adventure.armor.PatternPreset;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ArmorTextureLayerRenderer
        extends GeoRenderLayer<GeoArmorItem> {

    public ArmorTextureLayerRenderer(
            DynamicGeoArmorRenderer renderer
    ) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            GeoArmorItem animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {

        GeoArmorConfig config =
                animatable.getConfig();

        if (config == null) {
            return;
        }

        ItemStack stack =
                animatable.getCurrentStack();

        // =========================================================
        // Dye Color
        // =========================================================

        float dyeRed = 1.0F;
        float dyeGreen = 1.0F;
        float dyeBlue = 1.0F;

        /*
         * 没有染色：
         *
         * 1.0 / 1.0 / 1.0
         *
         * 这样不会改变 PNG 原本的颜色。
         *
         * 有染色：
         *
         * 使用 ItemStack 当前染料颜色。
         */
        if (!stack.isEmpty()
                && config.dyeable
                && animatable.hasCustomColor(stack)) {

            int color =
                    animatable.getColor(stack);

            dyeRed =
                    ((color >> 16) & 255)
                            / 255.0F;

            dyeGreen =
                    ((color >> 8) & 255)
                            / 255.0F;

            dyeBlue =
                    (color & 255)
                            / 255.0F;
        }

        // =========================================================
        // 普通 ArmorTextureLayer
        // =========================================================

        for (ArmorTextureLayer layer :
                config.layers) {

            RenderType layerRenderType;

            int layerLight =
                    packedLight;

            float layerRed = 1.0F;
            float layerGreen = 1.0F;
            float layerBlue = 1.0F;

            switch (layer.type) {

                // -------------------------------------------------
                // NORMAL
                // -------------------------------------------------

                case NORMAL:

                    layerRenderType =
                            RenderType.armorCutoutNoCull(
                                    layer.texture
                            );

                    int normalColor =
                            layer.color;

                    layerRed =
                            ((normalColor >> 16) & 255)
                                    / 255.0F;

                    layerGreen =
                            ((normalColor >> 8) & 255)
                                    / 255.0F;

                    layerBlue =
                            (normalColor & 255)
                                    / 255.0F;

                    break;

                // -------------------------------------------------
                // DYE
                // -------------------------------------------------

                case DYE:

                    layerRenderType =
                            RenderType.armorCutoutNoCull(
                                    layer.texture
                            );

                    layerRed =
                            dyeRed;

                    layerGreen =
                            dyeGreen;

                    layerBlue =
                            dyeBlue;

                    break;

                // -------------------------------------------------
                // EMISSIVE
                // -------------------------------------------------

                case EMISSIVE:

                    layerRenderType =
                            RenderType.entityTranslucentEmissive(
                                    layer.texture
                            );

                    layerLight =
                            15728880;

                    break;

                default:

                    continue;
            }

            VertexConsumer layerBuffer =
                    bufferSource.getBuffer(
                            layerRenderType
                    );

            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    layerRenderType,
                    layerBuffer,
                    partialTick,
                    layerLight,
                    packedOverlay,
                    layerRed,
                    layerGreen,
                    layerBlue,
                    1.0F
            );
        }

        // =========================================================
        // Pattern Preset
        // =========================================================

        /*
         * Pattern 不再从 ArmorTextureLayer 获取。
         *
         * 当前 ItemStack 保存：
         *
         * alone_adventure:armor_pattern = "1"
         *
         * 或：
         *
         * alone_adventure:armor_pattern = "2"
         *
         * 然后从 GeoArmorConfig 中找到对应的 PatternPreset。
         */
        if (!stack.isEmpty()) {

            PatternPreset pattern =
                    animatable.getPattern(stack);

            if (pattern != null) {

                renderPattern(
                        poseStack,
                        animatable,
                        bakedModel,
                        bufferSource,
                        partialTick,
                        packedLight,
                        packedOverlay,
                        pattern
                );
            }
        }
    }

    // =============================================================
    // Pattern Renderer
    // =============================================================

    private void renderPattern(
            PoseStack poseStack,
            GeoArmorItem animatable,
            BakedGeoModel bakedModel,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight,
            int packedOverlay,
            PatternPreset pattern
    ) {

        if (pattern.texture == null) {
            return;
        }

        RenderType patternRenderType =
                RenderType.armorCutoutNoCull(
                        pattern.texture
                );

        VertexConsumer patternBuffer =
                bufferSource.getBuffer(
                        patternRenderType
                );

        // ---------------------------------------------------------
        // Pattern Color
        // ---------------------------------------------------------

        int color =
                pattern.color;

        float red =
                ((color >> 16) & 255)
                        / 255.0F;

        float green =
                ((color >> 8) & 255)
                        / 255.0F;

        float blue =
                (color & 255)
                        / 255.0F;

        // ---------------------------------------------------------
        // Render Pattern
        // ---------------------------------------------------------

        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                patternRenderType,
                patternBuffer,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                1.0F
        );
    }
}