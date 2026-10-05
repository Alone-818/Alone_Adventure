package Alone818.com.alone_adventure.faction;

import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.init.ModFactionEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 巡逻战团系统：派系生物的主力刷新方式。
 *
 * 派系生物的投放分三层：
 *
 * 1. 巡逻战团（主力）：每 8 ~ 12 分钟在玩家
 *    周围 24 ~ 40 格生成一支同派系战团，
 *    与突袭波次完全相同的生成方式；
 *    规模与军衔随威胁等级成长（见 RaidManager）
 * 2. 突袭（大事件）：仇恨驱动，见 RaidManager
 * 3. 自然生成（少量补充）：低权重常规刷怪，
 *    见 faction_natural_spawns.json
 *
 * 巡逻规则：
 *
 * - 突袭进行中不巡逻（避免两线作战）
 * - 蘑菇岛（原版无怪生物群系）不巡逻
 * - 和平 / 创造 / 旁观 / 已死亡时跳过本次
 * - 离线期间错过的巡逻不补刷，上线重排
 *   （防止上线即被战团贴脸）
 *
 * 派系选择按仇恨加权（10 + 仇恨 / 25）：
 * 打得越狠的派系巡逻越频繁，
 * 与突袭的仇恨驱动一脉相承。
 *
 * 巡逻战团的行为：
 *
 * - 敌对派系（含仇恨过线的中立派系）：
 *   落地锁定玩家 + 号角预警（音量低于突袭）
 * - 中立派系：落地游荡，
 *   与敌对派系的战团互相开战
 *   （FactionEnemyTargetGoal）
 *
 * 战团规模与军衔（威胁等级驱动）：
 *
 * - 规模 = 3 + min(威胁等级, 3)，即 3 ~ 6 名
 * - 军衔由 RaidManager.rollAmbientTier 掷骰：
 *   威胁 0 级全基础兵（新手期不被高强度伏击），
 *   威胁 4 级 60% 一阶军衔 + 20% 二阶军衔
 *
 * 巡逻生物与自然生成一样随距离消失
 * （保持怪物上限流动，走远即散），
 * 不像突袭生物那样常驻追击。
 */
public final class PatrolManager {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 巡逻最短间隔：8 分钟 */
    public static final long MIN_INTERVAL_TICKS =
            9_600L;

    /** 巡逻最长间隔：12 分钟 */
    public static final long MAX_INTERVAL_TICKS =
            14_400L;

    /**
     * 迟到豁免：到期超过 1 分钟视为离线期间错过，
     * 不补刷。轮询粒度 10 秒，
     * 在线玩家的到期最多迟到 10 秒。
     */
    private static final long OVERDUE_SKIP_TICKS =
            1_200L;

    /** 派系选择的基础权重（无仇恨也人人有份） */
    private static final int FACTION_BASE_WEIGHT = 10;

    /** 仇恨权重换算：每 25 点仇恨 +1 权重 */
    private static final int HATRED_WEIGHT_DIVISOR =
            25;

    /**
     * 蘑菇岛落点重试次数：
     * 玩家贴着蘑菇岛边缘时向另一侧重新取点。
     */
    private static final int MUSHROOM_RETRY = 6;

    private PatrolManager() {
    }

    // =========================================================
    // 计划
    // =========================================================

    /**
     * 玩家上线：
     * 没有巡逻计划时从现在起排一个完整间隔。
     */
    static void onPlayerLogin(
            ServerPlayer player
    ) {

        RaidSavedData data =
                RaidManager.savedData();

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        if (data.nextPatrol.containsKey(playerId)) {
            return;
        }

        data.nextPatrol.put(
                playerId,
                player.serverLevel()
                        .getGameTime()
                        + randomInterval(
                        player.getRandom()
                )
        );

        data.setDirty();
    }

    /**
     * 每 10 秒轮询（由 RaidManager.tick 调用）：
     * 到点且满足条件 -> 派出一支巡逻战团。
     */
    static void checkPatrol(
            ServerPlayer player
    ) {

        RaidSavedData data =
                RaidManager.savedData();

        if (data == null) {
            return;
        }

        // 突袭进行中不巡逻
        if (data.activeRaids.containsKey(
                player.getUUID()
        )) {
            return;
        }

        UUID playerId =
                player.getUUID();

        long now =
                player.serverLevel()
                        .getGameTime();

        Long next =
                data.nextPatrol.get(playerId);

        if (next == null) {

            data.nextPatrol.put(
                    playerId,
                    now + randomInterval(
                            player.getRandom()
                    )
            );

            data.setDirty();

            return;
        }

        if (now < next) {
            return;
        }

        // 无论是否派出，都先排下一次
        data.nextPatrol.put(
                playerId,
                now + randomInterval(
                        player.getRandom()
                )
        );

        data.setDirty();

        // 离线期间错过：不补刷
        if (now - next > OVERDUE_SKIP_TICKS) {
            return;
        }

        ServerLevel level =
                player.serverLevel();

        if (player.isSpectator()
                || player.isCreative()
                || !player.isAlive()
                || level.getDifficulty()
                == Difficulty.PEACEFUL
                || ModFactionEntities.isMushroomIsland(
                level,
                player.blockPosition()
        )) {
            return;
        }

        spawnPatrol(
                player,
                level,
                pickFaction(player)
        );
    }

