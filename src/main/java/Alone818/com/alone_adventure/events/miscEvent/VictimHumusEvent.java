package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.trait.victim_humus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 受害者腐殖 - 伤害事件处理
 *
 * 效果：
 * - 攻击时损失的红色生命的 50% 转换为可叠加黄心
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class VictimHumusEvent {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // 检查是否佩戴受害者腐殖
        if (!victim_humus.isWearing(player)) return;

        // 计算会损失的红心（不能超过当前红心）
        float damage = event.getAmount();
        float redHeartLost = Math.min(damage, player.getHealth());

        // 50% 转换为黄心
        float absorptionGain = redHeartLost * 0.5f;

        // 转换为黄心层数（每层 2 点吸收）
        int layersToAdd = (int) (absorptionGain / victim_humus.ABSORPTION_PER_LAYER);

        if (layersToAdd > 0) {
            int currentLayers = victim_humus.getAbsorptionLayers(player);
            int newLayers = Math.min(currentLayers + layersToAdd, victim_humus.MAX_LAYERS);

            // 更新吸收效果
            float totalAbsorption = newLayers * victim_humus.ABSORPTION_PER_LAYER;
            player.setAbsorptionAmount(totalAbsorption);
        }
    }
}
