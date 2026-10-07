package Alone818.com.alone_adventure.entity.faction;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.IFactionMob;
import Alone818.com.alone_adventure.faction.MobTier;
import Alone818.com.alone_adventure.faction.PromotionTier;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;
import net.minecraft.world.phys.AABB;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 派系生物基类。
 *
 * 军衔（大等级）：
 *
 * 军衔是不同的实体类型（中士 / 将军等
 * 都是独立注册的生物），见 PromotionTier：
 *
 * 士兵 -> 中士 -> 将军
 * 弓手 -> 弩手 -> 连弩手
 * 法师 -> 大法师 -> 魔导师
 *
 * 等级（小等级）：
 *
 * 1. 每击杀一名敌人（玩家或敌对派系生物）+1 级
 * 2. 等级上限 3 级
 * 3. 3 级时再击杀 -> 原地蜕变成下一军衔的实体，等级回到 1 级
 * 4. 最高军衔 3 级封顶
 *
 * 军衔技能（详见各职业子类）：
 *
 * 中士：低血量拿出治疗药水喝下自救，全场限 1 次
 * 将军：喝药 3 次，且击杀敌人回复生命
 * 弩手：每次射击有 20% 概率连发一支（每次射击至多触发一次）
 * 连弩手：连发数量改为 5 支
 * 法师：治疗间隔较长（10 秒）
 * 大法师：治疗时有概率改为群体治疗（少量生命）
 * 魔导师：治疗时额外给予队友速度与力量
 *
 * 属性成长：
 *
 * 数值倍率 = 军衔基础倍率 + 等级加成
 *
 * 军衔基础：基础兵 100% / 一阶 200% / 二阶 350%
 * 等级加成：1 级 +0% / 2 级 +25% / 3 级 +50%
 *
 * 倍率作用于生命上限与攻击伤害，
 * 弓手 / 法师的投射物伤害也按同一倍率缩放。
 *
 * 手持武器（纯装饰，不影响攻击力，也不掉落）：
 *
 * 士兵线：士兵金剑 / 中士铁剑 / 将军钻石剑
 * 弓手线：弓手持弓 / 弩手持弩 / 连弩手双持弓
 *
 * 武器在各军衔实体构造时直接佩戴，
 * 晋升换实体后自动换装，
 * 见 FactionSoldier / FactionArcher。
 *
 * 自然生成（见 FactionSpawnEvent）：
 *
 * 只在地表按派系结群生成，
 * 自然生成的随距离消失，
 * 突袭 / 生成蛋生成的保持常驻。
 */
