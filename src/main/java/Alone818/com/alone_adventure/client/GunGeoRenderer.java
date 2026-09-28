package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import software.bernie.geckolib.renderer.GeoItemRenderer;

@OnlyIn(Dist.CLIENT)
public class GunGeoRenderer
        extends GeoItemRenderer<GunItem> {

    public GunGeoRenderer(
            GunItem gun
    ) {

        super(
                new GunGeoModel(
                        BuiltInRegistries.ITEM
                                .getKey(gun)
                                .getPath()
                )
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        // =====================================================
        // 双持模型
        // =====================================================

        if (getGeoModel()
                instanceof GunGeoModel model) {

            model.setDualWield(
                    GunItemAnimation.isDualWield(
                            stack
                    )
            );
        }

        // =====================================================
        // GeckoLib 原生 left_hand / right_hand 渲染
        //
        // GeoItemRenderer 内部已根据 context 自动处理：
        //   - left_hand / thirdperson_lefthand / firstperson_lefthand
        //     → 使用模型中的 "left_hand" 部分
        //   - right_hand / thirdperson_righthand / firstperson_righthand
        //     → 使用模型中的 "right_hand" 部分
        //
        // 无需手动 Z 轴翻转，GeckoLib 会自动调用正确的
        // 渲染逻辑（包括对左手模型的镜像处理）。
        // =====================================================

        super.renderByItem(
                stack,
                context,
                poseStack,
                buffer,
                packedLight,
                packedOverlay
        );
    }

    // =========================================================
    // Forge Renderer
    // =========================================================

    public static IClientItemExtensions extensions(
            GunItem gun
    ) {

        return new IClientItemExtensions() {

            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer
            getCustomRenderer() {

                if (this.renderer == null) {

                    this.renderer =
                            new GunGeoRenderer(
                                    gun
                            );
                }

                return this.renderer;
            }
        };
    }
}