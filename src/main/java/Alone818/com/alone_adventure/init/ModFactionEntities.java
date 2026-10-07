package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.entity.faction.FactionArcher;
import Alone818.com.alone_adventure.entity.faction.FactionMage;
import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.entity.faction.FactionSoldier;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.MobClass;
import Alone818.com.alone_adventure.faction.PromotionTier;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 派系系列生物。
 *
 * 每个派系三个职业，每个职业三个军衔：
 *
 * 士兵（近战）：士兵 -> 中士 -> 将军
 * 弓手（远程）：弓手 -> 弩手 -> 连弩手
 * 法师（辅助）：法师 -> 大法师 -> 魔导师
 *
 * 军衔是独立注册的实体类型
 * （不同的生物类型），
 * 3 级再击杀一名敌人时原地蜕变晋升，
 * 详见 FactionMobEntity。
 *
 * 实体 id = 派系id_军衔名，例如：
 *
 * empire_soldier / empire_sergeant / empire_general
 * empire_archer / empire_crossbowman / empire_repeater
 * empire_mage / empire_archmage / empire_magus
 *
 * 全部 4 派系 x 3 职业 x 3 军衔 = 36 个实体
 * 在这里批量展开注册；
 * 属性、渲染器、生成蛋按 allEntries() 批量挂。
 *
 * 以后新派系加生物：
 * 在 ModFactions 注册派系即可，
 * 这里自动展开整条军衔线。
 */
