package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 实体渲染器注册（客户端模组总线）。
 *
 * 墨制刀刃飞刃沿用原版"投掷物渲染器"：直接渲染堆栈中的物品模型（GROUND 视角），
 * 与雪球/喷溅药水同款，无需自定义模型，scale 1.0、非自发光。
 * 不注册渲染器的话飞弹在客户端不可见，表现为"刀丢了但什么都没飞出去"。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                ModEntities.INK_BLADE.get(),
                ctx -> new ThrownItemRenderer<>(
                        ctx,
                        1.0F,
                        false
                )
        );

        event.registerEntityRenderer(
                ModEntities.SHOCK_DEVICE.get(),
                ctx -> new ThrownItemRenderer<>(
                        ctx,
                        1.0F,
                        false
                )
        );

        event.registerEntityRenderer(
                ModEntities.REGIMENT_BANNER.get(),
                RegimentBannerRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.BULLET.get(),
                ctx -> new ThrownItemRenderer<>(
                        ctx,
                        0.35F,
                        true
                )
        );

        // 弹药箱
        event.registerEntityRenderer(
                ModEntities.AMMO_BOX.get(),
                AmmoBoxRenderer::new
        );
    }
}