    /**
     * 派系选择：按仇恨加权随机。
     *
     * 权重 = 10 + 仇恨 / 25：
     * 仇恨 1000（上限）的派系权重 50，
     * 是无仇恨派系的 5 倍。
     */
    private static Faction pickFaction(
            ServerPlayer player
    ) {

        RandomSource random =
                player.getRandom();

        UUID playerId =
                player.getUUID();

        int total = 0;

        for (Faction faction : ModFactions.all()) {
            total += factionWeight(playerId, faction);
        }

        int roll =
                random.nextInt(total);

        for (Faction faction : ModFactions.all()) {

            roll -= factionWeight(playerId, faction);

            if (roll < 0) {
                return faction;
            }
        }

        // 兜底（理论到不了这里）
        return ModFactions.all()[0];
    }

    private static int factionWeight(
            UUID playerId,
            Faction faction
    ) {

        return FACTION_BASE_WEIGHT
                + RaidManager.getHatred(
                playerId,
                faction
        ) / HATRED_WEIGHT_DIVISOR;
    }

    // =========================================================
    // 生成
    // =========================================================

    /**
     * 派出一支巡逻战团。
     *
     * 规模 = 3 + min(威胁等级, 3)，
     * 军衔由威胁等级掷骰（rollAmbientTier）。
     */
    private static void spawnPatrol(
            ServerPlayer player,
            ServerLevel level,
            Faction faction
    ) {

        RandomSource random =
                player.getRandom();

        UUID playerId =
                player.getUUID();

        int threat =
                RaidManager.getThreatLevel(playerId);

        int count =
                3 + Math.min(threat, 3);

        boolean hostile =
                RaidManager.isHostileToPlayer(
                        faction,
                        playerId
                );

        int spawned = 0;

        for (int i = 0; i < count; i++) {

            if (spawnPatrolMob(
                    player,
                    level,
                    random,
                    faction,
                    MobClass.random(random),
                    RaidManager.rollAmbientTier(
                            random,
                            threat
                    ),
                    hostile
            )) {
                spawned++;
            }
        }

        if (spawned == 0) {
            return;
        }

        LOGGER.info(
                "[Faction] patrol spawned: faction={} mobs={} threat={} hostile={} player={}",
                faction.getId(),
                spawned,
                threat,
                hostile,
                player.getName().getString()
        );

        // 敌对战团给预警（号角音量低于突袭）；
        // 中立战团静默游荡，遇到敌对派系才开战
        if (hostile) {

            player.sendSystemMessage(
                    Component.translatable(
                            "patrol.alone_adventure.incoming",
                            faction.getDisplayName()
                    )
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.RAID_HORN.value(),
                    SoundSource.HOSTILE,
                    0.7F,
                    0.8F
            );
        }
    }

    /**
     * 生成一名巡逻战团成员：
     * 与突袭同款落点（24 ~ 40 格地表），
     * 但避开蘑菇岛。
     *
     * @return 是否生成成功
     */
    private static boolean spawnPatrolMob(
            ServerPlayer player,
            ServerLevel level,
            RandomSource random,
            Faction faction,
            MobClass mobClass,
            PromotionTier tier,
            boolean hostile
    ) {

        ModFactionEntities.FactionMobEntry entry =
                ModFactionEntities.entry(
                        faction,
                        mobClass,
                        tier
                );

        if (entry == null) {
            return false;
        }

        BlockPos pos =
                findPatrolPos(
                        player,
                        level,
                        random
                );

        if (pos == null) {
            return false;
        }

        FactionMobEntity mob =
                entry.getType()
                        .get()
                        .create(level);

        if (mob == null) {
            return false;
        }

        mob.moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        // 敌对战团落地锁敌；中立战团游荡
        if (hostile) {
            mob.setTarget(player);
        }

        // 与自然生成一致：随距离消失，保持怪物上限流动
        mob.markDespawnable();

        level.addFreshEntity(mob);

        return true;
    }

    /**
     * 巡逻落点：与突袭同款
     * （玩家周围 24 ~ 40 格地表），
     * 全部尝试都落在蘑菇岛上时返回 null
     * （玩家紧贴蘑菇岛边缘的极端情况，
     * 本名成员放弃生成）。
     */
    @Nullable
    private static BlockPos findPatrolPos(
            ServerPlayer player,
            ServerLevel level,
            RandomSource random
    ) {

        for (int attempt = 0;
             attempt < MUSHROOM_RETRY;
             attempt++) {

            BlockPos pos =
                    FactionRaid.findSpawnPos(
                            player,
                            level,
                            random
                    );

            if (!ModFactionEntities.isMushroomIsland(
                    level,
                    pos
            )) {
                return pos;
            }
        }

        return null;
    }

    // =========================================================
    // 工具
    // =========================================================

    /**
     * 随机巡逻间隔：8 ~ 12 分钟。
     */
    private static long randomInterval(
            RandomSource random
    ) {

        return MIN_INTERVAL_TICKS
                + random.nextInt(
                (int) (MAX_INTERVAL_TICKS
                        - MIN_INTERVAL_TICKS)
        );
    }
}
