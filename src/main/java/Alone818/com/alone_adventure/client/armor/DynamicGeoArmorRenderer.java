package Alone818.com.alone_adventure.client.armor;

import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;


public class DynamicGeoArmorRenderer
        extends GeoArmorRenderer<GeoArmorItem> {


    public DynamicGeoArmorRenderer() {

        super(new DynamicGeoArmorModel());

        addRenderLayer(
                new ArmorTextureLayerRenderer(this)
        );
    }


    @Override
    public ResourceLocation getTextureLocation(
            GeoArmorItem animatable
    ) {

        GeoArmorConfig config =
                animatable.getConfig();

        if (config == null) {

            return new ResourceLocation(
                    "alone_adventure",
                    "textures/armor/test_base.png"
            );
        }

        return config.texture;
    }


    @Override
    public RenderType getRenderType(
            GeoArmorItem animatable,
            ResourceLocation texture,
            MultiBufferSource bufferSource,
            float partialTick
    ) {

        return RenderType.entityCutoutNoCull(texture);
    }
    @Override
    public void actuallyRender(
            PoseStack poseStack,
            GeoArmorItem animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {

        /*
         * Layer Renderer 的 reRender：
         *
         * NORMAL / DYE / EMISSIVE
         * 已经由 ArmorTextureLayerRenderer
         * 自己控制颜色。
         *
         * 所以这里不要再次修改颜色。
         */
        if (isReRender) {

            super.actuallyRender(
                    poseStack,
                    animatable,
                    model,
                    renderType,
                    bufferSource,
                    buffer,
                    true,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    red,
                    green,
                    blue,
                    alpha
            );

            return;
        }


        /*
         * 默认：
         *
         * 完全使用原始材质颜色。
         *
         * 1 / 1 / 1 不会改变纹理颜色。
         */
        float armorRed = 1.0F;
        float armorGreen = 1.0F;
        float armorBlue = 1.0F;


        ItemStack stack =
                animatable.getCurrentStack();


        /*
         * 只有真正染色的时候，
         * 才对基础材质进行颜色调节。
         */
        if (!stack.isEmpty()) {

            GeoArmorConfig config =
                    animatable.getConfig();

            if (config != null && config.dyeable) {

                /*
                 * 判断 ItemStack 是否真的有染色。
                 */
                if (animatable.hasCustomColor(stack)) {

                    int color =
                            animatable.getColor(stack);

                    armorRed =
                            ((color >> 16) & 255)
                                    / 255.0F;

                    armorGreen =
                            ((color >> 8) & 255)
                                    / 255.0F;

                    armorBlue =
                            (color & 255)
                                    / 255.0F;
                }
            }
        }


        /*
         * 最终渲染基础材质。
         */
        super.actuallyRender(
                poseStack,
                animatable,
                model,
                renderType,
                bufferSource,
                buffer,
                false,
                partialTick,
                packedLight,
                packedOverlay,
                armorRed,
                armorGreen,
                armorBlue,
                alpha
        );
    }
}