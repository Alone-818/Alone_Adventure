package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 枪械 GeckoLib 渲染器。
 *
 * 注意：这里不再保存/读取 currentStack 作为动画状态。
 * reload/fire 动画全部由 GunItemAnimation 的客户端事件时间戳驱动。
 */
@OnlyIn(Dist.CLIENT)
public class GunGeoRenderer extends GeoItemRenderer<GunItem> {

    public GunGeoRenderer(GunItem gun) {
        super(new GunGeoModel(BuiltInRegistries.ITEM.getKey(gun).getPath()));
    }

    @Override
    public void renderByItem(ItemStack stack,
                             ItemDisplayContext context,
                             PoseStack poseStack,
                             MultiBufferSource buffer,
                             int packedLight,
                             int packedOverlay) {

        if (getGeoModel() instanceof GunGeoModel model) {
            model.setDualWield(GunItemAnimation.isDualWield(stack));
        }

        if (isLeftArm(context)) {
            // 左手镜像：只影响左手第一/第三人称持枪模型。
            poseStack.pushPose();
            poseStack.scale(1.0F, 1.0F, -1.0F);
            super.renderByItem(
                    stack,
                    context,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay);
            poseStack.popPose();
        } else {
            super.renderByItem(
                    stack,
                    context,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay);
        }
    }

    private static boolean isLeftArm(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    /**
     * Forge 在 Item 构造期间调用 initializeClient，此时物品还没进入注册表。
     * 所以渲染器必须延迟到 getCustomRenderer() 第一次被调用时创建。
     */
    public static IClientItemExtensions extensions(GunItem gun) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GunGeoRenderer(gun);
                }

                return this.renderer;
            }
        };
    }
}
