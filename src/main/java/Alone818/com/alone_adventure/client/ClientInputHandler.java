package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.network.EagleSkillPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入监听 —— 按键触发饰品主动技能。
 *
 * {@link ModClientSetup#EAGLE_SKILL_KEY} 按下时（游戏内、无 GUI 遮挡），
 * 发送 {@link EagleSkillPacket} 到服务端由帝国天鹰验证佩戴与冷却后生效。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {

    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // consumeClick 只在"本次 tick 刚按下"时返回 true，同时清掉按下次数
        if (ModClientSetup.EAGLE_SKILL_KEY.consumeClick()) {
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                Alone_adventure.NETWORK.sendToServer(new EagleSkillPacket());
            }
        }
    }
}
