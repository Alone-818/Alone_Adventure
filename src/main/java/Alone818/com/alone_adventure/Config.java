package Alone818.com.alone_adventure;

import Alone818.com.alone_adventure.Curios.binding_bandage;
import Alone818.com.alone_adventure.Curios.bleedingshield;
import Alone818.com.alone_adventure.Curios.broken_mask;
import Alone818.com.alone_adventure.Curios.charging_core;
import Alone818.com.alone_adventure.Curios.crystalline_heart;
import Alone818.com.alone_adventure.Curios.dragon_power;
import Alone818.com.alone_adventure.Curios.imperial_eagle;
import Alone818.com.alone_adventure.Curios.necromancer_ledger;
import Alone818.com.alone_adventure.Curios.sealed_throne;
import Alone818.com.alone_adventure.Curios.survival_whimper;
import Alone818.com.alone_adventure.Effects.LacerationEffect;
import Alone818.com.alone_adventure.Items.InkBladeProjectile;
import Alone818.com.alone_adventure.Items.chainsawsword;
import Alone818.com.alone_adventure.Items.ink_blade;
import Alone818.com.alone_adventure.Items.machine_claw;
import Alone818.com.alone_adventure.Items.painstrike_hammer;
import Alone818.com.alone_adventure.Items.parryshield;
import Alone818.com.alone_adventure.Items.powersword;
import Alone818.com.alone_adventure.Items.reaper_scythe;
import Alone818.com.alone_adventure.Items.starlight_greatsword;
import Alone818.com.alone_adventure.events.BrokenMaskEvent;
import Alone818.com.alone_adventure.events.NightContractEvent;
import Alone818.com.alone_adventure.events.ShieldEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 饰品与武器数值配置 —— 全部已实现饰品/武器的可调数值集中在 alone_adventure-common.toml。
 *
 * 机制：各饰品/事件类的数值字段保留原常量初始值作为默认（public static 非 final），
 * 配置加载与重载时（ModConfigEvent.Loading / Reloading 均会触发本类）读取 TOML
 * 并回填到对应字段；游戏内逻辑照常读取这些字段，无需感知配置存在。
 *
 * 修改配置文件后重启游戏（或服务端 reload）即生效。
 * 注意：物品 tooltip 中写死的数值描述展示的是默认值，与自定义配置可能不一致。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    /** tick 类数值的通用上限（约 5.8 天） */
    private static final int MAX_TICKS = 10_000_000;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static ForgeConfigSpec.DoubleValue HEART_MAX_HEALTH_PENALTY;
    private static ForgeConfigSpec.IntValue HEART_PER_HEALTH_TO_SHIELD;
    private static ForgeConfigSpec.IntValue HEART_SHIELD_HEAL_INTERVAL_TICKS;
    private static ForgeConfigSpec.IntValue HEART_ARMOR_TO_SHIELD;

    private static ForgeConfigSpec.DoubleValue BLEEDING_ARMOR_REDUCTION_RATIO;
    private static ForgeConfigSpec.DoubleValue BLEEDING_HEALTH_REDUCTION_RATIO;
    private static ForgeConfigSpec.DoubleValue BLEEDING_ARMOR_PER_TOUGHNESS;
    private static ForgeConfigSpec.DoubleValue BLEEDING_HEALTH_PER_TOUGHNESS;

    private static ForgeConfigSpec.IntValue NIGHT_HEAL_INTERVAL_TICKS;
    private static ForgeConfigSpec.DoubleValue NIGHT_BONUS_PER_BRIGHTNESS;
    private static ForgeConfigSpec.DoubleValue NIGHT_NEW_MOON_BASE_BONUS;

    private static ForgeConfigSpec.IntValue WHIMPER_COOLDOWN_TICKS;

    private static ForgeConfigSpec.IntValue DRAGON_COOLDOWN_TICKS;
    private static ForgeConfigSpec.IntValue DRAGON_INVISIBILITY_TICKS;
    private static ForgeConfigSpec.IntValue DRAGON_INVULNERABLE_TICKS;
    private static ForgeConfigSpec.IntValue DRAGON_DARKNESS_TICKS;
    private static ForgeConfigSpec.DoubleValue DRAGON_REVIVE_HEALTH_RATIO;

    private static ForgeConfigSpec.IntValue EAGLE_SKILL_TICKS;
    private static ForgeConfigSpec.IntValue EAGLE_SKILL_COOLDOWN_TICKS;

    private static ForgeConfigSpec.DoubleValue MASK_ARMOR_BONUS;
    private static ForgeConfigSpec.DoubleValue MASK_MAX_HEALTH_BONUS;
    private static ForgeConfigSpec.DoubleValue MASK_TOUGHNESS_BONUS;
    private static ForgeConfigSpec.DoubleValue MASK_SKILL_RADIUS;
    private static ForgeConfigSpec.IntValue MASK_SKILL_VULN_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue MASK_SKILL_RESIST_DURATION_TICKS;
    private static ForgeConfigSpec.DoubleValue MASK_SKILL_HEAL_RATIO;
    private static ForgeConfigSpec.IntValue MASK_SKILL_COOLDOWN_TICKS;
    private static ForgeConfigSpec.DoubleValue MASK_VULNERABILITY_CHANCE;
    private static ForgeConfigSpec.IntValue MASK_VULNERABILITY_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue MASK_STRENGTH_DURATION_TICKS;
    private static ForgeConfigSpec.DoubleValue MASK_REWARD_HEAL_AMOUNT;
    private static ForgeConfigSpec.DoubleValue MASK_STRENGTH_CHANCE;

    private static ForgeConfigSpec.DoubleValue LEDGER_MAX_HEALTH_REDUCTION;
    private static ForgeConfigSpec.DoubleValue LEDGER_ATTACK_DAMAGE_REDUCTION;
    private static ForgeConfigSpec.DoubleValue LEDGER_ABSORPTION_AMOUNT;
    private static ForgeConfigSpec.IntValue LEDGER_ABSORPTION_INTERVAL_TICKS;
    private static ForgeConfigSpec.IntValue LEDGER_CALAMITY_BASE_LEVEL;
    private static ForgeConfigSpec.DoubleValue LEDGER_CALAMITY_DURATION_RATIO;
    private static ForgeConfigSpec.DoubleValue LEDGER_SKILL_STACK_DURATION_MULTIPLIER;
    private static ForgeConfigSpec.DoubleValue LEDGER_LETHAL_DAMAGE;
    private static ForgeConfigSpec.DoubleValue LEDGER_SKILL_RADIUS;
    private static ForgeConfigSpec.IntValue LEDGER_SKILL_LEVEL_BONUS;
    private static ForgeConfigSpec.DoubleValue LEDGER_SKILL_DURATION_RATIO;

    private static ForgeConfigSpec.DoubleValue BANDAGE_ENDURANCE_CHANCE;
    private static ForgeConfigSpec.IntValue BANDAGE_ENDURANCE_DURATION_TICKS;
    private static ForgeConfigSpec.DoubleValue BANDAGE_WEAKNESS_BONUS_PER_LEVEL;
    private static ForgeConfigSpec.IntValue BANDAGE_SHIELD_GAIN;
    private static ForgeConfigSpec.IntValue BANDAGE_SHIELD_MAX;
    private static ForgeConfigSpec.IntValue BANDAGE_SKILL_COOLDOWN_TICKS;

    private static ForgeConfigSpec.IntValue THRONE_MAX_STARS;
    private static ForgeConfigSpec.IntValue THRONE_STAR_REGEN_TICKS;
    private static ForgeConfigSpec.IntValue THRONE_STAR_LOSS_PER_HIT;
    private static ForgeConfigSpec.DoubleValue THRONE_DAMAGE_PER_STAR;
    private static ForgeConfigSpec.IntValue THRONE_STARS_PER_ARMOR;
    private static ForgeConfigSpec.DoubleValue THRONE_ARMOR_PER_GROUP;
    private static ForgeConfigSpec.IntValue THRONE_STARS_PER_SHIELD;
    private static ForgeConfigSpec.IntValue THRONE_SHIELD_MAX;
    private static ForgeConfigSpec.IntValue THRONE_ABSORPTION_DURATION_TICKS;

    private static ForgeConfigSpec.IntValue CORE_SKILL_COOLDOWN_TICKS;
    private static ForgeConfigSpec.IntValue CORE_THUNDER_MAX_LAYERS;
    private static ForgeConfigSpec.IntValue CORE_THUNDER_LAYER_TICKS;
    private static ForgeConfigSpec.IntValue CORE_FROST_MAX_LAYERS;
    private static ForgeConfigSpec.IntValue CORE_FROST_LAYER_TICKS;
    private static ForgeConfigSpec.IntValue CORE_DARK_MAX_LAYERS;
    private static ForgeConfigSpec.IntValue CORE_DARK_LAYER_TICKS;
    private static ForgeConfigSpec.DoubleValue CORE_THUNDER_DAMAGE_PER_LAYER;
    private static ForgeConfigSpec.IntValue CORE_THUNDER_BURST_TICKS;
    private static ForgeConfigSpec.DoubleValue CORE_FROST_ARMOR_PER_LAYER;
    private static ForgeConfigSpec.DoubleValue CORE_FROST_TOUGHNESS_PER_LAYER;
    private static ForgeConfigSpec.IntValue CORE_DARK_DAMAGE_PER_LAYER;

    // ===== 武器 =====
    private static ForgeConfigSpec.IntValue SWORD_ARMOR_PENALTY_THRESHOLD;
    private static ForgeConfigSpec.IntValue SWORD_ARMOR_TOUGHNESS_THRESHOLD;
    private static ForgeConfigSpec.DoubleValue SWORD_ARMOR_PENALTY_MULTIPLIER;
    private static ForgeConfigSpec.DoubleValue SWORD_ARMOR_TOUGHNESS_MULTIPLIER;
    private static ForgeConfigSpec.IntValue SWORD_MAX_PENALTY_DAMAGE;
    private static ForgeConfigSpec.IntValue SWORD_OVERCLOCK_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue SWORD_OVERCLOCK_DURABILITY_COST;
    private static ForgeConfigSpec.IntValue SWORD_OVERCLOCK_COOLDOWN_TICKS;
    private static ForgeConfigSpec.IntValue SWORD_RESISTANCE_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue SWORD_FIRE_DAMAGE;
    private static ForgeConfigSpec.IntValue SWORD_FIRE_BURN_DURATION;
    private static ForgeConfigSpec.DoubleValue SWORD_OVERCLOCK_DAMAGE_MULTIPLIER;
    private static ForgeConfigSpec.DoubleValue SWORD_OVERCLOCK_HEAL_RATIO;

    private static ForgeConfigSpec.IntValue PARRY_WINDOW_TICKS;
    private static ForgeConfigSpec.DoubleValue PARRY_REFLECT_RATIO;
    private static ForgeConfigSpec.IntValue PARRY_WEAKNESS_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue PARRY_WEAKNESS_AMPLIFIER;
    private static ForgeConfigSpec.DoubleValue PARRY_LONG_BLOCK_ABSORB;
    private static ForgeConfigSpec.IntValue PARRY_COOLDOWN_TICKS;

    private static ForgeConfigSpec.IntValue CHAIN_LACERATION_LEVELS_PER_ATTACK;
    private static ForgeConfigSpec.IntValue CHAIN_LACERATION_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue CHAIN_SLASH_INTERVAL_TICKS;
    private static ForgeConfigSpec.DoubleValue CHAIN_SLASH_DAMAGE;
    private static ForgeConfigSpec.IntValue CHAIN_SLASH_LACERATION_LEVELS;
    private static ForgeConfigSpec.DoubleValue CHAIN_SLASH_ARC_DEGREES;
    private static ForgeConfigSpec.DoubleValue CHAIN_SLASH_RANGE;
    private static ForgeConfigSpec.IntValue CHAIN_SLASH_MAX_USE_TICKS;
    private static ForgeConfigSpec.IntValue CHAIN_SLASH_COOLDOWN_TICKS;
    private static ForgeConfigSpec.IntValue CHAIN_MIN_USE_FOR_COOLDOWN_TICKS;
    private static ForgeConfigSpec.DoubleValue CHAIN_LACERATION_DAMAGE_PER_LEVEL;
    private static ForgeConfigSpec.IntValue CHAIN_LACERATION_REAPPLY_DURATION_TICKS;

    private static ForgeConfigSpec.DoubleValue HAMMER_AREA_RADIUS;
    private static ForgeConfigSpec.DoubleValue HAMMER_AREA_DAMAGE_RATIO;
    private static ForgeConfigSpec.IntValue HAMMER_VULNERABILITY_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue HAMMER_VULNERABILITY_LEVEL_STEP;

    private static ForgeConfigSpec.IntValue INK_COOLDOWN_TICKS;
    private static ForgeConfigSpec.IntValue INK_REGEN_INTERVAL_TICKS;
    private static ForgeConfigSpec.DoubleValue INK_THROW_SPEED;
    private static ForgeConfigSpec.DoubleValue INK_DAMAGE;
    private static ForgeConfigSpec.IntValue INK_WEAKNESS_DURATION_TICKS;
    private static ForgeConfigSpec.IntValue INK_WEAKNESS_MAX_LEVEL;

    private static ForgeConfigSpec.IntValue STAR_MAX_CHARGE;
    private static ForgeConfigSpec.IntValue STAR_CHARGE_INTERVAL_TICKS;
    private static ForgeConfigSpec.DoubleValue STAR_DAMAGE_PER_CHARGE;
    private static ForgeConfigSpec.IntValue STAR_ABSORPTION_DURATION_TICKS;
    private static ForgeConfigSpec.DoubleValue STAR_DAMAGE_PER_ABSORPTION_LEVEL;

    private static ForgeConfigSpec.IntValue SCYTHE_DAMAGE_PER_NEW_KILL;

    private static ForgeConfigSpec.IntValue CLAW_COMBO_DAMAGE_PER_HIT;
    private static ForgeConfigSpec.IntValue CLAW_COMBO_TIMEOUT_TICKS;

    static final ForgeConfigSpec SPEC;

    static {
        // ===== 结晶心脏 =====
        BUILDER.push("crystalline_heart");
        HEART_MAX_HEALTH_PENALTY = BUILDER
                .comment("最大生命值削减比例（-0.8 = 减少 80%）")
                .defineInRange("maxHealthPenalty", -0.8, -0.99, 0.0);
        HEART_PER_HEALTH_TO_SHIELD = BUILDER
                .comment("每多少点被削减的最大生命值转化为 1 点护盾上限")
                .defineInRange("perHealthToShield", 4, 1, 1000);
        HEART_SHIELD_HEAL_INTERVAL_TICKS = BUILDER
                .comment("受伤后护盾回复间隔（tick，20 tick = 1 秒）")
                .defineInRange("shieldHealIntervalTicks", 200, 1, MAX_TICKS);
        HEART_ARMOR_TO_SHIELD = BUILDER
                .comment("每多少点护甲提升 1 点护盾上限")
                .defineInRange("armorToShield", 5, 1, 1000);
        BUILDER.pop();

        // ===== 流血护盾 =====
        BUILDER.push("bleedingshield");
        BLEEDING_ARMOR_REDUCTION_RATIO = BUILDER
                .comment("护甲削减比例（0.8 = 减少 80%）")
                .defineInRange("armorReductionRatio", 0.8, 0.0, 0.99);
        BLEEDING_HEALTH_REDUCTION_RATIO = BUILDER
                .comment("最大生命值削减比例（0.2 = 减少 20%）")
                .defineInRange("healthReductionRatio", 0.2, 0.0, 0.99);
        BLEEDING_ARMOR_PER_TOUGHNESS = BUILDER
                .comment("每减少多少点护甲转化为 1 点护甲韧性")
                .defineInRange("armorPerToughness", 4.0, 0.001, 1000.0);
        BLEEDING_HEALTH_PER_TOUGHNESS = BUILDER
                .comment("每减少多少点生命值转化为 1 点护甲韧性")
                .defineInRange("healthPerToughness", 3.0, 0.001, 1000.0);
        BUILDER.pop();

        // ===== 暗夜契约 =====
        BUILDER.push("night_contract");
        NIGHT_HEAL_INTERVAL_TICKS = BUILDER
                .comment("夜晚生命恢复间隔（tick，20 tick = 1 秒）")
                .defineInRange("healIntervalTicks", 80, 1, MAX_TICKS);
        NIGHT_BONUS_PER_BRIGHTNESS = BUILDER
                .comment("每级月相亮度提供的移动速度/暴击伤害加成（0.10 = 10%）")
                .defineInRange("bonusPerBrightness", 0.10, 0.0, 10.0);
        NIGHT_NEW_MOON_BASE_BONUS = BUILDER
                .comment("新月保底加成（0.10 = 10%）")
                .defineInRange("newMoonBaseBonus", 0.10, 0.0, 10.0);
        BUILDER.pop();

        // ===== 求生渴望 =====
        BUILDER.push("survival_whimper");
        WHIMPER_COOLDOWN_TICKS = BUILDER
                .comment("致命伤害免疫冷却（tick，3600 = 3 分钟）")
                .defineInRange("cooldownTicks", 3600, 0, MAX_TICKS);
        BUILDER.pop();

        // ===== 龙胤之力 =====
        BUILDER.push("dragon_power");
        DRAGON_COOLDOWN_TICKS = BUILDER
                .comment("原地复活冷却（tick，8400 = 7 分钟）")
                .defineInRange("cooldownTicks", 8400, 0, MAX_TICKS);
        DRAGON_INVISIBILITY_TICKS = BUILDER
                .comment("复活后隐身持续（tick，140 = 7 秒）")
                .defineInRange("invisibilityTicks", 140, 0, MAX_TICKS);
        DRAGON_INVULNERABLE_TICKS = BUILDER
                .comment("复活后无敌持续（tick，100 = 5 秒）")
                .defineInRange("invulnerableTicks", 100, 0, MAX_TICKS);
        DRAGON_DARKNESS_TICKS = BUILDER
                .comment("复活后黑暗持续（tick，60 = 3 秒）")
                .defineInRange("darknessTicks", 60, 0, MAX_TICKS);
        DRAGON_REVIVE_HEALTH_RATIO = BUILDER
                .comment("复活恢复的生命比例（0.5 = 半血）")
                .defineInRange("reviveHealthRatio", 0.5, 0.0, 1.0);
        BUILDER.pop();

        // ===== 帝国天鹰 =====
        BUILDER.push("imperial_eagle");
        EAGLE_SKILL_TICKS = BUILDER
                .comment("主动技能效果持续（tick，2400 = 2 分钟）")
                .defineInRange("skillTicks", 2400, 1, MAX_TICKS);
        EAGLE_SKILL_COOLDOWN_TICKS = BUILDER
                .comment("主动技能冷却（tick，600 = 30 秒）")
                .defineInRange("skillCooldownTicks", 600, 0, MAX_TICKS);
        BUILDER.pop();

        // ===== 破损面具 =====
        BUILDER.push("broken_mask");
        MASK_ARMOR_BONUS = BUILDER
                .comment("基础护甲加成（点数）")
                .defineInRange("armorBonus", 7.0, 0.0, 1000.0);
        MASK_MAX_HEALTH_BONUS = BUILDER
                .comment("基础最大生命加成（比例，0.20 = +20%）")
                .defineInRange("maxHealthBonus", 0.20, 0.0, 10.0);
        MASK_TOUGHNESS_BONUS = BUILDER
                .comment("基础护甲韧性加成（点数）")
                .defineInRange("toughnessBonus", 3.0, 0.0, 1000.0);
        MASK_SKILL_RADIUS = BUILDER
                .comment("主动技能半径（格）")
                .defineInRange("skillRadius", 8.0, 0.0, 64.0);
        MASK_SKILL_VULN_DURATION_TICKS = BUILDER
                .comment("主动技能范围内易伤持续（tick，1200 = 1 分钟）")
                .defineInRange("skillVulnDurationTicks", 1200, 1, MAX_TICKS);
        MASK_SKILL_RESIST_DURATION_TICKS = BUILDER
                .comment("主动技能自身抗性持续（tick，1200 = 1 分钟）")
                .defineInRange("skillResistDurationTicks", 1200, 1, MAX_TICKS);
        MASK_SKILL_HEAL_RATIO = BUILDER
                .comment("主动技能自身治疗比例（0.5 = 生命上限一半）")
                .defineInRange("skillHealRatio", 0.5, 0.0, 1.0);
        MASK_SKILL_COOLDOWN_TICKS = BUILDER
                .comment("主动技能冷却（tick，1800 = 90 秒）")
                .defineInRange("skillCooldownTicks", 1800, 0, MAX_TICKS);
        MASK_VULNERABILITY_CHANCE = BUILDER
                .comment("攻击命中施加易伤的概率（0.20 = 20%）")
                .defineInRange("vulnerabilityChance", 0.20, 0.0, 1.0);
        MASK_VULNERABILITY_DURATION_TICKS = BUILDER
                .comment("命中施加的易伤持续（tick，200 = 10 秒）")
                .defineInRange("vulnerabilityDurationTicks", 200, 1, MAX_TICKS);
        MASK_STRENGTH_DURATION_TICKS = BUILDER
                .comment("易伤被消耗时获得的力量持续（tick，100 = 5 秒）")
                .defineInRange("strengthDurationTicks", 100, 1, MAX_TICKS);
        MASK_REWARD_HEAL_AMOUNT = BUILDER
                .comment("易伤被消耗时回复的生命值（点）")
                .defineInRange("rewardHealAmount", 10.0, 0.0, 1000.0);
        MASK_STRENGTH_CHANCE = BUILDER
                .comment("易伤被消耗时获得力量的概率（0.50 = 50%）")
                .defineInRange("strengthChance", 0.50, 0.0, 1.0);
        BUILDER.pop();

        // ===== 亡灵秘典 =====
        BUILDER.push("necromancer_ledger");
        LEDGER_MAX_HEALTH_REDUCTION = BUILDER
                .comment("最大生命值减少比例（0.20 = 20%）")
                .defineInRange("maxHealthReduction", 0.20, 0.0, 0.99);
        LEDGER_ATTACK_DAMAGE_REDUCTION = BUILDER
                .comment("攻击伤害减少比例（0.95 = 95%）")
                .defineInRange("attackDamageReduction", 0.95, 0.0, 0.99);
        LEDGER_ABSORPTION_AMOUNT = BUILDER
                .comment("周期性获得的黄心点数")
                .defineInRange("absorptionAmount", 10.0, 0.0, 1000.0);
        LEDGER_ABSORPTION_INTERVAL_TICKS = BUILDER
                .comment("黄心回复间隔（tick，1200 = 60 秒）")
                .defineInRange("absorptionIntervalTicks", 1200, 1, MAX_TICKS);
        LEDGER_CALAMITY_BASE_LEVEL = BUILDER
                .comment("攻击施加的灾厄基础等级")
                .defineInRange("calamityBaseLevel", 1, 1, 100);
        LEDGER_CALAMITY_DURATION_RATIO = BUILDER
                .comment("灾厄时长比例：时长（秒）= 攻击伤害 × 该值")
                .defineInRange("calamityDurationRatio", 0.8, 0.0, 100.0);
        LEDGER_SKILL_STACK_DURATION_MULTIPLIER = BUILDER
                .comment("技能后下次攻击的灾厄时长倍率")
                .defineInRange("skillStackDurationMultiplier", 2.0, 1.0, 100.0);
        LEDGER_LETHAL_DAMAGE = BUILDER
                .comment("致命一击的直接伤害（灾厄等级×时长 ≥ 目标生命时触发）")
                .defineInRange("lethalDamage", 32676.0, 0.0, 1.0E9);
        LEDGER_SKILL_RADIUS = BUILDER
                .comment("R 技能影响半径（格）")
                .defineInRange("skillRadius", 8.0, 0.0, 64.0);
        LEDGER_SKILL_LEVEL_BONUS = BUILDER
                .comment("R 技能：周围生物灾厄等级额外加值")
                .defineInRange("skillLevelBonus", 3, 0, 100);
        LEDGER_SKILL_DURATION_RATIO = BUILDER
                .comment("R 技能：周围生物灾厄时长变为该比例（0.20 = 1/5）")
                .defineInRange("skillDurationRatio", 0.20, 0.0, 1.0);
        BUILDER.pop();

        // ===== 紧缚绷带 =====
        BUILDER.push("binding_bandage");
        BANDAGE_ENDURANCE_CHANCE = BUILDER
                .comment("攻击命中为自己施加耐力的概率（0.5 = 50%）")
                .defineInRange("enduranceChance", 0.5, 0.0, 1.0);
        BANDAGE_ENDURANCE_DURATION_TICKS = BUILDER
                .comment("耐力持续（tick，60 = 3 秒）")
                .defineInRange("enduranceDurationTicks", 60, 1, MAX_TICKS);
        BANDAGE_WEAKNESS_BONUS_PER_LEVEL = BUILDER
                .comment("对虚弱目标攻击的额外伤害：虚弱等级 × 该值（点）")
                .defineInRange("weaknessBonusPerLevel", 2.0, 0.0, 100.0);
        BANDAGE_SHIELD_GAIN = BUILDER
                .comment("主动技能每次获得的护盾点数")
                .defineInRange("shieldGain", 1, 0, 100);
        BANDAGE_SHIELD_MAX = BUILDER
                .comment("护盾存储上限（点）")
                .defineInRange("shieldMax", 3, 0, 100);
        BANDAGE_SKILL_COOLDOWN_TICKS = BUILDER
                .comment("主动技能冷却（tick，900 = 45 秒）")
                .defineInRange("skillCooldownTicks", 900, 0, MAX_TICKS);
        BUILDER.pop();

        // ===== 封印王座 =====
        BUILDER.push("sealed_throne");
        THRONE_MAX_STARS = BUILDER
                .comment("星辉上限（点）")
                .defineInRange("maxStars", 12, 1, 1000);
        THRONE_STAR_REGEN_TICKS = BUILDER
                .comment("星辉回复间隔（tick，100 = 5 秒）")
                .defineInRange("starRegenTicks", 100, 1, MAX_TICKS);
        THRONE_STAR_LOSS_PER_HIT = BUILDER
                .comment("受击扣除红心时损失的星辉（点）")
                .defineInRange("starLossPerHit", 2, 0, 100);
        THRONE_DAMAGE_PER_STAR = BUILDER
                .comment("每个星辉提供的攻击伤害加成（0.05 = 5%）")
                .defineInRange("damagePerStar", 0.05, 0.0, 10.0);
        THRONE_STARS_PER_ARMOR = BUILDER
                .comment("每多少个星辉提供一组护甲加成")
                .defineInRange("starsPerArmor", 4, 1, 1000);
        THRONE_ARMOR_PER_GROUP = BUILDER
                .comment("每星辉组提供的护甲点数")
                .defineInRange("armorPerGroup", 2.0, 0.0, 1000.0);
        THRONE_STARS_PER_SHIELD = BUILDER
                .comment("主动技能：每多少星辉转化为 1 点护盾")
                .defineInRange("starsPerShield", 6, 1, 1000);
        THRONE_SHIELD_MAX = BUILDER
                .comment("王座护盾堆叠上限（点）")
                .defineInRange("shieldMax", 4, 1, 100);
        THRONE_ABSORPTION_DURATION_TICKS = BUILDER
                .comment("主动技能黄心（吸收）持续（tick，400 = 20 秒）")
                .defineInRange("absorptionDurationTicks", 400, 1, MAX_TICKS);
        BUILDER.pop();

        // ===== 充能核心 =====
        BUILDER.push("charging_core");
        CORE_SKILL_COOLDOWN_TICKS = BUILDER
                .comment("形态切换冷却（tick，100 = 5 秒）")
                .defineInRange("skillCooldownTicks", 100, 0, MAX_TICKS);
        CORE_THUNDER_MAX_LAYERS = BUILDER
                .comment("雷霆形态充能层数上限")
                .defineInRange("thunderMaxLayers", 6, 1, 100);
        CORE_THUNDER_LAYER_TICKS = BUILDER
                .comment("雷霆形态每层充能时间（tick，200 = 10 秒）")
                .defineInRange("thunderLayerTicks", 200, 1, MAX_TICKS);
        CORE_FROST_MAX_LAYERS = BUILDER
                .comment("冰冻形态充能层数上限")
                .defineInRange("frostMaxLayers", 10, 1, 100);
        CORE_FROST_LAYER_TICKS = BUILDER
                .comment("冰冻形态每层充能时间（tick，300 = 15 秒）")
                .defineInRange("frostLayerTicks", 300, 1, MAX_TICKS);
        CORE_DARK_MAX_LAYERS = BUILDER
                .comment("黑暗形态充能层数上限")
                .defineInRange("darkMaxLayers", 5, 1, 100);
        CORE_DARK_LAYER_TICKS = BUILDER
                .comment("黑暗形态每层充能时间（tick，600 = 30 秒）")
                .defineInRange("darkLayerTicks", 600, 1, MAX_TICKS);
        CORE_THUNDER_DAMAGE_PER_LAYER = BUILDER
                .comment("雷霆常态：每层攻击伤害加成（0.05 = 5%）")
                .defineInRange("thunderDamagePerLayer", 0.05, 0.0, 10.0);
        CORE_THUNDER_BURST_TICKS = BUILDER
                .comment("雷霆释放：力量持续（tick，300 = 15 秒）")
                .defineInRange("thunderBurstTicks", 300, 1, MAX_TICKS);
        CORE_FROST_ARMOR_PER_LAYER = BUILDER
                .comment("冰冻常态：每层护甲加成（点）")
                .defineInRange("frostArmorPerLayer", 1.0, 0.0, 100.0);
        CORE_FROST_TOUGHNESS_PER_LAYER = BUILDER
                .comment("冰冻常态：每层护甲韧性加成（点，默认 0.5 即每 2 层 1 点）")
                .defineInRange("frostToughnessPerLayer", 0.5, 0.0, 100.0);
        CORE_DARK_DAMAGE_PER_LAYER = BUILDER
                .comment("黑暗释放：每层转化的下次攻击附加伤害（点）")
                .defineInRange("darkDamagePerLayer", 8, 0, 1000);
        BUILDER.pop();

        // ===== 动力剑 =====
        BUILDER.push("powersword");
        SWORD_ARMOR_PENALTY_THRESHOLD = BUILDER
                .comment("穿甲：护甲值超过该值后开始计算额外伤害")
                .defineInRange("armorPenaltyThreshold", 8, 0, 1000);
        SWORD_ARMOR_TOUGHNESS_THRESHOLD = BUILDER
                .comment("穿甲：护甲韧性超过该值后开始计算额外伤害")
                .defineInRange("armorToughnessThreshold", 6, 0, 1000);
        SWORD_ARMOR_PENALTY_MULTIPLIER = BUILDER
                .comment("穿甲：每点超出护甲的额外伤害")
                .defineInRange("armorPenaltyMultiplier", 0.3, 0.0, 100.0);
        SWORD_ARMOR_TOUGHNESS_MULTIPLIER = BUILDER
                .comment("穿甲：每点超出韧性的额外伤害")
                .defineInRange("armorToughnessMultiplier", 0.5, 0.0, 100.0);
        SWORD_MAX_PENALTY_DAMAGE = BUILDER
                .comment("穿甲伤害合计上限（点）")
                .defineInRange("maxPenaltyDamage", 10, 0, 1000);
        SWORD_OVERCLOCK_DURATION_TICKS = BUILDER
                .comment("超频持续（tick，260 = 13 秒）")
                .defineInRange("overclockDurationTicks", 260, 1, MAX_TICKS);
        SWORD_OVERCLOCK_DURABILITY_COST = BUILDER
                .comment("超频激活损耗的耐久（点）")
                .defineInRange("overclockDurabilityCost", 5, 0, 1000);
        SWORD_OVERCLOCK_COOLDOWN_TICKS = BUILDER
                .comment("超频冷却（tick，600 = 30 秒）")
                .defineInRange("overclockCooldownTicks", 600, 0, MAX_TICKS);
        SWORD_RESISTANCE_DURATION_TICKS = BUILDER
                .comment("超频激活时的抗性 II 持续（tick，100 = 5 秒）")
                .defineInRange("resistanceDurationTicks", 100, 1, MAX_TICKS);
        SWORD_FIRE_DAMAGE = BUILDER
                .comment("超频命中额外火焰伤害（点）")
                .defineInRange("fireDamage", 3, 0, 1000);
        SWORD_FIRE_BURN_DURATION = BUILDER
                .comment("超频命中点燃持续（tick，80 = 4 秒）")
                .defineInRange("fireBurnDurationTicks", 80, 0, MAX_TICKS);
        SWORD_OVERCLOCK_DAMAGE_MULTIPLIER = BUILDER
                .comment("超频期间攻击伤害倍率（1.5 = +50%）")
                .defineInRange("overclockDamageMultiplier", 1.5, 0.0, 100.0);
        SWORD_OVERCLOCK_HEAL_RATIO = BUILDER
                .comment("超频期间吸血比例（0.6 = 造成伤害的 60%）")
                .defineInRange("overclockHealRatio", 0.6, 0.0, 100.0);
        BUILDER.pop();

        // ===== 招架之盾 =====
        BUILDER.push("parryshield");
        PARRY_WINDOW_TICKS = BUILDER
                .comment("招架窗口：举盾后该 tick 数内可招架（20 = 1 秒）")
                .defineInRange("parryWindowTicks", 20, 1, MAX_TICKS);
        PARRY_REFLECT_RATIO = BUILDER
                .comment("招架反弹伤害倍率（2.5 = 250%）")
                .defineInRange("parryReflectRatio", 2.5, 0.0, 100.0);
        PARRY_WEAKNESS_DURATION_TICKS = BUILDER
                .comment("招架使攻击者虚弱的持续（tick，60 = 3 秒）")
                .defineInRange("weaknessDurationTicks", 60, 1, MAX_TICKS);
        PARRY_WEAKNESS_AMPLIFIER = BUILDER
                .comment("招架虚弱的等级（0 = I 级）")
                .defineInRange("weaknessAmplifier", 1, 0, 255);
        PARRY_LONG_BLOCK_ABSORB = BUILDER
                .comment("长举格挡吸收伤害比例（0.8 = 80%）")
                .defineInRange("longBlockAbsorb", 0.8, 0.0, 1.0);
        PARRY_COOLDOWN_TICKS = BUILDER
                .comment("招架冷却（tick，20 = 1 秒）")
                .defineInRange("parryCooldownTicks", 20, 0, MAX_TICKS);
        BUILDER.pop();

        // ===== 链锯剑 =====
        BUILDER.push("chainsawsword");
        CHAIN_LACERATION_LEVELS_PER_ATTACK = BUILDER
                .comment("普通攻击命中叠加的撕裂层数")
                .defineInRange("lacerationLevelsPerAttack", 4, 0, 100);
        CHAIN_LACERATION_DURATION_TICKS = BUILDER
                .comment("撕裂持续（tick，每次命中刷新）")
                .defineInRange("lacerationDurationTicks", 40, 1, MAX_TICKS);
        CHAIN_SLASH_INTERVAL_TICKS = BUILDER
                .comment("扫射间隔：每 N tick 一刀（2 = 10 刀/秒）")
                .defineInRange("slashIntervalTicks", 2, 1, 100);
        CHAIN_SLASH_DAMAGE = BUILDER
                .comment("扫射每刀伤害（点）")
                .defineInRange("slashDamage", 0.5, 0.0, 1000.0);
        CHAIN_SLASH_LACERATION_LEVELS = BUILDER
                .comment("扫射每刀叠加的撕裂层数")
                .defineInRange("slashLacerationLevels", 1, 0, 100);
        CHAIN_SLASH_ARC_DEGREES = BUILDER
                .comment("扫射扇形总角度（度，半角 = 该值 / 2）")
                .defineInRange("slashArcDegrees", 90.0, 1.0, 360.0);
        CHAIN_SLASH_RANGE = BUILDER
                .comment("扫射作用半径（格）")
                .defineInRange("slashRange", 4.0, 0.0, 64.0);
        CHAIN_SLASH_MAX_USE_TICKS = BUILDER
                .comment("单次扫射最长持续（tick，140 = 7 秒）")
                .defineInRange("slashMaxUseTicks", 140, 1, MAX_TICKS);
        CHAIN_SLASH_COOLDOWN_TICKS = BUILDER
                .comment("扫射结束后的冷却（tick，300 = 15 秒）")
                .defineInRange("slashCooldownTicks", 300, 0, MAX_TICKS);
        CHAIN_MIN_USE_FOR_COOLDOWN_TICKS = BUILDER
                .comment("不足该 tick 数视为短按，不施加冷却")
                .defineInRange("minUseForCooldownTicks", 10, 0, MAX_TICKS);
        CHAIN_LACERATION_DAMAGE_PER_LEVEL = BUILDER
                .comment("撕裂结算：每层造成的伤害（点）")
                .defineInRange("lacerationDamagePerLevel", 0.6, 0.0, 100.0);
        CHAIN_LACERATION_REAPPLY_DURATION_TICKS = BUILDER
                .comment("撕裂减半后重新挂上的持续（tick）")
                .defineInRange("lacerationReapplyDurationTicks", 30, 1, MAX_TICKS);
        BUILDER.pop();

        // ===== 痛击之锤 =====
        BUILDER.push("painstrike_hammer");
        HAMMER_AREA_RADIUS = BUILDER
                .comment("范围打击：命中点周围半径（格）")
                .defineInRange("areaRadius", 2.0, 0.0, 64.0);
        HAMMER_AREA_DAMAGE_RATIO = BUILDER
                .comment("范围打击伤害：攻击者攻击伤害的比例（0.6 = 60%）")
                .defineInRange("areaDamageRatio", 0.6, 0.0, 10.0);
        HAMMER_VULNERABILITY_DURATION_TICKS = BUILDER
                .comment("命中施加的易伤持续（tick，200 = 10 秒）")
                .defineInRange("vulnerabilityDurationTicks", 200, 1, MAX_TICKS);
        HAMMER_VULNERABILITY_LEVEL_STEP = BUILDER
                .comment("每次命中（含范围命中）叠加的易伤等级")
                .defineInRange("vulnerabilityLevelStep", 2, 0, 100);
        BUILDER.pop();

        // ===== 墨制刀刃 =====
        BUILDER.push("ink_blade");
        INK_COOLDOWN_TICKS = BUILDER
                .comment("投掷冷却（tick，3 = 0.15 秒）")
                .defineInRange("cooldownTicks", 3, 0, MAX_TICKS);
        INK_REGEN_INTERVAL_TICKS = BUILDER
                .comment("耐久回收间隔（tick，10 = 0.5 秒/点，即每秒 2 点）")
                .defineInRange("regenIntervalTicks", 10, 1, MAX_TICKS);
        INK_THROW_SPEED = BUILDER
                .comment("飞刃初速（雪球为 1.5）")
                .defineInRange("throwSpeed", 2.5, 0.1, 10.0);
        INK_DAMAGE = BUILDER
                .comment("飞刃命中伤害（点）")
                .defineInRange("damage", 3.0, 0.0, 1000.0);
        INK_WEAKNESS_DURATION_TICKS = BUILDER
                .comment("虚弱叠加持续（tick，100 = 5 秒，命中刷新）")
                .defineInRange("weaknessDurationTicks", 100, 1, MAX_TICKS);
        INK_WEAKNESS_MAX_LEVEL = BUILDER
                .comment("虚弱叠加上限（级）")
                .defineInRange("weaknessMaxLevel", 5, 1, 255);
        BUILDER.pop();

        // ===== 星辉大剑 =====
        BUILDER.push("starlight_greatsword");
        STAR_MAX_CHARGE = BUILDER
                .comment("星辉储能层数上限")
                .defineInRange("maxCharge", 20, 1, 1000);
        STAR_CHARGE_INTERVAL_TICKS = BUILDER
                .comment("每层储能的聚能间隔（tick，60 = 3 秒）")
                .defineInRange("chargeIntervalTicks", 60, 1, MAX_TICKS);
        STAR_DAMAGE_PER_CHARGE = BUILDER
                .comment("每层储能的攻击伤害加成（0.10 = 10%）")
                .defineInRange("damagePerCharge", 0.10, 0.0, 10.0);
        STAR_ABSORPTION_DURATION_TICKS = BUILDER
                .comment("命中后黄心（吸收）持续（tick，400 = 20 秒）")
                .defineInRange("absorptionDurationTicks", 400, 1, MAX_TICKS);
        STAR_DAMAGE_PER_ABSORPTION_LEVEL = BUILDER
                .comment("吸收转化：每多少点命中伤害转化为 1 层吸收（每层 2 点黄心）")
                .defineInRange("damagePerAbsorptionLevel", 5.0, 0.1, 1000.0);
        BUILDER.pop();

        // ===== 死神镰刀 =====
        BUILDER.push("reaper_scythe");
        SCYTHE_DAMAGE_PER_NEW_KILL = BUILDER
                .comment("每击杀一种新生物的攻击伤害加成（点，无上限）")
                .defineInRange("damagePerNewKill", 2, 0, 1000);
        BUILDER.pop();

        // ===== 机器爪刃 =====
        BUILDER.push("machine_claw");
        CLAW_COMBO_DAMAGE_PER_HIT = BUILDER
                .comment("连击：每次命中带来的后续攻击伤害加成（点）")
                .defineInRange("comboDamagePerHit", 1, 0, 1000);
        CLAW_COMBO_TIMEOUT_TICKS = BUILDER
                .comment("连击重置的停顿时长（tick，40 = 2 秒，含挥空）")
                .defineInRange("comboTimeoutTicks", 40, 1, MAX_TICKS);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /** 配置加载 / 重载时回填到各饰品类与事件类的静态字段（默认值即各字段初始值） */
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 结晶心脏
        crystalline_heart.MAX_HEALTH_PENALTY = HEART_MAX_HEALTH_PENALTY.get();
        crystalline_heart.PER_HEALTH_TO_SHIELD = HEART_PER_HEALTH_TO_SHIELD.get();
        ShieldEvent.HEAL_INTERVAL_TICKS = HEART_SHIELD_HEAL_INTERVAL_TICKS.get();
        ShieldEvent.ARMOR_TO_SHIELD = HEART_ARMOR_TO_SHIELD.get();

        // 流血护盾
        bleedingshield.ARMOR_REDUCTION_RATIO = BLEEDING_ARMOR_REDUCTION_RATIO.get();
        bleedingshield.HEALTH_REDUCTION_RATIO = BLEEDING_HEALTH_REDUCTION_RATIO.get();
        bleedingshield.ARMOR_PER_TOUGHNESS = BLEEDING_ARMOR_PER_TOUGHNESS.get();
        bleedingshield.HEALTH_PER_TOUGHNESS = BLEEDING_HEALTH_PER_TOUGHNESS.get();

        // 暗夜契约
        NightContractEvent.HEAL_INTERVAL_TICKS = NIGHT_HEAL_INTERVAL_TICKS.get();
        NightContractEvent.BONUS_PER_BRIGHTNESS = NIGHT_BONUS_PER_BRIGHTNESS.get();
        NightContractEvent.NEW_MOON_BASE_BONUS = NIGHT_NEW_MOON_BASE_BONUS.get();

        // 求生渴望
        survival_whimper.COOLDOWN_TICKS = WHIMPER_COOLDOWN_TICKS.get();

        // 龙胤之力
        dragon_power.COOLDOWN_TICKS = DRAGON_COOLDOWN_TICKS.get();
        dragon_power.INVISIBILITY_TICKS = DRAGON_INVISIBILITY_TICKS.get();
        dragon_power.INVULNERABLE_TICKS = DRAGON_INVULNERABLE_TICKS.get();
        dragon_power.DARKNESS_TICKS = DRAGON_DARKNESS_TICKS.get();
        dragon_power.REVIVE_HEALTH_RATIO = (float) DRAGON_REVIVE_HEALTH_RATIO.get().doubleValue();

        // 帝国天鹰
        imperial_eagle.SKILL_TICKS = EAGLE_SKILL_TICKS.get();
        imperial_eagle.SKILL_COOLDOWN_TICKS = EAGLE_SKILL_COOLDOWN_TICKS.get();

        // 破损面具
        broken_mask.ARMOR_BONUS = MASK_ARMOR_BONUS.get();
        broken_mask.MAX_HEALTH_BONUS = MASK_MAX_HEALTH_BONUS.get();
        broken_mask.TOUGHNESS_BONUS = MASK_TOUGHNESS_BONUS.get();
        broken_mask.SKILL_RADIUS = MASK_SKILL_RADIUS.get();
        broken_mask.SKILL_VULN_DURATION_TICKS = MASK_SKILL_VULN_DURATION_TICKS.get();
        broken_mask.SKILL_RESIST_DURATION_TICKS = MASK_SKILL_RESIST_DURATION_TICKS.get();
        broken_mask.SKILL_HEAL_RATIO = (float) MASK_SKILL_HEAL_RATIO.get().doubleValue();
        broken_mask.SKILL_COOLDOWN_TICKS = MASK_SKILL_COOLDOWN_TICKS.get();
        BrokenMaskEvent.VULNERABILITY_CHANCE = (float) MASK_VULNERABILITY_CHANCE.get().doubleValue();
        BrokenMaskEvent.VULNERABILITY_DURATION_TICKS = MASK_VULNERABILITY_DURATION_TICKS.get();
        BrokenMaskEvent.STRENGTH_DURATION_TICKS = MASK_STRENGTH_DURATION_TICKS.get();
        BrokenMaskEvent.REWARD_HEAL_AMOUNT = (float) MASK_REWARD_HEAL_AMOUNT.get().doubleValue();
        BrokenMaskEvent.STRENGTH_CHANCE = (float) MASK_STRENGTH_CHANCE.get().doubleValue();

        // 亡灵秘典
        necromancer_ledger.MAX_HEALTH_REDUCTION = (float) LEDGER_MAX_HEALTH_REDUCTION.get().doubleValue();
        necromancer_ledger.ATTACK_DAMAGE_REDUCTION = (float) LEDGER_ATTACK_DAMAGE_REDUCTION.get().doubleValue();
        necromancer_ledger.ABSORPTION_AMOUNT = LEDGER_ABSORPTION_AMOUNT.get();
        necromancer_ledger.ABSORPTION_INTERVAL_TICKS = LEDGER_ABSORPTION_INTERVAL_TICKS.get();
        necromancer_ledger.CALAMITY_BASE_LEVEL = LEDGER_CALAMITY_BASE_LEVEL.get();
        necromancer_ledger.CALAMITY_DURATION_RATIO = LEDGER_CALAMITY_DURATION_RATIO.get();
        necromancer_ledger.SKILL_STACK_DURATION_MULTIPLIER = LEDGER_SKILL_STACK_DURATION_MULTIPLIER.get();
        necromancer_ledger.LETHAL_DAMAGE = (float) LEDGER_LETHAL_DAMAGE.get().doubleValue();
        necromancer_ledger.SKILL_RADIUS = LEDGER_SKILL_RADIUS.get();
        necromancer_ledger.SKILL_LEVEL_BONUS = LEDGER_SKILL_LEVEL_BONUS.get();
        necromancer_ledger.SKILL_DURATION_RATIO = (float) LEDGER_SKILL_DURATION_RATIO.get().doubleValue();

        // 紧缚绷带
        binding_bandage.ENDURANCE_CHANCE = (float) BANDAGE_ENDURANCE_CHANCE.get().doubleValue();
        binding_bandage.ENDURANCE_DURATION_TICKS = BANDAGE_ENDURANCE_DURATION_TICKS.get();
        binding_bandage.WEAKNESS_BONUS_PER_LEVEL = (float) BANDAGE_WEAKNESS_BONUS_PER_LEVEL.get().doubleValue();
        binding_bandage.SHIELD_GAIN = BANDAGE_SHIELD_GAIN.get();
        binding_bandage.SHIELD_MAX = BANDAGE_SHIELD_MAX.get();
        binding_bandage.SKILL_COOLDOWN_TICKS = BANDAGE_SKILL_COOLDOWN_TICKS.get();

        // 封印王座
        sealed_throne.MAX_STARS = THRONE_MAX_STARS.get();
        sealed_throne.STAR_REGEN_TICKS = THRONE_STAR_REGEN_TICKS.get();
        sealed_throne.STAR_LOSS_PER_HIT = THRONE_STAR_LOSS_PER_HIT.get();
        sealed_throne.DAMAGE_PER_STAR = THRONE_DAMAGE_PER_STAR.get();
        sealed_throne.STARS_PER_ARMOR = THRONE_STARS_PER_ARMOR.get();
        sealed_throne.ARMOR_PER_GROUP = THRONE_ARMOR_PER_GROUP.get();
        sealed_throne.STARS_PER_SHIELD = THRONE_STARS_PER_SHIELD.get();
        sealed_throne.SHIELD_MAX = THRONE_SHIELD_MAX.get();
        sealed_throne.ABSORPTION_DURATION_TICKS = THRONE_ABSORPTION_DURATION_TICKS.get();

        // 充能核心
        charging_core.SKILL_COOLDOWN_TICKS = CORE_SKILL_COOLDOWN_TICKS.get();
        charging_core.THUNDER_BURST_TICKS = CORE_THUNDER_BURST_TICKS.get();
        charging_core.THUNDER_DAMAGE_PER_LAYER = (float) CORE_THUNDER_DAMAGE_PER_LAYER.get().doubleValue();
        charging_core.FROST_ARMOR_PER_LAYER = CORE_FROST_ARMOR_PER_LAYER.get();
        charging_core.FROST_TOUGHNESS_PER_LAYER = CORE_FROST_TOUGHNESS_PER_LAYER.get();
        charging_core.DARK_DAMAGE_PER_LAYER = CORE_DARK_DAMAGE_PER_LAYER.get();
        charging_core.Form.THUNDER.maxLayers = CORE_THUNDER_MAX_LAYERS.get();
        charging_core.Form.THUNDER.layerTicks = CORE_THUNDER_LAYER_TICKS.get();
        charging_core.Form.FROST.maxLayers = CORE_FROST_MAX_LAYERS.get();
        charging_core.Form.FROST.layerTicks = CORE_FROST_LAYER_TICKS.get();
        charging_core.Form.DARK.maxLayers = CORE_DARK_MAX_LAYERS.get();
        charging_core.Form.DARK.layerTicks = CORE_DARK_LAYER_TICKS.get();

        // ===== 动力剑 =====
        powersword.ARMOR_PENALTY_THRESHOLD = SWORD_ARMOR_PENALTY_THRESHOLD.get();
        powersword.ARMOR_TOUGHNESS_THRESHOLD = SWORD_ARMOR_TOUGHNESS_THRESHOLD.get();
        powersword.ARMOR_PENALTY_MULTIPLIER = (float) SWORD_ARMOR_PENALTY_MULTIPLIER.get().doubleValue();
        powersword.ARMOR_TOUGHNESS_MULTIPLIER = (float) SWORD_ARMOR_TOUGHNESS_MULTIPLIER.get().doubleValue();
        powersword.MAX_PENALTY_DAMAGE = SWORD_MAX_PENALTY_DAMAGE.get();
        powersword.OVERCLOCK_DURATION_TICKS = SWORD_OVERCLOCK_DURATION_TICKS.get();
        powersword.OVERCLOCK_DURABILITY_COST = SWORD_OVERCLOCK_DURABILITY_COST.get();
        powersword.OVERCLOCK_COOLDOWN_TICKS = SWORD_OVERCLOCK_COOLDOWN_TICKS.get();
        powersword.RESISTANCE_DURATION_TICKS = SWORD_RESISTANCE_DURATION_TICKS.get();
        powersword.FIRE_DAMAGE = SWORD_FIRE_DAMAGE.get();
        powersword.FIRE_BURN_DURATION = SWORD_FIRE_BURN_DURATION.get();
        powersword.OVERCLOCK_DAMAGE_MULTIPLIER = (float) SWORD_OVERCLOCK_DAMAGE_MULTIPLIER.get().doubleValue();
        powersword.OVERCLOCK_HEAL_RATIO = (float) SWORD_OVERCLOCK_HEAL_RATIO.get().doubleValue();

        // ===== 招架之盾 =====
        parryshield.PARRY_WINDOW_TICKS = PARRY_WINDOW_TICKS.get();
        parryshield.PARRY_REFLECT_RATIO = (float) PARRY_REFLECT_RATIO.get().doubleValue();
        parryshield.WEAKNESS_DURATION_TICKS = PARRY_WEAKNESS_DURATION_TICKS.get();
        parryshield.WEAKNESS_AMPLIFIER = PARRY_WEAKNESS_AMPLIFIER.get();
        parryshield.LONG_BLOCK_ABSORB = (float) PARRY_LONG_BLOCK_ABSORB.get().doubleValue();
        parryshield.PARRY_COOLDOWN_TICKS = PARRY_COOLDOWN_TICKS.get();

        // ===== 链锯剑（含撕裂结算参数）=====
        chainsawsword.LACERATION_LEVELS_PER_ATTACK = CHAIN_LACERATION_LEVELS_PER_ATTACK.get();
        chainsawsword.LACERATION_DURATION_TICKS = CHAIN_LACERATION_DURATION_TICKS.get();
        chainsawsword.SLASH_INTERVAL_TICKS = CHAIN_SLASH_INTERVAL_TICKS.get();
        chainsawsword.SLASH_DAMAGE = (float) CHAIN_SLASH_DAMAGE.get().doubleValue();
        chainsawsword.SLASH_LACERATION_LEVELS = CHAIN_SLASH_LACERATION_LEVELS.get();
        chainsawsword.SLASH_ARC_DEGREES = (float) CHAIN_SLASH_ARC_DEGREES.get().doubleValue();
        chainsawsword.SLASH_RANGE = (float) CHAIN_SLASH_RANGE.get().doubleValue();
        chainsawsword.SLASH_MAX_USE_TICKS = CHAIN_SLASH_MAX_USE_TICKS.get();
        chainsawsword.SLASH_COOLDOWN_TICKS = CHAIN_SLASH_COOLDOWN_TICKS.get();
        chainsawsword.MIN_USE_FOR_COOLDOWN_TICKS = CHAIN_MIN_USE_FOR_COOLDOWN_TICKS.get();
        LacerationEffect.DAMAGE_PER_LEVEL = (float) CHAIN_LACERATION_DAMAGE_PER_LEVEL.get().doubleValue();
        LacerationEffect.REAPPLY_DURATION_TICKS = CHAIN_LACERATION_REAPPLY_DURATION_TICKS.get();

        // ===== 痛击之锤 =====
        painstrike_hammer.AREA_RADIUS = HAMMER_AREA_RADIUS.get();
        painstrike_hammer.AREA_DAMAGE_RATIO = HAMMER_AREA_DAMAGE_RATIO.get();
        painstrike_hammer.VULNERABILITY_DURATION_TICKS = HAMMER_VULNERABILITY_DURATION_TICKS.get();
        painstrike_hammer.VULNERABILITY_LEVEL_STEP = HAMMER_VULNERABILITY_LEVEL_STEP.get();

        // ===== 墨制刀刃 =====
        ink_blade.COOLDOWN_TICKS = INK_COOLDOWN_TICKS.get();
        ink_blade.REGEN_INTERVAL_TICKS = INK_REGEN_INTERVAL_TICKS.get();
        ink_blade.THROW_SPEED = (float) INK_THROW_SPEED.get().doubleValue();
        InkBladeProjectile.DAMAGE = (float) INK_DAMAGE.get().doubleValue();
        InkBladeProjectile.WEAKNESS_DURATION_TICKS = INK_WEAKNESS_DURATION_TICKS.get();
        InkBladeProjectile.WEAKNESS_MAX_LEVEL = INK_WEAKNESS_MAX_LEVEL.get();

        // ===== 星辉大剑 =====
        starlight_greatsword.MAX_CHARGE = STAR_MAX_CHARGE.get();
        starlight_greatsword.CHARGE_INTERVAL_TICKS = STAR_CHARGE_INTERVAL_TICKS.get();
        starlight_greatsword.DAMAGE_PER_CHARGE = (float) STAR_DAMAGE_PER_CHARGE.get().doubleValue();
        starlight_greatsword.ABSORPTION_DURATION_TICKS = STAR_ABSORPTION_DURATION_TICKS.get();
        starlight_greatsword.DAMAGE_PER_ABSORPTION_LEVEL = (float) STAR_DAMAGE_PER_ABSORPTION_LEVEL.get().doubleValue();

        // ===== 死神镰刀 =====
        reaper_scythe.DAMAGE_PER_NEW_KILL = SCYTHE_DAMAGE_PER_NEW_KILL.get();

        // ===== 机器爪刃 =====
        machine_claw.COMBO_DAMAGE_PER_HIT = CLAW_COMBO_DAMAGE_PER_HIT.get();
        machine_claw.COMBO_TIMEOUT_TICKS = CLAW_COMBO_TIMEOUT_TICKS.get();
    }
}
