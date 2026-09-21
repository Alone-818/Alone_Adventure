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
        super.renderByItem(stack, context, poseStack, buffer, packedLight, packedOverlay);
    }

    /** 供 {@link GunItem#initializeClient} 注册（每枪一个渲染器实例） */
    public static IClientItemExtensions extensions(GunItem gun) {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new GunGeoRenderer(gun);

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
    }
}
