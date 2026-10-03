package Alone818.com.alone_adventure.client.armor;

import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.ArmorTextureLayer;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;
import Alone818.com.alone_adventure.armor.LayerType;

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


        /*
         * 获取当前正在渲染的装备
         */
        ItemStack stack =
                animatable.getCurrentStack();


        float dyeRed = 1.0F;
        float dyeGreen = 1.0F;
        float dyeBlue = 1.0F;

        if (!stack.isEmpty()
                && config.dyeable
                && animatable.hasCustomColor(stack)) {

            int color =
                    animatable.getColor(stack);

            dyeRed =
                    ((color >> 16) & 255) / 255.0F;

            dyeGreen =
                    ((color >> 8) & 255) / 255.0F;

            dyeBlue =
                    (color & 255) / 255.0F;
        }
        /*
         * 遍历所有额外纹理层
         */
        for (ArmorTextureLayer layer : config.layers) {

            RenderType layerRenderType;

            int layerLight =
                    packedLight;

            float layerRed = 1.0F;
            float layerGreen = 1.0F;
            float layerBlue = 1.0F;


            switch (layer.type) {

                /*
                 * 普通图案层
                 *
                 * 不跟随玩家染色
                 */
                case NORMAL:

                    layerRenderType =
                            RenderType.armorCutoutNoCull(
                                    layer.texture
                            );

                    int color =
                            layer.color;

                    layerRed =
                            ((color >> 16) & 255)
                                    / 255.0F;

                    layerGreen =
                            ((color >> 8) & 255)
                                    / 255.0F;

                    layerBlue =
                            (color & 255)
                                    / 255.0F;

                    break;


                /*
                 * 染色层
                 *
                 * 跟随装备染色
                 */
                case DYE:

                    layerRenderType =
                            RenderType.armorCutoutNoCull(
                                    layer.texture
                            );

                    layerRed = dyeRed;
                    layerGreen = dyeGreen;
                    layerBlue = dyeBlue;

                    break;


                /*
                 * 发光层
                 */
                case EMISSIVE:

                    layerRenderType =
                            RenderType.entityTranslucentEmissive(
                                    layer.texture
                            );

                    /*
                     * 最大光照
                     */
                    layerLight = 15728880;

                    break;


                default:

                    continue;
            }


            /*
             * 获取这一层自己的 VertexConsumer
             */
            VertexConsumer layerBuffer =
                    bufferSource.getBuffer(
                            layerRenderType
                    );


            /*
             * GeckoLib 重新渲染模型
             *
             * 注意：
             * 你的 GeckoLib 版本的 reRender()
             * 需要使用 13 个参数。
             */
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
    }
}