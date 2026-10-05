package Alone818.com.alone_adventure.faction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 派系中枢。
 *
 * 负责：
 *
 * 1. 派系注册
 * 2. 生物归属查询（实体类型 -> 派系）
 * 3. 派系关系矩阵
 *
 * 关系默认值：
 * - 同派系 -> 同盟
 * - 不同派系 -> 敌对（全面开战）
 * - 无派系（玩家、未划入派系的生物）-> 与谁都中立
 *
 * 以后想调整某一对派系的关系，
 * 调 setRelation 即可，例如把恶魔和部落设为中立：
 *
 * FactionManager.setRelation(
 *         ModFactions.DEMON,
 *         ModFactions.TRIBE,
 *         Faction.Relation.NEUTRAL
 * );
 */
public final class FactionManager {

    /** 全部派系，按 id 索引 */
    private static final Map<String, Faction> FACTIONS =
            new HashMap<>();

    /** 实体类型 -> 完整分类（派系 + 攻击方式 + 等级） */
    private static final Map<EntityType<?>, MobClassification> TYPE_CLASSIFICATIONS =
            new HashMap<>();

    /** 派系关系矩阵 */
    private static final Map<Faction, Map<Faction, Faction.Relation>> RELATIONS =
            new HashMap<>();

    private FactionManager() {
    }

    // =========================================================
    // 注册
    // =========================================================

    /**
     * 注册一个派系（对玩家中立）。
     *
     * @param id    派系 id，小写下划线，例如 "empire"
     * @param color 派系主题色，0xAARRGGBB 或 0xRRGGBB
     *
     * @return 派系实例
     */
    public static Faction register(
            String id,
            int color
    ) {

        return register(
                id,
                color,
                false
        );
    }

    /**
     * 注册一个派系。
     *
     * @param id               派系 id，小写下划线，例如 "empire"
     * @param color            派系主题色
     * @param hostileToPlayers 是否主动索敌玩家
     *
     * @return 派系实例
     */
    public static Faction register(
            String id,
            int color,
            boolean hostileToPlayers
    ) {

        Faction faction =
                new Faction(
                        id,
                        "faction.alone_adventure." + id,
                        color,
                        hostileToPlayers
                );

        FACTIONS.put(id, faction);

        return faction;
    }

    /**
     * 把若干实体类型划入某个派系。
     *
     * 简写形式：默认近战、1 级。
     *
     * 用于原版生物，或暂时没有实现
     * IFactionMob 的模组生物。
     */
    @SafeVarargs
    public static void registerMember(
            Faction faction,
            EntityType<?>... types
    ) {

        registerMember(
                faction,
                AttackType.MELEE,
                MobTier.TIER_1,
                types
        );
    }

    /**
     * 把若干实体类型划入某个派系，
     * 同时登记攻击方式与等级。
     */
    @SafeVarargs
    public static void registerMember(
            Faction faction,
            AttackType attackType,
            MobTier tier,
            EntityType<?>... types
    ) {

        for (EntityType<?> type : types) {

            TYPE_CLASSIFICATIONS.put(
                    type,
                    new MobClassification(
                            faction,
                            attackType,
                            tier
                    )
            );
        }
    }

    // =========================================================
    // 查询
    // =========================================================

    /**
     * 按 id 查询派系。
     */
    @Nullable
    public static Faction getFaction(
            String id
    ) {

        return FACTIONS.get(id);
    }

    /**
     * 查询一个生物属于哪个派系。
     *
     * 优先级：
     *
     * 1. 实现了 IFactionMob 的生物，以其自报派系为准
     * 2. 按实体类型注册表查询
     * 3. 都没有 -> null（无派系）
     */
    @Nullable
    public static Faction getFaction(
            LivingEntity entity
    ) {

        if (entity instanceof IFactionMob factionMob) {
            return factionMob.getFaction();
        }

        MobClassification classification =
                TYPE_CLASSIFICATIONS.get(entity.getType());

        return classification != null
                ? classification.getFaction()
                : null;
    }

    /**
     * 查询一个生物的攻击方式。
     *
     * 未划入派系体系的生物返回 null。
     */
    @Nullable
    public static AttackType getAttackType(
            LivingEntity entity
    ) {

        if (entity instanceof IFactionMob factionMob) {
            return factionMob.getAttackType();
        }

        MobClassification classification =
                TYPE_CLASSIFICATIONS.get(entity.getType());

        return classification != null
                ? classification.getAttackType()
                : null;
    }

    /**
     * 查询一个生物的等级。
     *
     * 未划入派系体系的生物返回 null。
     */
    @Nullable
    public static MobTier getTier(
            LivingEntity entity
    ) {

        if (entity instanceof IFactionMob factionMob) {
            return factionMob.getTier();
        }

        MobClassification classification =
                TYPE_CLASSIFICATIONS.get(entity.getType());

        return classification != null
                ? classification.getTier()
                : null;
    }

    // =========================================================
    // 关系
    // =========================================================

    /**
     * 设置两个派系的关系，双向生效。
     *
     * 不设置时使用默认值：
     * 同派系同盟，不同派系敌对。
     */
    public static void setRelation(
            Faction a,
            Faction b,
            Faction.Relation relation
    ) {

        RELATIONS
                .computeIfAbsent(a, key -> new HashMap<>())
                .put(b, relation);

        RELATIONS
                .computeIfAbsent(b, key -> new HashMap<>())
                .put(a, relation);
    }

    /**
     * 查询两个派系的关系。
     *
     * 任一方无派系 -> null。
     */
    @Nullable
    public static Faction.Relation getRelation(
            @Nullable Faction a,
            @Nullable Faction b
    ) {

        if (a == null || b == null) {
            return null;
        }

        if (a == b) {
            return Faction.Relation.ALLY;
        }

        Faction.Relation relation =
                RELATIONS
                        .getOrDefault(a, Map.of())
                        .get(b);

        return relation != null
                ? relation
                : Faction.Relation.HOSTILE;
    }

    /**
     * 两个派系是否同盟。
     */
    public static boolean isAlly(
            @Nullable Faction a,
            @Nullable Faction b
    ) {

        return getRelation(a, b)
                == Faction.Relation.ALLY;
    }

    /**
     * 两个派系是否敌对。
     */
    public static boolean isHostile(
            @Nullable Faction a,
            @Nullable Faction b
    ) {

        return getRelation(a, b)
                == Faction.Relation.HOSTILE;
    }

    /**
     * 两个生物是否同阵营。
     */
    public static boolean isAlly(
            LivingEntity a,
            LivingEntity b
    ) {

        return isAlly(
                getFaction(a),
                getFaction(b)
        );
    }

    /**
     * 两个生物是否敌对阵营。
     */
    public static boolean isHostile(
            LivingEntity a,
            LivingEntity b
    ) {

        return isHostile(
                getFaction(a),
                getFaction(b)
        );
    }
}
