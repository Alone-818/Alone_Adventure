package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.entity.faction.FactionSoldier;
import Alone818.com.alone_adventure.faction.MobClass;
import Alone818.com.alone_adventure.faction.PromotionTier;
import Alone818.com.alone_adventure.faction.RaidManager;
import Alone818.com.alone_adventure.init.ModFactionEntities;

import Alone818.com.alone_adventure.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 派系生物自然生成。
 *
 * 派系生物的主力刷新方式：
 *
 * 1. 自然生成（主力）：通过 biome_modifier 刷怪
 *    data/alone_adventure/forge/biome_modifier/faction_natural_spawns.json
 *    每派系权重 25，minCount 2 / maxCount 4
 *    与原版僵尸相当的频率
 *
 * 2. 巡逻战团（稀有事件）：间隔 4-8 分钟在玩家
 *    周围 24-40 格处生成 5-8 人精英结群
 *    详见 PatrolManager
 *
 * 3. 突袭（仇恨驱动）：仇恨>=400 触发
 *    详见 RaidManager
 *
 * 自然生成结群：
 * - 基础兵单独生成（像僵尸一样随机分布）
 * - 精英军衔（一阶/二阶/精英）生成同派系精英结群
 *   1 名精英 + 2-3 名基础兵伴从
 *
 * 蘑菇岛不刷怪：
 * 蘑菇岛是原版无怪生物群系（安全区），
 * 自然 / 区块首刷在这里直接取消；
 * 突袭 / 巡逻战团不受限。
 *
 * 位置规则与原版僵尸一致：
 * MOTION_BLOCKING_NO_LEAVES 高度图 = 只在地表，
 * 再走原版怪物生成检查
 * （站位方块合法 + 黑暗 + 非和平）。
 *
 * 自然生成的生物标记为随距离消失
 * （保持怪物上限流动），
 * 突袭 / 生成蛋生物不受影响，保持常驻。
 *
 * 结群之间由派系关系驱动开战：
 * 不同派系互相主动索敌（FactionEnemyTargetGoal），
 * 同派系支援集火（FactionEvent）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionSpawnEvent {

    /** 每名队友的落点尝试次数 */
    private static final int PLACE_ATTEMPTS = 10;

    /** 落点散布半径（格） */
    private static final int PLACE_RADIUS = 4;

    /** 队长与玩家的最小生成距离：避免在玩家周围 12 格内生成 */
    private static final double MIN_PLAYER_DISTANCE = 12.0D;

    /**
     * 自然生成结算：
     * 蘑菇岛拦截 + 标记可消失 + 精英结群。
     *
     * 生成蛋 / 突袭 / 指令生成不进这里，
     * 保持一只一只生成、且常驻。
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(
            MobSpawnEvent.FinalizeSpawn event
    ) {

        if (!(event.getEntity()
                instanceof FactionMobEntity mob)) {

            return;
        }

        MobSpawnType spawnType =
                event.getSpawnType();

        boolean natural =
                spawnType == MobSpawnType.NATURAL
                        || spawnType
                        == MobSpawnType.CHUNK_GENERATION;

        /*
         * 蘑菇岛安全区：
         * 自然 / 区块首刷在这里直接取消
         * （生物群系修改器覆盖全部主世界，
         * 蘑菇岛需要在这里单独拦截）。
         */
        if (natural
                && ModFactionEntities.isMushroomIsland(
                event.getLevel(),
                mob.blockPosition()
        )) {

            event.setSpawnCancelled(true);

            return;
        }

        /*
         * 威慑战旗区域拦截：
         * 检查生成位置是否在威慑战旗的 5×5 区块范围内。
         */
        if (natural && isWithinDeterrenceRange(event.getLevel(), mob.blockPosition())) {
            event.setSpawnCancelled(true);
            return;
        }

        /*
         * 自然 / 区块首刷 / 刷怪笼生成的
         * 随距离消失，保持怪物上限流动。
         */
        if (natural
                || spawnType == MobSpawnType.SPAWNER) {

            mob.markDespawnable();
        }

        /*
         * 自然生成的精英军衔（非基础兵）生成精英结群：
         * 一阶/二阶/精英兵生成自己的同派系精英结群。
         */
        if (natural
                && mob.getRank() != PromotionTier.BASE) {

            spawnPack(
                    mob,
                    event.getLevel()
            );
        }
    }

    /**
     * 队长落地，补齐同派系结群：
     *
     * 1 名战士 + 随机远程组（2 弓手 / 1 弓手 1 法师）。
     *
     * 队友军衔按附近玩家的威胁等级掷骰，
     * 结群强度随玩家成长（升级机制）。
     */
    private static void spawnPack(
            FactionMobEntity leader,
            ServerLevelAccessor level
    ) {

        RandomSource random =
                level.getRandom();

        /*
         * 威胁等级取队长附近 128 格内的玩家：
         * 自然生成必然发生在某个玩家周围，
         * 找不到（极端情况）按威胁 0 级处理。
         */
        Player nearest =
                leader.level()
                        .getNearestPlayer(
                                leader,
                                128.0D
                        );

        int threat =
                nearest != null
                        ? RaidManager.getThreatLevel(
                        nearest.getUUID()
                )
                        : 0;

        List<MobClass> companions =
                new ArrayList<>();

        // 第二名战士
        companions.add(MobClass.SOLDIER);

        // 必有一名弓手
        companions.add(MobClass.ARCHER);

        // 远程第四人：再一名弓手 或 一名法师
        companions.add(
                random.nextBoolean()
                        ? MobClass.ARCHER
                        : MobClass.MAGE
        );

        for (MobClass mobClass : companions) {

            tryPlacePackMember(
                    leader,
                    level,
                    mobClass,
                    RaidManager.rollAmbientTier(
                            random,
                            threat
                    )
            );
        }
    }

    /**
     * 在队长周围找一个合格的地表落点
     * 生成一名队友；找不到就缺员
     * （结群因此 3 ~ 4 只）。
     *
     * 军衔由调用方按威胁等级掷好传入
     * （队长保持基础军衔，是生成入口）。
     */
    private static void tryPlacePackMember(
            FactionMobEntity leader,
            ServerLevelAccessor level,
            MobClass mobClass,
            PromotionTier tier
    ) {

        ModFactionEntities.FactionMobEntry entry =
                ModFactionEntities.entry(
                        leader.getFaction(),
                        mobClass,
                        tier
                );

        if (entry == null) {
            return;
        }

        EntityType<? extends FactionMobEntity> type =
                entry.getType()
                        .get();

        RandomSource random =
                level.getRandom();

        for (int attempt = 0;
             attempt < PLACE_ATTEMPTS;
             attempt++) {

            int x =
                    Mth.floor(leader.getX())
                            + random.nextInt(
                            PLACE_RADIUS * 2 + 1)
                            - PLACE_RADIUS;

            int z =
                    Mth.floor(leader.getZ())
                            + random.nextInt(
                            PLACE_RADIUS * 2 + 1)
                            - PLACE_RADIUS;

            BlockPos pos =
                    level.getHeightmapPos(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(x, 0, z)
                    );

            /*
             * 检查与玩家的距离：避免在玩家周围 12 格内生成
             */
            Player nearest =
                    leader.level()
                            .getNearestPlayer(
                                    leader,
                                    MIN_PLAYER_DISTANCE
                            );

            if (nearest != null) {
                continue;
            }

            /*
             * 原版同款怪物生成检查：
             * 站位方块合法 + 黑暗 + 非和平；
             * 队友也不落进蘑菇岛
             * （队长可能在蘑菇岛边缘生成）。
             */
            if (!SpawnPlacements.checkSpawnRules(
                    type,
                    level,
                    MobSpawnType.NATURAL,
                    pos,
                    random
            ) || ModFactionEntities.isMushroomIsland(
                    level,
                    pos
            )) {
                continue;
            }

            FactionMobEntity member =
                    type.create(
                            level.getLevel()
                    );

            if (member == null) {
                return;
            }

            member.moveTo(
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F,
                    0.0F
            );

            if (!member.checkSpawnObstruction(level)) {
                continue;
            }

            // 队友同属自然生成，同样随距离消失
            member.markDespawnable();

            level.addFreshEntity(member);

            return;
        }
    }

    /**
     * 检查给定位置是否在威慑战旗的 5×5 区块范围内。
     *
     * @param level 关卡
     * @param pos   要检查的位置
     * @return 如果在威慑范围内返回 true，否则返回 false
     */
    private static boolean isWithinDeterrenceRange(
            net.minecraft.world.level.LevelReader level,
            BlockPos pos
    ) {
        // 获取当前区块坐标
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // 检查周围 5×5 范围内的区块是否有威慑战旗
        // 即检查中心 ±2 的区块范围
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int bannerChunkX = chunkX + dx;
                    int bannerChunkZ = chunkZ + dz;

                    // 检查此区块是否有威慑战旗方块
                    if (hasDeterrenceBannerInChunk(serverLevel, bannerChunkX, bannerChunkZ)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查指定区块内是否有威慑战旗方块。
     *
     * 通过区块的方块实体 (BlockEntity) 缓存快速查找。
     *
     * @param level     关卡
     * @param chunkX    区块 X 坐标
     * @param chunkZ    区块 Z 坐标
     * @return 如果有威慑战旗返回 true，否则返回 false
     */
    private static boolean hasDeterrenceBannerInChunk(
            net.minecraft.server.level.ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        // 检查区块是否已加载
        if (!level.hasChunk(chunkX, chunkZ)) {
            return false;
        }

        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(chunkX, chunkZ);

        for (var blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Alone818.com.alone_adventure.blocks.DeterrenceBannerBlockEntity) {
                return true;
            }
        }

        return false;
    }
}
