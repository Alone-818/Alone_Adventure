package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.network.EagleSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入监听 —— 按键触发饰品主动技能。
 *
 * {@link ModClientSetup#EAGLE_SKILL_KEY} 按下时（游戏内、无 GUI 遮挡），
 * 发送 {@link EagleSkillPacket} 到服务端由帝国天鹰验证佩戴与冷却后生效。
 *
 * 让位规则：主/副手持枪时 R 键优先交给枪械装填（ClientGunHandler），
 * 本监听不触发饰品技能。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {

    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // consumeClick 只在"本次 tick 刚按下"时返回 true，同时清掉按下次数
        if (ModClientSetup.EAGLE_SKILL_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            // 持枪让位：R 键此时是枪械装填
            if (player.getMainHandItem().getItem() instanceof GunItem
                    || player.getOffhandItem().getItem() instanceof GunItem) {
                return;
            }
            Alone_adventure.NETWORK.sendToServer(new EagleSkillPacket());
        }
    }
}
