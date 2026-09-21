package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.QuestItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 任务物品 - 击杀计数结算。
 *
 * 玩家击杀生物时，遍历所有已注册的任务物品（{@link QuestItem}），
 * 命中的击杀任务进度 +1（持久 NBT，跨死亡/重登录），并经动作栏反馈进度
 * （{@code 任务进度:击杀僵尸 ×10(3/10)}）。
 * 完成判定与材料解锁在 {@link QuestItem} 的周期评估中完成，此处只累计。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class QuestEvent {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        for (QuestItem quest : QuestItem.getRegistered()) {
            String questId = quest.questId();
            for (QuestItem.Task task : quest.getTasks()) {
                if (task instanceof QuestItem.KillTask kill && kill.matchesKill(victim)) {
                    int now = QuestItem.addKillProgress(player, questId, task.id());
                    player.displayClientMessage(kill.progressText(now, questId), true);
                }
            }
        }
    }
}
