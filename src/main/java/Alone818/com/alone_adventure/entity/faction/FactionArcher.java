package Alone818.com.alone_adventure.entity.faction;

import Alone818.com.alone_adventure.faction.AttackType;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionEnemyTargetGoal;
import Alone818.com.alone_adventure.faction.PromotionTier;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 派系弓手：远程职业。
 *
 * 升级路线：弓手 -> 弩手 -> 连弩手。
 *
 * 军衔技能：
 *
 * 弩手：每次射击有 20% 概率连发一支，
 *       每次射击至多触发一次。
 * 连弩手：连发数量改为 5 支，
 *         每隔几 tick 补一支，形成连弩弹幕。
 *
 * 保持距离射箭，
 * 箭伤随等级倍率成长。
 *
 * 手持武器按军衔区分（纯装饰）：
 * 弓手持弓 / 弩手持弩 / 连弩手双持弓，
 * 箭伤与手中武器无关，武器也不掉落。
 */
public class FactionArcher
        extends FactionMobEntity
        implements RangedAttackMob {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 箭基础伤害（基础兵 1 级），随倍率成长 */
    public static final float ARROW_BASE_DAMAGE = 3F;

    /** 连发触发概率（弩手 / 连弩手） */
    public static final float BURST_CHANCE = 0.2F;

    /** 连弩手连发数量 */
    public static final int REPEATER_BURST_COUNT = 5;

    /** 连发间隔（tick） */
    public static final int BURST_INTERVAL = 3;

    /** 连发剩余箭数 */
    private int burstRemaining = 0;

    /** 距下一支连发箭的倒计时 */
    private int burstDelay = 0;

    public FactionArcher(
            EntityType<? extends Monster> type,
            Level level,
            Faction faction,
            PromotionTier tier,
            @Nullable Supplier<EntityType<? extends FactionMobEntity>> nextTierType
    ) {
        super(type, level, faction, tier, nextTierType);

        equipRankWeapon();
    }

    /**
     * 军衔武器（纯装饰）：
     *
     * 弓手持弓，
     * 弩手持弩，
     * 连弩手双持弓——主副手各一张。
     *
     * 箭伤固定按军衔倍率缩放（见 shootArrowAt），
     * 与手中武器无关，武器也不掉落；
     * 晋升换实体后按新军衔重新佩戴。
     */
    private void equipRankWeapon() {

        setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(
                        getRank() == PromotionTier.ADVANCED
                                ? Items.CROSSBOW
                                : Items.BOW
                )
        );

        // 连弩手副手再持一张弓：双持弓
        if (getRank() == PromotionTier.ELITE) {

            setItemSlot(
                    EquipmentSlot.OFFHAND,
                    new ItemStack(Items.BOW)
            );

            setDropChance(
                    EquipmentSlot.OFFHAND,
                    0.0F
            );
        }

        setDropChance(
                EquipmentSlot.MAINHAND,
                0.0F
        );
    }

    @Override
    public AttackType getAttackType() {
        return AttackType.RANGED;
    }

    /**
     * 弓手基础属性（基础兵 1 级）：
     * 生命 20 / 护甲 4 / 移速稍快方便拉开距离，
     * 军衔倍率在实体构造时叠上。
     */
    public static AttributeSupplier.Builder createAttributes() {

        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {

        registerCommonGoals();

        this.goalSelector.addGoal(
                2,
                new RangedAttackGoal(
                        this,
                        1.0D,
                        20,
                        15.0F
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
     * 射箭。
     *
     * 弹道复用原版骷髅的射击数学，
     * 基础伤害按等级倍率缩放。
     *
     * 弩手 / 连弩手每次射击后有概率连发。
     */
    @Override
    public void performRangedAttack(
            LivingEntity target,
            float velocity
    ) {

        shootArrowAt(target);

        /*
         * 弩手：20% 概率连发，每次射击至多触发一次。
         * 连弩手：连发数量改为 5 支。
         */
        if (getRank() != PromotionTier.BASE
                && getRandom().nextFloat()
                < BURST_CHANCE) {

            burstRemaining =
                    getRank() == PromotionTier.ELITE
                            ? REPEATER_BURST_COUNT
                            : 1;

            burstDelay = BURST_INTERVAL;
        }
    }

    /**
     * 连发节奏：
     * 每隔 BURST_INTERVAL tick 朝当前目标补一支箭。
     */
    @Override
    protected void customServerAiStep() {

        super.customServerAiStep();

        if (burstRemaining <= 0) {
            return;
        }

        if (burstDelay > 0) {
            burstDelay--;
            return;
        }

        LivingEntity target = getTarget();

        // 目标没了就收弓
        if (target == null
                || !target.isAlive()) {

            burstRemaining = 0;
            return;
        }

        shootArrowAt(target);

        burstRemaining--;
        burstDelay = BURST_INTERVAL;
    }

    /**
     * 朝目标射一支箭。
     */
    private void shootArrowAt(LivingEntity target) {

        Arrow arrow =
                new Arrow(
                        level(),
                        this
                );

        double dx =
                target.getX() - getX();

        double dy =
                target.getY(0.5D)
                        - arrow.getY();

        double dz =
                target.getZ() - getZ();

        /*
         * 简单的抛物线补偿：
         * 距离越远抬高越多
         */
        double horizontal =
                Math.sqrt(
                        dx * dx + dz * dz
                );

        arrow.shoot(
                dx,
                dy + horizontal * 0.2D,
                dz,
                1.6F,
                (float) (14
                        - level().getDifficulty().getId() * 4)
        );

        arrow.setBaseDamage(
                ARROW_BASE_DAMAGE
                        * getStatMultiplier()
        );

        playSound(
                SoundEvents.SKELETON_SHOOT,
                1.0F,
                1.0F
        );

        level().addFreshEntity(arrow);
    }
}
