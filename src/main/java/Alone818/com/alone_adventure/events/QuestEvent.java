package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.contract.QuestItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
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

    /**
     * 玩家维度变更事件：检测坐标轴到达任务和群系到达任务。
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        // 维度变化时重新评估坐标和群系任务
        if (event.getEntity() instanceof ServerPlayer player) {
            checkPositionAndBiomeTasks(player, true);
        }
    }

    /**
     * 玩家复活事件：检测坐标轴到达任务和群系到达任务。
     */
    @SubscribeEvent
    public static void onPlayerRespawned(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        // 重生时重新评估坐标和群系任务
        if (event.getEntity() instanceof ServerPlayer player) {
            checkPositionAndBiomeTasks(player, false);
        }
    }

    /**
     * 玩家移动 tick 事件：周期性检测坐标轴和群系任务。
     * 仅在服务端每 10 tick 检测一次，避免性能开销。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;
        // 每 10 tick 检测一次
        if (player.tickCount % 10 != 0) return;

        checkPositionAndBiomeTasks(player, false);
    }

    /**
     * 检测并更新佩戴饰品中的坐标轴任务和群系任务进度。
     */
    private static void checkPositionAndBiomeTasks(ServerPlayer player, boolean forceCheck) {
        for (QuestItem quest : QuestItem.getRegistered()) {
            if (!QuestItem.isWearing(player, quest)) continue;

            ItemStack questStack = QuestItem.getPlayerStack(player, quest);
            if (questStack.isEmpty()) continue;

            boolean anyProgress = false;
            String questId = quest.questId();

            for (QuestItem.Task task : quest.getTasks()) {
                if (task instanceof QuestItem.PositionTask posTask) {
                    if (posTask.test(player, questId, quest)) {
                        anyProgress = true;
                        player.displayClientMessage(
                                Component.translatable("message.alone_adventure.quest.position_done",
                                        posTask.title(questId)), true);
                    }
                } else if (task instanceof QuestItem.BiomeTask biomeTask) {
                    if (biomeTask.test(player, questId, quest)) {
                        anyProgress = true;
                        player.displayClientMessage(
                                Component.translatable("message.alone_adventure.quest.biome_done",
                                        biomeTask.title(questId)), true);
                    }
                }
            }

            // 如果有坐标或群系任务达成，检查是否全部完成
            if (anyProgress) {
                quest.checkAllDone(player, questStack);
            }
        }
    }
}