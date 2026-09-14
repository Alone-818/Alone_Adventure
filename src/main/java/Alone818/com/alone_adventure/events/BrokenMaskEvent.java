package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Random;

/**
 * 破损面具 - 战斗效果
 *
 * 1. 佩戴者攻击命中时，有 20% 概率对目标施加易伤 I（10 秒）
 *    （与痛击之锤共用 {@link VulnerabilityEvent} 的易伤消耗结算）。
 * 2. 目标身上的易伤被伤害消耗时（由 {@link VulnerabilityEvent} 回调），
 *    攻击者（佩戴者）获得力量 I（5 秒）并回复固定数值生命（3 点）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class BrokenMaskEvent {

    // 施加易伤的概率
    public static final float VULNERABILITY_CHANCE = 0.20F;
    // 易伤持续时间：10 秒
    public static final int VULNERABILITY_DURATION_TICKS = 200;
    // 力量持续时间：5 秒
    public static final int STRENGTH_DURATION_TICKS = 100;
    // 消耗易伤时回复的固定生命值 → 改为 10 HP
    public static final float REWARD_HEAL_AMOUNT = 10.0F;
    // 消耗易伤时获得力量的概率 → 改为 50%
    public static final float STRENGTH_CHANCE = 0.50F;

    private static final Random RANDOM = new Random();

    /**
     * 命中时按概率施加易伤 I。
     * 伤害尚未结算，目标本 tick 内即将受到伤害，
     * 因此该次伤害就会消耗易伤并触发力量+回血奖励（第一次命中即有反馈）。
     * 不伤害驯兽（与痛击之锤行为一致）。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;
        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        if (target instanceof TamableAnimal) return;
        if (!isWearing(attacker)) return;
        if (RANDOM.nextFloat() >= VULNERABILITY_CHANCE) return;

        target.addEffect(new MobEffectInstance(ModEffects.VULNERABILITY.get(),
                VULNERABILITY_DURATION_TICKS, 0, false, true));
    }

    /**
     * 易伤被消耗时的奖励回调（由 {@link VulnerabilityEvent} 在移除易伤的同一 tick 调用）。
     * 仅当攻击者佩戴破损面具时生效：50% 概率获得力量 I，并回复固定生命（10 点）。
     * 注意：此处不能再施加易伤，否则会与本次结算形成循环。
     */
    public static void onVulnerabilityConsumed(LivingEntity attacker) {
        if (!(attacker instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!isWearing(player)) return;

        // 50% 概率获得力量 I（5 秒）
        if (RANDOM.nextFloat() < STRENGTH_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION_TICKS, 0,
                    false, true));
        }
        player.heal(REWARD_HEAL_AMOUNT);
    }

    /** 判断玩家是否佩戴破损面具 */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.BROKEN_MASK.get()).isPresent();
    }
}
