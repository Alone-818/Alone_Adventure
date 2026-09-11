package Alone818.com.alone_adventure.Effects;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 撕裂效果 - 来自 39 个千年后的神秘帝国
 *
 * 持续时间结束时（最后一刻）结算一次：
 * 伤害 = 当前等级 × 0.6，随后等级减半，
 * 并在效果彻底消失后以减半的等级重新挂上，循环结算直到等级归零。
 *
 * 等级代表撕裂的层数。重新挂上的等级结算在
 * {@link Alone818.com.alone_adventure.events.WeaponAttackEvent#onLivingTick} 驱动的
 * {@link #tryReapply} 中完成。
 */
public class LacerationEffect extends MobEffect {

    // 结算伤害比例：伤害 = 等级 × 0.6
    public static final float DAMAGE_PER_LEVEL = 0.6F;

    // 减半后重新挂上的持续时间（tick）
    public static final int REAPPLY_DURATION_TICKS = 30;

    // 减半后待重新挂上的等级（存 amplifier = 等级 - 1，keyed by entity UUID）
    private static final Map<UUID, Integer> pendingReapply = new ConcurrentHashMap<>();

    public LacerationEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A3000);
    }

    /**
     * 只在效果剩余的最后一刻触发一次（结算时机），其余 tick 不结算。
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1;
    }

    /**
     * 结束结算：造成 等级 × 0.6 的伤害，然后等级减半待重挂。
     */
    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        int level = amplifier + 1; // 实际等级 = amplifier + 1

        // 结算伤害：等级 × 0.6（魔法伤害，无视护甲）
        target.hurt(target.damageSources().magic(), level * DAMAGE_PER_LEVEL);

        // 等级减半，记入待重挂表，由 tryReapply 在效果到期消失后应用
        int halvedLevel = level / 2;
        if (halvedLevel >= 1) {
            pendingReapply.put(target.getUUID(), halvedLevel - 1);
        }
    }

    /**
     * 效果已到期消失后，把减半的等级重新挂上（服务器端每 tick 调用）。
     * 若效果仍然存在则保留待重挂记录，等待其完全过期。
     */
    public static void tryReapply(LivingEntity entity) {
        if (entity.hasEffect(ModEffects.LACERATION.get())) return;

        Integer pendingAmplifier = pendingReapply.remove(entity.getUUID());
        if (pendingAmplifier == null || pendingAmplifier < 0 || !entity.isAlive()) return;

        entity.addEffect(new MobEffectInstance(
                ModEffects.LACERATION.get(),
                REAPPLY_DURATION_TICKS,
                pendingAmplifier,
                false, true, true));
    }
}