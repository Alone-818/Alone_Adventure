package Alone818.com.alone_adventure.entity.faction;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionEnemyTargetGoal;
import Alone818.com.alone_adventure.faction.PromotionTier;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * 派系士兵：近战职业。
 *
 * 升级路线：士兵 -> 中士 -> 将军。
 *
 * 军衔技能：
 *
 * 中士：低血量拿出治疗药水喝下自救，
 *       全场限 1 次（见 SelfHealDrinkGoal）。
 * 将军：喝药 3 次，
 *       且每击杀一名敌人回复部分生命。
 *
 * 手持武器按军衔区分（纯装饰）：
 * 士兵金剑 / 中士铁剑 / 将军钻石剑，
 * 不参与伤害结算，也不掉落。
 */
public class FactionSoldier extends FactionMobEntity {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 将军击杀回血：最大生命百分比 */
    public static final float KILL_HEAL_RATIO = 0.2F;

    /** 军衔手持武器（纯装饰，按 PromotionTier 顺序）：金剑 / 铁剑 / 钻石剑 */
    private static final Item[] RANK_SWORDS = {
            Items.GOLDEN_SWORD,
            Items.IRON_SWORD,
            Items.DIAMOND_SWORD
    };

    /** 剩余喝药次数（军衔技能） */
    private int selfHealUsesLeft;

    public FactionSoldier(
            EntityType<? extends Monster> type,
            Level level,
            Faction faction,
            PromotionTier tier,
            @Nullable Supplier<EntityType<? extends FactionMobEntity>> nextTierType
    ) {
        super(type, level, faction, tier, nextTierType);

        // 出生按军衔补满喝药次数
        selfHealUsesLeft = getMaxSelfHealUses();

        equipRankWeapon();
    }

    /**
     * 军衔武器：士兵金剑 / 中士铁剑 / 将军钻石剑。
     *
     * 纯装饰：近战伤害只来自攻击属性，
     * 武器不参与结算，也不掉落，
     * 只用于区分军衔外观；
     * 晋升换实体后按新军衔重新佩戴。
     */
    private void equipRankWeapon() {

        setItemSlot(
                EquipmentSlot.MAINHAND,
                rankWeapon()
        );

        setDropChance(
                EquipmentSlot.MAINHAND,
                0.0F
        );
    }

    /**
     * 当前军衔的武器。
     */
    private ItemStack rankWeapon() {

        return new ItemStack(
                RANK_SWORDS[getRank().ordinal()]
        );
    }

    /**
     * 士兵基础属性（基础兵 1 级）：
     * 生命 20 / 攻击 3 / 护甲 4，
     * 军衔倍率在实体构造时叠上。
     */
    public static AttributeSupplier.Builder createAttributes() {

        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 6.0D);
    }

    /**
     * 当前军衔的喝药次数上限：
     * 士兵 0 / 中士 1 / 将军 3。
     */
    @Override
    public int getMaxSelfHealUses() {

        if (getRank() == PromotionTier.ADVANCED) {
            return 1;
        }

        if (getRank() == PromotionTier.ELITE) {
            return 3;
        }

        return 0;
    }

    /**
     * 剩余喝药次数。
     */
    @Override
    public int getSelfHealUsesLeft() {
        return selfHealUsesLeft;
    }

    /**
     * 喝一口药，消耗一次机会。
     */
    @Override
    public void consumeSelfHealUse() {

        selfHealUsesLeft =
                Math.max(
                        0,
                        selfHealUsesLeft - 1
                );
    }

    /**
     * 将军技能：击杀敌人回复 20% 最大生命。
     */
    @Override
    public void onEnemyKilled() {

        if (getRank() != PromotionTier.ELITE) {
            return;
        }

        heal(
                getMaxHealth()
                        * KILL_HEAL_RATIO
        );

        if (level() instanceof ServerLevel server) {

            server.sendParticles(
                    ParticleTypes.HEART,
                    getX(),
                    getY() + getBbHeight() * 0.75D,
                    getZ(),
                    5,
                    0.3D,
                    0.2D,
                    0.3D,
                    0.05D
            );
        }
    }

    @Override
    protected void registerGoals() {

        registerCommonGoals();

        /*
         * 低血自救（中士起解锁）：
         * 优先级高于近战，喝药期间停下动作。
         */
        this.goalSelector.addGoal(
                1,
                new SelfHealDrinkGoal(this)
        );

        this.goalSelector.addGoal(
                2,
                new MeleeAttackGoal(
                        this,
                        1.1D,
                        true
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

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {

        super.addAdditionalSaveData(tag);

        tag.putInt(
                "SelfHealUses",
                selfHealUsesLeft
        );
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {

        super.readAdditionalSaveData(tag);

        /*
         * 旧档缺字段时按当前军衔补满。
         */
        selfHealUsesLeft =
                tag.contains("SelfHealUses")
                        ? Mth.clamp(
                        tag.getInt("SelfHealUses"),
                        0,
                        getMaxSelfHealUses()
                )
                        : getMaxSelfHealUses();

        /*
         * 喝药状态不入档：
         * 如果存档正好落在喝药途中，
         * 手上的药水会作为装备被存下来，
         * 这里换回军衔武器。
         */
        if (getItemInHand(InteractionHand.MAIN_HAND)
                .is(Items.POTION)) {

            setItemInHand(
                    InteractionHand.MAIN_HAND,
                    rankWeapon()
            );
        }
    }
}
