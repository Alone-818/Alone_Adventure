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
        // 左手镜像
        // =====================================================

        if (isLeftArm(context)) {

            poseStack.pushPose();

            poseStack.scale(
                    1.0F,
                    1.0F,
                    -1.0F
            );

            super.renderByItem(
                    stack,
                    context,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );

            poseStack.popPose();

        } else {

            super.renderByItem(
                    stack,
                    context,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static boolean isLeftArm(
            ItemDisplayContext context
    ) {

        return context
                == ItemDisplayContext.FIRST_PERSON_LEFT_HAND

                || context
                == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
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