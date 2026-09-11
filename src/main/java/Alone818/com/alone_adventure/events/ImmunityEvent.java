package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.dragon_power;
import Alone818.com.alone_adventure.Curios.survival_whimper;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.network.ReviveEffectPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 免疫伤害事件 —— 通用的"致命伤害免疫"分发器
 *
 * 多个饰品可注册相似的规则：受到致命伤害 -> 免疫此次伤害 -> 执行各自效果 -> 进入冷却。
 * 冷却以"剩余游戏刻"记录在饰品堆栈的 NBT 中（随 Curios 自动同步与存档，跨登录有效），
 * 只有在饰品被佩戴时冷却才会递减，摘下后暂停，重新佩戴后继续。
 *
 * 监听 LivingDamageEvent 并使用最低优先级（LOWEST）：
 * 让结晶心脏等普通护盾/减免类事件优先处理，只有确实致命的伤害才会走到这里。
 *
 * 新饰品接入方式：在 initRules() 中 registerRule 一条规则即可，
 * 规则提供 饰品物品 + 冷却刻数 + 触发效果回调。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ImmunityEvent {

    /** 通用冷却 NBT 键：剩余冷却刻数（佩戴期间每 10 tick 递减一次） */
    public static final String TAG_COOLDOWN_REMAIN = "ImmunityCooldownRemain";

    /** 一条免疫规则：饰品物品、冷却刻数、免疫成功时执行的效果 */
    public record ImmunityRule(Item item, int cooldownTicks, BiConsumer<ServerPlayer, ItemStack> effect) {}

    private static final List<ImmunityRule> RULES = new ArrayList<>();
    private static boolean initialized = false;

    public static void registerRule(ImmunityRule rule) {
        RULES.add(rule);
    }

    /**
     * 延迟注册规则：注册表对象在模组构造阶段尚未就绪，
     * 首次处理伤害时再取 ModItems.xxx.get()。
     */
    private static void initRules() {
        if (initialized) return;
        initialized = true;

        // 求生渴望：免疫致命伤害并传送回家，冷却 3 分钟
        registerRule(new ImmunityRule(ModItems.SURVIVAL_WHIMPER.get(), survival_whimper.COOLDOWN_TICKS,
                (player, stack) -> survival_whimper.teleportHome(player)));

        // 龙胤之力：免疫致命伤害并原地复活（隐身/无敌/黑暗），冷却 7 分钟
        registerRule(new ImmunityRule(ModItems.DRAGON_POWER.get(), dragon_power.COOLDOWN_TICKS,
                (player, stack) -> dragon_power.reviveInPlace(player)));
    }

    /** 该饰品堆栈当前是否处于免疫冷却中（剩余冷却刻数未耗尽） */
    public static boolean isOnCooldown(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(TAG_COOLDOWN_REMAIN) > 0;
    }

    /** 剩余冷却刻数（0 表示无冷却） */
    public static long getRemainingCooldown(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_COOLDOWN_REMAIN)) return 0;
        return Math.max(0, tag.getInt(TAG_COOLDOWN_REMAIN));
    }

    /**
     * 玩家 tick：每 10 tick 对佩戴中的免疫饰品递减一次冷却。
     * 摘下的饰品找不到对应槽位，冷却自然暂停。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        // 每 10 tick 更新一次，降低写回与同步频率
        if (player.tickCount % 10 != 0) return;

        initRules();
        for (ImmunityRule rule : RULES) {
            if (rule.item() == null) continue;

            CuriosApi.getCuriosHelper().findFirstCurio(player, rule.item()).ifPresent(slotResult -> {
                ItemStack stack = slotResult.stack();
                CompoundTag tag = stack.getTag();
                if (tag == null || !tag.contains(TAG_COOLDOWN_REMAIN)) return;
                int remaining = tag.getInt(TAG_COOLDOWN_REMAIN);
                if (remaining <= 0) {
                    tag.remove(TAG_COOLDOWN_REMAIN);
                    return;
                }

                // 单次更新扣除 10 tick，冷却速率与真实时间一致
                remaining -= 10;
                if (remaining <= 0) {
                    tag.remove(TAG_COOLDOWN_REMAIN);
                } else {
                    tag.putInt(TAG_COOLDOWN_REMAIN, remaining);
                }
                stack.setTag(tag);
                CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                        handler.setEquippedCurio(slotResult.slotContext().identifier(),
                                slotResult.slotContext().index(), stack));
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        initRules();

        LivingEntity entity = event.getEntity();

        // 只对玩家生效，且只在服务端处理（冷却与传送均为服务端逻辑，NBT 由 Curios 同步客户端）
        if (!(entity instanceof ServerPlayer player)) return;

        // 已被其他处理器（如结晶心脏护盾）取消的伤害不再触发免疫
        if (event.isCanceled()) return;

        // /kill（genericKill）和虚空坠落等无视无敌保护的伤害不能被免疫，必须放行
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // 只有致命伤害（伤害值不低于当前生命值）才触发
        float damage = event.getAmount();
        if (damage <= 0 || damage < player.getHealth()) return;

        for (ImmunityRule rule : RULES) {
            if (rule.item() == null) continue;

            Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper().findFirstCurio(player, rule.item());
            if (curioOpt.isEmpty()) continue;

            SlotResult slotResult = curioOpt.get();
            ItemStack stack = slotResult.stack();

            // 冷却中的饰品不响应，继续尝试下一条规则
            if (isOnCooldown(stack)) continue;

            // 免疫此次伤害
            event.setCanceled(true);

            // 播放类似不死图腾的客户端动画（粒子 + 音效 + 物品激活），
            // 展示的是触发复活的饰品本身，发给所有跟踪该玩家的客户端
            ReviveEffectPacket.sendToTracking(player, stack);

            // 写入冷却剩余刻数（tick 更新）并写回真实槽位
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(TAG_COOLDOWN_REMAIN, rule.cooldownTicks());
            stack.setTag(tag);
            String slotTypeId = slotResult.slotContext().identifier();
            int index = slotResult.slotContext().index();
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(slotTypeId, index, stack));

            // 执行该饰品的免疫效果
            rule.effect().accept(player, stack);

            // 一次伤害只由第一个可用的饰品响应
            return;
        }
    }
}