public class ModFactionEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.ENTITY_TYPES,
                    Alone_adventure.MODID
            );

    /**
     * 职业实体工厂：
     * 按军衔补全构造参数。
     */
    private interface MobFactory<E extends FactionMobEntity> {

        E create(
                EntityType<? extends Monster> type,
                Level level,
                Faction faction,
                PromotionTier tier,
                @Nullable Supplier<EntityType<? extends FactionMobEntity>> nextTierType
        );
    }

    /**
     * 单个派系生物条目：
     * 实体注册表项 + 派系 / 职业 / 军衔归属，
     * 供属性、渲染器、生成蛋批量展开。
     */
    public static final class FactionMobEntry {

        private final RegistryObject<? extends EntityType<? extends FactionMobEntity>> type;
        private final Faction faction;
        private final MobClass mobClass;
        private final PromotionTier tier;

        FactionMobEntry(
                RegistryObject<? extends EntityType<? extends FactionMobEntity>> type,
                Faction faction,
                MobClass mobClass,
                PromotionTier tier
        ) {
            this.type = type;
            this.faction = faction;
            this.mobClass = mobClass;
            this.tier = tier;
        }

        /**
         * 实体注册表项。
         */
        public RegistryObject<? extends EntityType<? extends FactionMobEntity>> getType() {
            return type;
        }

        /**
         * 所属派系。
         */
        public Faction getFaction() {
            return faction;
        }

        /**
         * 职业。
         */
        public MobClass getMobClass() {
            return mobClass;
        }

        /**
         * 军衔。
         */
        public PromotionTier getTier() {
            return tier;
        }

        /**
         * 实体 id，例如 "empire_sergeant"。
         */
        public String getEntityId() {
            return faction.getId()
                    + "_"
                    + mobClass.getSuffix(tier);
        }
    }

    /** 全部条目，key 见 {@link #key} */
    private static final Map<String, FactionMobEntry> ENTRIES =
            new LinkedHashMap<>();

    /*
     * 4 派系 x 3 职业 x 3 军衔批量注册。
     */
    static {

        for (Faction faction : ModFactions.all()) {

            registerLine(
                    faction,
                    MobClass.SOLDIER,
                    FactionSoldier::new
            );

            registerLine(
                    faction,
                    MobClass.ARCHER,
                    FactionArcher::new
            );

            registerLine(
                    faction,
                    MobClass.MAGE,
                    FactionMage::new
            );
        }
    }

    /**
     * 注册一条军衔线（一个职业的全部军衔）。
     */
    private static <E extends FactionMobEntity> void registerLine(
            Faction faction,
            MobClass mobClass,
            MobFactory<E> factory
    ) {

        for (PromotionTier tier : PromotionTier.values()) {

            String id =
                    faction.getId()
                            + "_"
                            + mobClass.getSuffix(tier);

            /*
             * 下一军衔的实体类型：
             * 晋升发生在运行时，
             * 那时全部条目都已注册完毕，
             * 这里延迟到晋升那一刻才解析。
             */
            @Nullable
            Supplier<EntityType<? extends FactionMobEntity>> nextTierType =
                    tier.isMax()
                            ? null
                            : () -> {

                        FactionMobEntry next =
                                ENTRIES.get(
                                        key(
                                                faction,
                                                mobClass,
                                                tier.next()
                                        )
                                );

                        return next != null
                                ? next.type.get()
                                : null;
                    };

            RegistryObject<EntityType<E>> type =
                    ENTITIES.register(
                            id,
                            () -> EntityType.Builder
                                    .<E>of(
                                            (entityType, level) ->
                                                    factory.create(
                                                            entityType,
                                                            level,
                                                            faction,
                                                            tier,
                                                            nextTierType
                                                    ),
                                            MobCategory.MONSTER
                                    )
                                    .sized(0.6F, 1.95F)
                                    .clientTrackingRange(10)
                                    .build(id)
                    );

            ENTRIES.put(
                    key(faction, mobClass, tier),
                    new FactionMobEntry(
                            type,
                            faction,
                            mobClass,
                            tier
                    )
            );
        }
    }

    /**
     * 按 派系 + 职业 + 军衔 查条目。
     */
    @Nullable
    public static FactionMobEntry entry(
            Faction faction,
            MobClass mobClass,
            PromotionTier tier
    ) {

        return ENTRIES.get(
                key(faction, mobClass, tier)
        );
    }

    /**
     * 该位置是否属于派系生物的"安全区"
     * （蘑菇岛：原版设计为无怪生物群系）。
     *
     * 安全区内不自然生成、不派巡逻战团，
     * 保持蘑菇岛与原版一致的避难所定位；
     * 突袭不受此限制
     * （突袭是玩家仇恨触发的定向事件，
     * 与原版劫掠可发生在蘑菇岛同理）。
     */
    public static boolean isMushroomIsland(
            LevelReader level,
            BlockPos pos
    ) {

        return level.getBiome(pos)
                .is(Biomes.MUSHROOM_FIELDS);
    }

    /**
     * 全部条目（注册顺序）。
     *
     * 属性、渲染器、生成蛋、创造模式物品栏
     * 都按这个列表批量展开。
     */
    public static List<FactionMobEntry> allEntries() {

        return new ArrayList<>(ENTRIES.values());
    }

    private static String key(
            Faction faction,
            MobClass mobClass,
            PromotionTier tier
    ) {

        return faction.getId()
                + ":"
                + mobClass.name()
                + ":"
                + tier.name();
    }

    // =========================================================
    // 属性
    // =========================================================

    /**
     * 属性注册（模组总线事件），
     * 由主 mod 构造函数挂监听。
     */
    public static void registerAttributes(
            EntityAttributeCreationEvent event
    ) {

        for (FactionMobEntry entry : ENTRIES.values()) {

            event.put(
                    entry.type.get(),
                    attributesFor(entry.mobClass)
                            .build()
            );
        }
    }

    /**
     * 职业对应的基础属性。
     */
    private static AttributeSupplier.Builder attributesFor(
            MobClass mobClass
    ) {

        switch (mobClass) {

            case SOLDIER:
                return FactionSoldier
                        .createAttributes();

            case ARCHER:
                return FactionArcher
                        .createAttributes();

            case MAGE:
            default:
                return FactionMage
                        .createAttributes();
        }
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    // =========================================================
    // 自然生成位置
    // =========================================================

    /**
     * 自然生成位置规则（主 mod 构造函数挂载）。
     *
     * ON_GROUND + MOTION_BLOCKING_NO_LEAVES
     * = 只在地表、站方块上，不在水里生成；
     * 再叠加原版怪物条件（黑暗 + 非和平）。
     *
     * 只有基础军衔参与自然生成
     * （高一档靠击杀蜕变晋升，见 FactionMobEntity）；
     * 士兵是结群队长（见 FactionSpawnEvent），
     * 弓手 / 法师是结群队友，
     * 全部基础兵种都挂上位置规则，
     * 供队友落点检查复用。
     */
    public static void registerSpawnPlacements(
            SpawnPlacementRegisterEvent event
    ) {

        for (FactionMobEntry entry : ENTRIES.values()) {

            if (entry.getTier() != PromotionTier.BASE) {
                continue;
            }

            event.register(
                    entry.getType().get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    ModFactionEntities::checkFactionSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE
            );
        }
    }

    /**
     * 派系生物生成检查：地表 + 不在水里。
     */
    private static boolean checkFactionSpawnRules(
            EntityType<?> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        // 先走原版怪物生成检查（黑暗 + 非和平）
        if (!Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) (EntityType<?>) type, level, spawnType, pos, random)) {
            return false;
        }

        // 检查是否在地下：y 坐标低于海平面（63）视为地下，禁止生成
        if (pos.getY() < 63) {
            return false;
        }

        // 检查脚下是否是水：如果是水则禁止生成
        if (!level.getBlockState(pos.below()).isAir()
                && level.getBlockState(pos.below()).getFluidState().isSource()) {
            return false;
        }

        return true;
    }
}