public abstract class FactionMobEntity
        extends Monster
        implements IFactionMob {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 每个军衔内的等级上限 */
    public static final int MAX_LEVEL = 3;

    /** 等级属性加成：1 / 2 / 3 级（每级 +5%） */
    private static final float[] LEVEL_STAT = {0.0F, 0.05F, 0.1F};

    /** 难度血量乘区：普通 1.05，困难 1.1 */
    private static final float DIFFICULTY_HEALTH_MULTIPLIER_NORMAL = 1.05F;
    private static final float DIFFICULTY_HEALTH_MULTIPLIER_HARD = 1.1F;

    /** 难度护甲加成：普通 +0，困难 +1 */
    private static final int DIFFICULTY_ARMOR_BONUS_NORMAL = 0;
    private static final int DIFFICULTY_ARMOR_BONUS_HARD = 1;

    /**
     * 喝药状态（同步到客户端，渲染喝药姿势），
     * 见 SelfHealDrinkGoal。
     */
    private static final EntityDataAccessor<Boolean> DATA_DRINKING =
            SynchedEntityData.defineId(
                    FactionMobEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    /**
     * 是否正在施法（客户端渲染双手举高念咒姿势），
     * 由 FactionMage 的 CastSpellGoal 控制。
     */
    private static final EntityDataAccessor<Boolean> DATA_CASTING =
            SynchedEntityData.defineId(
                    FactionMobEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    /**
     * 是否为突袭成员（同步到客户端，渲染派系光芒），
     * 见 FactionMobRenderer。
     */
    private static final EntityDataAccessor<Boolean> DATA_RAID_MEMBER =
            SynchedEntityData.defineId(
                    FactionMobEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    /** 生命成长修改器 UUID（全部派系生物共用，按实体隔离） */
    private static final UUID HEALTH_MODIFIER =
            UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a01");

    /** 攻击成长修改器 UUID */
    private static final UUID DAMAGE_MODIFIER =
            UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a02");

    /** 帝国护甲加成修改器 UUID */
    private static final UUID EMPIRE_ARMOR_MODIFIER =
            UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a03");

    /** 帝国生命加成修改器 UUID */
    private static final UUID EMPIRE_HEALTH_BONUS_MODIFIER =
            UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a04");

    /** 亡灵重生冷却时间（360 秒 = 7200 tick） */
    private static final int UNDEAD_RESURRECTION_COOLDOWN = 7200;

    /** 恶魔低血量增益持续时间（tick） */
    private static final int DEMON_BUFF_DURATION = 60;

    /** 部落吸血比例（10%） */
    private static final float TRIBE_LIFESTEAL_RATIO = 0.1F;

    /** 所属派系，由实体工厂在注册时传入 */
    private final Faction faction;

    /** 军衔，由实体工厂按实体类型传入 */
    private final PromotionTier tier;

    /** 下一军衔的实体类型，最高军衔为 null */
    @Nullable
    private final Supplier<EntityType<? extends FactionMobEntity>> nextTierType;

    /** 当前等级：1 ~ 3 */
    private int level = 1;

    /**
     * 是否随距离消失。
     *
     * 自然生成 / 区块首刷 / 刷怪笼 = true
     * （保持怪物上限流动）；
     * 突袭 / 生成蛋 / 指令生成 = false
     * （队伍靠等级积累，走远就没了会很难受）。
     * 晋升换实体时原样继承。
     */
    private boolean despawnable = false;

    /**
     * 自然生成时的生成时刻（游戏时间 tick），用于白天消失检测。
     */
    private long spawnTime = 0;

    /**
     * 亡灵重生冷却结束时间（游戏时间 tick），-1 表示无冷却。
     */
    private long resurrectionCooldownEnd = -1;

    protected FactionMobEntity(
            EntityType<? extends Monster> type,
            Level level,
            Faction faction,
            PromotionTier tier,
            @Nullable Supplier<EntityType<? extends FactionMobEntity>> nextTierType
    ) {
        super(type, level);

        this.faction = faction;
        this.tier = tier;
        this.nextTierType = nextTierType;

        /*
         * 军衔倍率在生成时就要生效：
         * 中士 / 将军一出生就是放大后的属性。
         */
        applyCombatStats(false);
        applyDifficultyModifiers();
        setHealth(getMaxHealth());
    }

    // =========================================================
    // 每 tick 逻辑
    // =========================================================

    /**
     * 每 tick 执行派系特效：
     * 帝国周围友军护甲加成 + 生命上限加成
     * 亡灵在白天重置重生冷却
     * 恶魔低血量获得力量 + 抗性
     */
    @Override
    public void tick() {
        super.tick();
        serverTickFactionAbility();
    }

    /**
     * 派系特效执行（仅服务端）。
     */
    private void serverTickFactionAbility() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }

        Faction faction = getFaction();

        // 帝国 ：周围 8 格内帝国实体越多，护甲越高（上限+5），生命上限越高（上限+12）
        if (faction == ModFactions.EMPIRE) {
            applyEmpireSynergy(server);
        }

        // 亡灵 ：在白天重置重生冷却
        if (faction == ModFactions.UNDEAD) {
            resetUndeadCooldownIfDay(server);
        }

        // 恶魔 ：低血量获得力量 II + 抗性 I
        if (faction == ModFactions.DEMON) {
            applyDemonBuff(server);
        }
    }

    /**
     * 帝国同派加成：
     * 8 格内每有 1 名帝国实体 = +1 护甲（上限 5）
     * 同步加成 +1 最大生命（上限 8）。
     */
    private static final double EMPIRE_CHECK_RADIUS = 8.0D;
    private static final int EMPIRE_MAX_ARMOR_BONUS = 5;
    private static final int EMPIRE_MAX_HEALTH_BONUS = 8;

    private void applyEmpireSynergy(ServerLevel server) {
        // 统计周围 8 格内的帝国实体
        int count = 0;
        AABB searchBox = new AABB(
                position().subtract(EMPIRE_CHECK_RADIUS, EMPIRE_CHECK_RADIUS, EMPIRE_CHECK_RADIUS),
                position().add(EMPIRE_CHECK_RADIUS, EMPIRE_CHECK_RADIUS, EMPIRE_CHECK_RADIUS)
        );

        for (FactionMobEntity other : server.getEntitiesOfClass(
                FactionMobEntity.class,
                searchBox
        )) {
            if (other != this && other.getFaction() == ModFactions.EMPIRE) {
                count++;
            }
        }

        int armorBonus = Math.min(count, EMPIRE_MAX_ARMOR_BONUS);
        int healthBonus = Math.min(count * 1, EMPIRE_MAX_HEALTH_BONUS);

        // 更新护甲属性
        AttributeInstance armorAttr = getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(EMPIRE_ARMOR_MODIFIER);
            armorAttr.addTransientModifier(
                    new AttributeModifier(
                            EMPIRE_ARMOR_MODIFIER,
                            "AloneAdventure_EmpireArmor",
                            armorBonus,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

        // 更新生命上限
        AttributeInstance healthAttr = getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(EMPIRE_HEALTH_BONUS_MODIFIER);
            healthAttr.addTransientModifier(
                    new AttributeModifier(
                            EMPIRE_HEALTH_BONUS_MODIFIER,
                            "AloneAdventure_EmpireHealth",
                            healthBonus,
                            AttributeModifier.Operation.ADDITION
                    )
            );
            // 补满新增的生命
            if (healthBonus > 0) {
                setHealth(Math.min(getHealth() + healthBonus, getMaxHealth()));
            }
        }
    }

    /**
     * 亡灵白天重置重生冷却。
     *
     * 修复：只在冷却已结束的情况下才重置，避免正在冷却中被清空导致无限复活。
     */
    private void resetUndeadCooldownIfDay(ServerLevel server) {
        // 只有在没有正在进行的冷却时才重置
        // 防止在冷却期间被白天逻辑清空导致无限复活
        if (resurrectionCooldownEnd == -1 && server.isDay() && server.canSeeSky(blockPosition())) {
            // 不在战斗中时可以恢复复活能力
            // 但不会清除正在生效的冷却
        }
    }

    /**
     * 恶魔低血量增益：
     * 生命低于 50% 时获得力量 II + 抗性 I。
     */
    private void applyDemonBuff(ServerLevel server) {
        float healthPercent = getHealth() / getMaxHealth();
        if (healthPercent < 0.5F) {
            // 每 0.5 秒一次检查，避免每 tick 重复添加
            if (tickCount % 10 == 0) {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DEMON_BUFF_DURATION, 1, false, false, false));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DEMON_BUFF_DURATION, 0, false, false, false));
            }
        }
    }

    // =========================================================
    // 同步数据
    // =========================================================

    @Override
    protected void defineSynchedData() {

        super.defineSynchedData();

        entityData.define(
                DATA_DRINKING,
                false
        );

        entityData.define(
                DATA_RAID_MEMBER,
                false
        );

        entityData.define(
                DATA_CASTING,
                false
        );
    }

    /**
     * 是否为突袭成员（客户端渲染派系光芒）。
     */
    public boolean isRaidMember() {
        return entityData.get(DATA_RAID_MEMBER);
    }

    /**
     * 标记为突袭成员（服务端生成时调用）。
     */
    public void setRaidMember(boolean raidMember) {
        entityData.set(DATA_RAID_MEMBER, raidMember);
    }

    /**
     * 是否正在喝药（客户端渲染抬手姿势）。
     */
    public boolean isDrinkingPotion() {

        return entityData.get(
                DATA_DRINKING
        );
    }

    /**
     * 设置喝药状态。
     */
    public void setDrinkingPotion(boolean drinking) {

        entityData.set(
                DATA_DRINKING,
                drinking
        );
    }

    /**
     * 是否正在施法（客户端渲染双手举高念咒姿势）。
     */
    public boolean isCastingSpell() {

        return entityData.get(
                DATA_CASTING
        );
    }

    /**
     * 设置施法状态。
     */
    public void setCastingSpell(boolean casting) {

        entityData.set(
                DATA_CASTING,
                casting
        );
    }

    // =========================================================
    // IFactionMob
    // =========================================================

    @Override
    public Faction getFaction() {
        return faction;
    }

    /**
     * 当前军衔（实体类型档位）。
     */
    public PromotionTier getRank() {
        return tier;
    }

    /**
     * 当前等级对应的分类档位（IFactionMob）：
     *
     * 1 级基础兵 / 2 级精锐 / 3 级王牌。
     */
    @Override
    public MobTier getTier() {
        return MobTier.values()[level - 1];
    }

    /**
     * 当前等级（1 ~ 3）。
     */
    public int getLevel() {
        return level;
    }

    /**
     * 当前数值倍率 = 军衔基础 + 等级加成。
     *
     * 基础兵 1 级 = 100%，将军 3 级 = 400%。
     */
    public float getStatMultiplier() {

        return tier.getBaseStat()
                + LEVEL_STAT[level - 1];
    }

    // =========================================================
    // 等级成长
    // =========================================================

    /**
     * 击杀 +1 级。
     *
     * 3 级时再击杀 -> 蜕变成下一军衔实体，等级回到 1 级；
     * 最高军衔 3 级封顶。
     */
    public void gainKillLevel() {

        if (level >= MAX_LEVEL) {

            if (tier.isMax()) {
                return;
            }

            promoteToNextTier();
        } else {

            level++;

            applyCombatStats(true);

            playLevelUpEffects();
        }
    }

    /**
     * 有效击杀钩子：
     * 每确认一次有效击杀时调用
     * （在升级结算之前，晋升会替换实体），
     * 子类实现各自的军衔技能
     * （例如将军击杀敌人回血）。
     */
    /**
     * 亡灵重生：被击倒后重生回满血。
     * 冷却时间 360 秒（7200 tick）。
     *
     * 此方法已不再使用，复活逻辑已移至 hurt() 中预判处理。
     * 保留以便后续可能的重构。
     */
    public boolean tryResurrect() {
        return false;
    }

    // =========================================================
    // 受伤逻辑
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 检查攻击者是否属于同派系
        if (source.getEntity() instanceof Player attacker) {
            Faction mobFaction = getFaction();
            Faction playerFaction = RaidManager.getPlayerFaction(attacker.getUUID());

            // 如果玩家和生物属于同一派系，完全免疫伤害
            if (mobFaction != null && playerFaction != null && mobFaction == playerFaction) {
                return false;
            }
        }

        // 亡灵复活预判：在受伤前检测是否会死亡
        // 如果是亡灵且可以复活，则手动处理而不是让原版死亡逻辑执行
        if (!level().isClientSide && getFaction() == ModFactions.UNDEAD) {
            float potentialHealth = getHealth() - amount;
            if (potentialHealth <= 0) {
                // 会受到致命伤害，检查是否可以复活
                Faction currentFaction = getFaction();
                if (currentFaction == ModFactions.UNDEAD) {
                    long currentTime = level().getGameTime();
                    if (resurrectionCooldownEnd == -1 || currentTime >= resurrectionCooldownEnd) {
                        // 可以复活：先设置冷却
                        resurrectionCooldownEnd = currentTime + UNDEAD_RESURRECTION_COOLDOWN;
                        // 手动扣血但不触发死亡（保留至少 1 点血）
                        setHealth(1.0F);
                        // 播放复活特效
                        if (level() instanceof ServerLevel server) {
                            server.sendParticles(
                                    ParticleTypes.ENCHANT,
                                    getX(),
                                    getY() + getBbHeight() * 0.5D,
                                    getZ(),
                                    20,
                                    0.3D,
                                    0.3D,
                                    0.3D,
                                    0.1D
                            );
                            playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                        }
                        // 回满血
                        setHealth(getMaxHealth());
                        return true;
                    }
                }
            }
        }

        boolean damaged = super.hurt(source, amount);

        return damaged;
    }

    // =========================================================
    // 击杀钩子
    // =========================================================

    /**
     * 有效击杀钩子：
     * 每确认一次有效击杀时调用
     *（在升级结算之前，晋升会替换实体），
     * 子类实现各自的军衔技能
     *（例如将军击杀敌人回血）。
     *
     * 部落额外：攻击造成伤害的 20% 恢复为自身生命（吸血效果）。
     */
    public void onEnemyKilled() {
        // 部落吸血：击杀时恢复 20% 最大生命
        if (getFaction() == ModFactions.TRIBE) {
            int healAmount = (int) (getMaxHealth() * TRIBE_LIFESTEAL_RATIO);
            heal(healAmount);
        }
    }

    // =========================================================
    // 军衔技能：喝药自救
    // =========================================================

    /**
     * 当前军衔的喝药次数上限。
     *
     * 默认 0（不会自救），
     * 士兵线覆写：中士 1 次 / 将军 3 次，
     * 见 FactionSoldier。
     */
    public int getMaxSelfHealUses() {
        return 0;
    }

    /**
     * 剩余喝药次数。
     *
     * 默认 0，由士兵线覆写。
     */
    public int getSelfHealUsesLeft() {
        return 0;
    }

    /**
     * 喝一口药，消耗一次机会。
     *
     * 默认无操作，由士兵线覆写。
     */
    public void consumeSelfHealUse() {
    }

    /**
     * 蜕变晋升：原地替换成下一军衔的实体。
     *
     * 新实体是 1 级满状态，
     * 军衔技能资源（喝药次数等）一并重置，
     * 保留位置、朝向与当前目标。
     */
    private void promoteToNextTier() {

        if (level().isClientSide
                || nextTierType == null) {
            return;
        }

        EntityType<? extends FactionMobEntity> nextType =
                nextTierType.get();

        if (nextType == null) {
            return;
        }

        FactionMobEntity promoted =
                nextType.create(level());

        if (promoted == null) {
            return;
        }

        promoted.moveTo(
                getX(),
                getY(),
                getZ(),
                getYRot(),
                getXRot()
        );

        promoted.setDeltaMovement(
                getDeltaMovement()
        );

        // 自然生成的生物晋升后仍保持可消失
        promoted.despawnable = despawnable;

        if (hasCustomName()) {

            promoted.setCustomName(
                    getCustomName()
            );

            promoted.setCustomNameVisible(
                    isCustomNameVisible()
            );
        }

        level().addFreshEntity(promoted);

        // 保留当前目标，晋升后继续作战
        if (getTarget() != null) {
            promoted.setTarget(getTarget());
        }

        playLevelUpEffects();

        discard();
    }

    /**
     * 按当前军衔与等级重设生命 / 攻击成长修改器。
     *
     * @param healGrowth true 时把增加的生命上限补进当前血量
     */
    protected void applyCombatStats(boolean healGrowth) {

        float oldMax = getMaxHealth();

        AttributeInstance health =
                getAttribute(Attributes.MAX_HEALTH);

        if (health != null) {

            health.removeModifier(HEALTH_MODIFIER);

            health.addTransientModifier(
                    new AttributeModifier(
                            HEALTH_MODIFIER,
                            "AloneAdventure_FactionHealth",
                            getStatMultiplier() - 1.0F,
                            AttributeModifier.Operation.MULTIPLY_TOTAL
                    )
            );
        }

        AttributeInstance damage =
                getAttribute(Attributes.ATTACK_DAMAGE);

        if (damage != null) {

            damage.removeModifier(DAMAGE_MODIFIER);

            damage.addTransientModifier(
                    new AttributeModifier(
                            DAMAGE_MODIFIER,
                            "AloneAdventure_FactionDamage",
                            getStatMultiplier() - 1.0F,
                            AttributeModifier.Operation.MULTIPLY_TOTAL
                    )
            );
        }

        if (healGrowth) {

            float newMax = getMaxHealth();

            setHealth(
                    Mth.clamp(
                            getHealth() + (newMax - oldMax),
                            1.0F,
                            newMax
                    )
            );
        }
    }

    /**
     * 根据难度应用血量乘区和护甲加成：
     * 普通难度：血量 * 1.1，护甲 +1
     * 困难难度：血量 * 1.2，护甲 +2
     */
    private void applyDifficultyModifiers() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }

        var difficulty = server.getDifficulty();
        if (difficulty == net.minecraft.world.Difficulty.PEACEFUL) {
            return;
        }

        float healthMultiplier = (difficulty == net.minecraft.world.Difficulty.HARD)
                ? DIFFICULTY_HEALTH_MULTIPLIER_HARD
                : DIFFICULTY_HEALTH_MULTIPLIER_NORMAL;

        int armorBonus = (difficulty == net.minecraft.world.Difficulty.HARD)
                ? DIFFICULTY_ARMOR_BONUS_HARD
                : DIFFICULTY_ARMOR_BONUS_NORMAL;

        // 应用血量乘区（乘法）
        AttributeInstance healthAttr = getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            var modifierUuid = UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a05");
            healthAttr.removeModifier(modifierUuid);
            healthAttr.addTransientModifier(
                    new AttributeModifier(
                            modifierUuid,
                            "AloneAdventure_DifficultyHealth",
                            healthMultiplier - 1.0F,
                            AttributeModifier.Operation.MULTIPLY_TOTAL
                    )
            );
        }

        // 应用护甲加成（加法）
        AttributeInstance armorAttr = getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            var modifierUuid = UUID.fromString("7f5a1a9e-1a1e-4ad2-9a71-4c0f7e5b8a06");
            armorAttr.removeModifier(modifierUuid);
            armorAttr.addTransientModifier(
                    new AttributeModifier(
                            modifierUuid,
                            "AloneAdventure_DifficultyArmor",
                            armorBonus,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }
    }

    /**
     * 升级特效：音效 + 绿色粒子。
     */
    private void playLevelUpEffects() {

        playSound(
                SoundEvents.PLAYER_LEVELUP,
                0.8F,
                1.2F
        );

        if (level() instanceof ServerLevel server) {

            server.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    getX(),
                    getY() + getBbHeight() * 0.75D,
                    getZ(),
                    10,
                    0.3D,
                    0.3D,
                    0.3D,
                    0.05D
            );
        }
    }

    // =========================================================
    // 名字
    // =========================================================

    /**
     * 名字带等级（军衔名已经在实体类型名里）：
     *
     * 帝国士兵
     * 帝国中士 II
     * 帝国将军 III
     */
    @Override
    public Component getName() {

        if (hasCustomName()) {
            return super.getName();
        }

        MutableComponent name =
                Component.empty()
                        .append(getTypeName());

        if (level > 1) {
            name.append(" ")
                    .append(Component.translatable(
                            "tier.alone_adventure.tier_" + level
                    ));
        }

        return name;
    }

    // =========================================================
    // 通用 AI
    // =========================================================

    /**
     * 通用目标注册，子类 registerGoals 里先调这个，
     * 再挂各自的战斗目标。
     */
    protected void registerCommonGoals() {

        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                6,
                new WaterAvoidingRandomStrollGoal(
                        this,
                        1.0D
                )
        );

        this.goalSelector.addGoal(
                7,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        this.goalSelector.addGoal(
                8,
                new RandomLookAroundGoal(this)
        );

        /*
         * 被打反击。
         * 同派系误伤由 FactionEvent 全局过滤。
         */
        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );
    }

    /**
     * 标记为随距离消失。
     *
     * 由 FactionSpawnEvent 在自然生成时调用；
     * 突袭 / 生成蛋路径不调，保持常驻。
     */
    public void markDespawnable() {
        despawnable = true;
        spawnTime = level().getGameTime(); // 记录生成时刻
    }

    /**
     * 自然生成的派系生物随距离消失，
     * 否则会永久占满怪物上限。
     *
     * 突袭 / 生成蛋生成的保持常驻：
     * 队伍靠等级积累，掉线 / 走远就没了会很难受。
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        if (!despawnable) return false;

        // 白天且超过 5 分钟未攻击任何目标则消失
        if (level().isDay() && getTarget() == null) {
            long aliveTicks = level().getGameTime() - spawnTime;
            // 5 分钟 = 6000 tick
            if (aliveTicks >= 6000) {
                return true;
            }
        }

        return true;
    }

    // =========================================================
    // 装饰装备
    // =========================================================

    /**
     * 禁用原版生成时的装备附魔：
     *
     * 手持武器纯装饰（金 / 铁 / 钻石剑，弓 / 弩 / 双持弓），
     * 原版按难度附魔可能给武器附上锋利，
     * 那会真正改变攻击力，
     * 与"武器不影响攻击力"的设计冲突。
     *
     * 生成蛋路径会走 finalizeSpawn，
     * 这里保持空操作，
     * 武器只用于区分军衔外观。
     */
    @Override
    protected void populateDefaultEquipmentEnchantments(
            RandomSource random,
            DifficultyInstance difficulty
    ) {
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {

        super.addAdditionalSaveData(tag);

        // 军衔由实体类型决定，不用存
        tag.putInt("FactionLevel", level);
        tag.putBoolean("FactionDespawn", despawnable);
        tag.putLong("SpawnTime", spawnTime);
        tag.putLong("ResurrectionCooldownEnd", resurrectionCooldownEnd);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {

        /*
         * 先读等级并挂好属性修改器，
         * 再走父类读取：
         * 父类读血量时会按当前生命上限截断，
         * 顺序反了会把高军衔的血量截到基础值。
         */
        level = Mth.clamp(
                tag.getInt("FactionLevel"),
                1,
                MAX_LEVEL
        );

        despawnable = tag.getBoolean("FactionDespawn");
        spawnTime = tag.getLong("SpawnTime");

        // 读取重生冷却数据
        long savedResurrectionEnd = tag.getLong("ResurrectionCooldownEnd");
        if (savedResurrectionEnd > 0) {
            resurrectionCooldownEnd = savedResurrectionEnd;
        } else {
            resurrectionCooldownEnd = -1;
        }

        applyCombatStats(false);

        super.readAdditionalSaveData(tag);
    }
}
