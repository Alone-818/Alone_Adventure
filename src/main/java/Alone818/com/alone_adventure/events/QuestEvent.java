package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.contract.QuestItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 任务物品 - 击杀计数结算。
 *
 * <b>佩戴判定</b>：仅在玩家佩戴对应契约饰品时才统计击杀进度。
 * 击杀时遍历已注册的任务物品，检查玩家是否佩戴该物品，
 * 若佩戴则对命中的击杀任务累计进度。
 * 进度跨死亡/重登录持久化，动作栏反馈。
 * 完成判定与材料解锁在 {@link QuestItem} 的周期评估中完成。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class QuestEvent {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // 仅统计佩戴契约饰品时的击杀进度
        for (QuestItem quest : QuestItem.getRegistered()) {
            if (!QuestItem.isWearing(player, quest)) continue;

            String questId = quest.questId();
            for (QuestItem.Task task : quest.getTasks()) {
                if (task instanceof QuestItem.KillTask kill && kill.matchesKill(victim)) {
                    // 佩戴的契约饰品堆栈 NBT 中累加击杀进度
                    ItemStack questStack = QuestItem.getPlayerStack(player, quest);
                    if (!questStack.isEmpty()) {
                        int now = kill.addKillProgress(questStack);
                        player.displayClientMessage(kill.progressText(now, questId), true);
                    }
                }
            }
        }
    }
}