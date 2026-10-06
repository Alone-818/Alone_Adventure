package Alone818.com.alone_adventure.entity.faction;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.PromotionTier;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * 法师的辅助目标：
 *
 * 周期性寻找附近血量不满的同派系队友，
 * 治疗伤势最重的一个。
 *
 * 军衔差异：
 *
 * 法师（基础兵）：治疗间隔较长（10 秒）。
 * 大法师：治疗间隔缩短（7.5 秒），
 *         且有概率改为群体治疗——
 *         范围内全部受伤队友各回复少量生命。
 * 魔导师：治疗间隔最短（5 秒），
 *         保留群体治疗，
 *         且治疗时额外给予队友速度与力量。
 *
 * 治疗量随法师自身的等级倍率成长。
 */
public class AllyHealGoal extends Goal {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 寻找队友范围 */
    public static final double RANGE = 16.0D;

    /** 法师（基础兵）治疗冷却：10 秒 */
    public static final long COOLDOWN_BASE = 200L;

    /** 大法师治疗冷却：7.5 秒 */
    public static final long COOLDOWN_ADVANCED = 150L;

    /** 魔导师治疗冷却：5 秒 */
    public static final long COOLDOWN_ELITE = 100L;

    /** 单体治疗基础量（1 级基础兵） */
    public static final float HEAL_BASE = 4.0F;

    /** 大法师 / 魔导师群体治疗概率 */
    public static final float GROUP_HEAL_CHANCE = 0.35F;

    /** 群体治疗基础量（每人少量生命） */
    public static final float GROUP_HEAL_BASE = 2.0F;

    /** 魔导师增益时长：8 秒 */
    public static final int BUFF_TICKS = 160;

    private final FactionMobEntity mage;

    /** 下一次可以治疗的时间（gameTime） */
    private long nextHealTime = 0L;

    /** 本轮选中的治疗目标 */
    @Nullable
    private LivingEntity ally;

    public AllyHealGoal(FactionMobEntity mage) {

        this.mage = mage;

        setFlags(
                EnumSet.of(
                        Goal.Flag.MOVE,
                        Goal.Flag.LOOK
                )
        );
    }

    /**
     * 冷却结束且有受伤的队友才激活。
     */
    @Override
    public boolean canUse() {

        if (mage.level().isClientSide) {
            return false;
        }

        if (mage.level().getGameTime()
                < nextHealTime) {
            return false;
        }

        ally = findMostInjuredAlly();

        return ally != null;
    }

    /**
     * 即发即止：start() 里完成治疗。
     */
    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {

        if (ally == null) {
            return;
        }

        Faction mageFaction =
                mage.getFaction();

        if (mageFaction == null
                || FactionManager.getFaction(ally)
                != mageFaction) {

            ally = null;
            return;
        }

        PromotionTier tier =
                mage.getRank();

        /*
         * 大法师起：有概率改为群体治疗。
         */
        boolean groupHeal =
                tier != PromotionTier.BASE
                        && mage.getRandom()
                        .nextFloat()
                        < GROUP_HEAL_CHANCE;

        if (groupHeal) {
            groupHeal(mageFaction);
        } else {
            healAlly(ally);
        }

        nextHealTime =
                mage.level().getGameTime()
                        + cooldownFor(tier);

        mage.playSound(
                SoundEvents.PLAYER_LEVELUP,
                0.6F,
                1.8F
        );

        ally = null;
    }

    /**
     * 当前军衔的治疗冷却。
     */
    private long cooldownFor(PromotionTier tier) {

        if (tier == PromotionTier.ADVANCED) {
            return COOLDOWN_ADVANCED;
        }

        if (tier == PromotionTier.ELITE) {
            return COOLDOWN_ELITE;
        }

        return COOLDOWN_BASE;
    }

    /**
     * 单体治疗：基础量 x 法师当前倍率。
     */
    private void healAlly(LivingEntity target) {

        float amount =
                HEAL_BASE
                        * mage.getStatMultiplier();

        target.heal(amount);

        playHealEffects(target);

        applyMagusBuffs(target);
    }

    /**
     * 群体治疗（大法师 / 魔导师）：
     *
     * 范围内全部受伤的同派系队友
     * 各回复少量生命。
     */
    private void groupHeal(Faction faction) {

        AABB area =
                mage.getBoundingBox()
                        .inflate(RANGE);

        List<LivingEntity> allies =
                mage.level().getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        living ->
                                living != mage
                                        && living.isAlive()
                );

        for (LivingEntity target : allies) {

            if (FactionManager.getFaction(target)
                    != faction) {

                continue;
            }

            // 满血的不占治疗
            if (target.getHealth()
                    >= target.getMaxHealth()) {

                continue;
            }

            float amount =
                    GROUP_HEAL_BASE
                            * mage.getStatMultiplier();

            target.heal(amount);

            playHealEffects(target);

            applyMagusBuffs(target);
        }
    }

    /**
     * 魔导师技能：
     * 治疗时额外给予队友速度 II 与力量 I。
     */
    private void applyMagusBuffs(LivingEntity target) {

        if (mage.getRank() != PromotionTier.ELITE) {
            return;
        }

        target.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        BUFF_TICKS,
                        1
                )
        );

        target.addEffect(
                new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST,
                        BUFF_TICKS,
                        0
                )
        );
    }

    /**
     * 治疗特效：心形粒子。
     */
    private void playHealEffects(LivingEntity target) {

        if (mage.level()
                instanceof ServerLevel server) {

            server.sendParticles(
                    ParticleTypes.HEART,
                    target.getX(),
                    target.getY()
                            + target.getBbHeight() * 0.75D,
                    target.getZ(),
                    4,
                    0.3D,
                    0.2D,
                    0.3D,
                    0.02D
            );
        }
    }

    /**
     * 找范围内伤势最重的同派系队友。
     */
    @Nullable
    private LivingEntity findMostInjuredAlly() {

        Faction faction =
                mage.getFaction();

        if (faction == null) {
            return null;
        }

        AABB area =
                mage.getBoundingBox()
                        .inflate(RANGE);

        List<LivingEntity> candidates =
                mage.level().getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        living ->
                                living != mage
                                        && living.isAlive()
                );

        LivingEntity best = null;
        float bestDeficit = 0.5F;

        for (LivingEntity candidate : candidates) {

            if (FactionManager.getFaction(candidate)
                    != faction) {

                continue;
            }

            float deficit =
                    candidate.getMaxHealth()
                            - candidate.getHealth();

            if (deficit > bestDeficit) {
                best = candidate;
                bestDeficit = deficit;
            }
        }

        return best;
    }
}
