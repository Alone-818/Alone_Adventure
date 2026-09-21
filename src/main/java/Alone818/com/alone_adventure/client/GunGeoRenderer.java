package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 枪械 GeckoLib 渲染器 —— 第一/第三人称、GUI 全场景渲染
 * Blockbench（基岩格式）几何模型（{@code geo/gun/<物品id>.geo.json}），
 * 动画由 {@link GunItemAnimation} 行为状态机驱动。
 *
 * {@link #renderByItem} 在进入渲染前登记本帧堆栈（供按堆栈判定装填等状态）
 * 并计算双持举枪姿态（主手切 {@code <物品id>_raised.geo.json} 变体，缺失回退）。
 *
 * <b>左臂镜像</b>：左手持枪（第一/第三人称左臂上下文）时对几何与动画整体做
 * 矢状面镜像（{@code scale(-1,1,1)}），与右臂动作严格对称——动画的侧向分量
 * （Y/Z 旋转、X 位移）在两臂的 display 反向摆放下视觉效果恰好相反，
 * 必须镜像才能对称（原版左手 display 已含 Y/Z 取反，本镜像补齐几何与动画层，
 * 组合后左手渲染 = 右手的精确镜像）。GeckoLib 默认
 * {@code RenderType.entityCutoutNoCull}（双面渲染），负缩放无背面剔除问题。
 */
@OnlyIn(Dist.CLIENT)
public class GunGeoRenderer extends GeoItemRenderer<GunItem> {

    public GunGeoRenderer(GunItem gun) {
        super(new GunGeoModel(BuiltInRegistries.ITEM.getKey(gun).getPath()));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        if (getGeoModel() instanceof GunGeoModel model) {
            model.setDualWield(GunItemAnimation.isDualWield(stack));
        }
        GunItemAnimation.beginRender(stack);
        if (isLeftArm(context)) {
            // 左臂镜像：与右臂动作/姿态对称（GUI、地面、展示框不镜像）
            poseStack.pushPose();
            poseStack.scale(1.0F, 1.0F, -1.0F);
            super.renderByItem(stack, context, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        } else {
            super.renderByItem(stack, context, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    /** 该渲染上下文是否为左臂（第一/第三人称左手）——镜像以对齐右臂动作 */
    private static boolean isLeftArm(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    /**
     * 供 {@link GunItem#initializeClient} 注册（每枪一个渲染器实例）。
     *
     * <b>渲染器必须惰性构造</b>：{@code initializeClient} 在物品<b>构造期</b>被 Forge 调用
     * （{@code Item} 构造器尾部），此刻物品尚未写入注册表——此时按注册名定位资源
     * {@code BuiltInRegistries.ITEM.getKey(gun)} 会兜底返回 {@code minecraft:air}。
     * 推迟到 {@link #getCustomRenderer()} 首次被渲染管线调用（物品已注册）再构造，
     * {@link GunGeoModel} 才能解析到正确的注册名路径。
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
