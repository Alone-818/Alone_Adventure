package Alone818.com.alone_adventure.entity.faction;

import Alone818.com.alone_adventure.faction.AttackType;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionEnemyTargetGoal;
import Alone818.com.alone_adventure.faction.PromotionTier;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 派系法师：辅助职业。
 *
 * 升级路线：法师 -> 大法师 -> 魔导师。
 *
 * 军衔技能（治疗行为见 AllyHealGoal）：
 *
 * 法师：治疗间隔较长（10 秒）。
 * 大法师：治疗间隔缩短（7.5 秒），
 *         且有 35% 概率改为群体治疗——
 *         范围内全部受伤队友各回复少量生命。
 * 魔导师：治疗间隔最短（5 秒），保留群体治疗，
 *         且治疗时额外给予队友速度 II 与力量 I（8 秒）。
 *
 * 主要职责是辅助同派系生物：
 * 周期性治疗附近血量不满的同派系队友，
 * 没有伤员时才发射法弹攻击敌人。
 */
public class FactionMage
        extends FactionMobEntity
        implements RangedAttackMob {

    /** 法弹基础伤害（基础兵 1 级），随倍率成长 */
    public static final float BOLT_BASE_DAMAGE = 1.5F;

    public FactionMage(
            EntityType<? extends Monster> type,
            Level level,
            Faction faction,
            PromotionTier tier,
            @Nullable Supplier<EntityType<? extends FactionMobEntity>> nextTierType
    ) {
        super(type, level, faction, tier, nextTierType);
    }

    @Override
    public AttackType getAttackType() {
        return AttackType.RANGED;
    }

    /**
     * 法师基础属性（基础兵 1 级）：
     * 生命最低，避免抢前排，
     * 军衔倍率在实体构造时叠上。
     */
    public static AttributeSupplier.Builder createAttributes() {

        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {

        registerCommonGoals();

        /*
         * 治疗队友优先于输出：
         * 有伤员就先治疗。
         */
        this.goalSelector.addGoal(
                2,
                new AllyHealGoal(this)
        );

        this.goalSelector.addGoal(
                3,
                new RangedAttackGoal(
                        this,
                        0.8D,
                        30,
                        14.0F
                )
        );

        // 对玩家的态度由派系定义决定（恶魔/亡灵主动敌对，帝国/部落中立）
        this.targetSelector.addGoal(
                2,
                new FactionEnemyTargetGoal(
                        this,
                        true
                )
        );
    }

    /**
     * 发射法弹。
     *
     * 暂用原版箭弹道，
     * 基础伤害比弓手低，
     * 法师的定位是辅助而不是输出。
     */
    @Override
    public void performRangedAttack(
            LivingEntity target,
            float velocity
    ) {

        Arrow bolt =
                new Arrow(
                        level(),
                        this
                );

        double dx =
                target.getX() - getX();

        double dy =
                target.getY(0.5D)
                        - bolt.getY();

        double dz =
                target.getZ() - getZ();

        double horizontal =
                Math.sqrt(
                        dx * dx + dz * dz
                );

        bolt.shoot(
                dx,
                dy + horizontal * 0.2D,
                dz,
                1.6F,
                6.0F
        );

        bolt.setBaseDamage(
                BOLT_BASE_DAMAGE
                        * getStatMultiplier()
        );

        playSound(
                SoundEvents.WITCH_THROW,
                1.0F,
                1.4F
        );

        level().addFreshEntity(bolt);
    }
}
