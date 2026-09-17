package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.binding_bandage;
import Alone818.com.alone_adventure.Curios.broken_mask;
import Alone818.com.alone_adventure.Curios.imperial_eagle;
import Alone818.com.alone_adventure.Curios.sealed_throne;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Map;

/**
 * 主动技能冷却完成提示 —— 覆盖所有绑定按键（默认 R）的主动技能饰品：
 * 帝国天鹰、破损面具、紧缚绷带、封印王座。
 *
 * 绝对时刻冷却类（天鹰/面具/绷带）：NBT 记录技能下次可用时刻 TAG_SKILL_READY_AT，
 * 佩戴中的饰品一旦跨过该时刻即通知玩家，并随即移除记录（既清理数据也防止重复提醒）。
 * 若玩家离线期间冷却走完，重新上线后的第一次 tick 也会补发提醒。
 *
 * 封印王座无传统冷却：激活消耗全部星辉时打上"待就绪"标记，
 * 星辉回复到第一点（技能重新可用）时通知并清除标记。
 *
 * tick 开销优化：
 * - 每 20 tick（1 秒）检查一次，未佩戴任何相关饰品时只是一次纯 NBT 键探测；
 * - 单次获取饰品能力并遍历全部槽位，替代原来每秒 4 次按物品 findFirstCurio 查询
 *   （每次查询内部都要遍历全部槽位）；
 * - 仅在确实跨过就绪时刻时才写回槽位并同步客户端。
 *
 * 提示方式：仅发给佩戴者本人的升级音效 + 动作栏字幕。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class SkillReadyEvent {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        // 节流：每 20 tick（1 秒）检查一次。就绪提醒为瞬时反馈，1 秒内延迟无感知
        if (player.tickCount % 20 != 0) return;

        long now = player.level().getGameTime();

        // 单次获取饰品能力后直接遍历所有槽位，替代按物品逐个 findFirstCurio 查询
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IItemHandlerModifiable inv = entry.getValue().getStacks();
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack stack = inv.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    CompoundTag tag = stack.getTag();
                    if (tag == null) continue;

                    String langKey = checkStack(tag, stack, now);
                    if (langKey != null) {
                        // NBT 修改（移除已到期记录）写回真实槽位，触发 Curios 客户端同步
                        stack.setTag(tag);
                        handler.setEquippedCurio(entry.getKey(), i, stack);
                        notifyReady(player, langKey);
                    }
                }
            }
        });
    }

    /**
     * 检查单个饰品堆栈是否刚跨过"技能就绪"时刻：
     * 是则清除对应 NBT 记录（防止重复提醒）并返回字幕语言键，否则返回 null。
     * 各饰品的就绪键互斥（同一堆栈只可能属于一种饰品），逐键探测即可。
     */
    private static String checkStack(CompoundTag tag, ItemStack stack, long now) {
        if (tag.contains(imperial_eagle.TAG_SKILL_READY_AT)) {
            if (now < tag.getLong(imperial_eagle.TAG_SKILL_READY_AT)) return null;
            tag.remove(imperial_eagle.TAG_SKILL_READY_AT);
            return "subtitle.alone_adventure.skill_ready.imperial_eagle";
        }
        if (tag.contains(broken_mask.TAG_SKILL_READY_AT)) {
            if (now < tag.getLong(broken_mask.TAG_SKILL_READY_AT)) return null;
            tag.remove(broken_mask.TAG_SKILL_READY_AT);
            return "subtitle.alone_adventure.skill_ready.broken_mask";
        }
        if (tag.contains(binding_bandage.TAG_SKILL_READY_AT)) {
            if (now < tag.getLong(binding_bandage.TAG_SKILL_READY_AT)) return null;
            tag.remove(binding_bandage.TAG_SKILL_READY_AT);
            return "subtitle.alone_adventure.skill_ready.binding_bandage";
        }
        // 封印王座：技能消耗后等待第一点星辉回复，即视为技能就绪
        if (tag.getBoolean(sealed_throne.NB_TAG_SKILL_PENDING)
                && sealed_throne.getStars(stack) > 0) {
            tag.remove(sealed_throne.NB_TAG_SKILL_PENDING);
            return "subtitle.alone_adventure.skill_ready.sealed_throne";
        }
        return null;
    }

    /** 升级音效 + 动作栏字幕，仅提醒佩戴者本人 */
    private static void notifyReady(ServerPlayer player, String langKey) {
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(langKey), true);
    }
}
