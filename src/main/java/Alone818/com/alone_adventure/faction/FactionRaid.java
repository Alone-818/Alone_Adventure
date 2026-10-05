package Alone818.com.alone_adventure.faction;

import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.init.ModFactionEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 一次派系突袭（对单个玩家）。
 *
 * 生命周期：
 *
 * 1. RaidManager 触发（周期检测 / 短时间连杀）
 * 2. 开场准备（10 秒，血条出现）
 * 3. 逐波进攻：波次间休整 20 秒
 * 4. 全部波次肃清 -> 胜利（仇恨清零，见 RaidManager）
 * 5. 玩家死亡超过 7 次 / 超出 2 小时 -> 失败
 *
 * 波次：
 *
 * 基础 3 波，最高 7 波
 * （仇恨每超出临界 80 点 +1 波）。
  * 每波生成 6 ~ 15 名派系生物（6 + (波数 - 1) x 1.5），
 * 军衔构成由 波次 + 玩家威胁等级 双轴决定
 * （见 pickTier）：
 * 波次推进一阶军衔概率 +25%，
 * 威胁等级每级 +10%，最高 80%；
 * 二阶军衔从威胁 3 级起出现；
 * 最终波固定追加一名最高军衔统领。
 *
 * 防拖延机制：
 *
 * 走失的突袭生物（距离过远）每 10 秒
 * 传送回玩家附近；全部生物进入未加载区块
 * 超过 30 秒则视为走失，空降一支新的搜查队。
 *
 * 时限只统计玩家在线的时间，
 * 下线挂起，上线继续。
 */
public class FactionRaid {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    /** 突袭生物的持久数据标记（调试 / 清理用） */
    public static final String RAID_MOB_TAG =
            "AloneAdventureRaidMob";

    /** 走失生物召回半径（格） */
    private static final double RECALL_DISTANCE = 64.0D;

    /** 生成半径（格） */
    private static final double SPAWN_MIN = 24.0D;
    private static final double SPAWN_MAX = 40.0D;

    /** 被突袭玩家 */
    private final UUID playerId;

    /** 发起突袭的派系 */
    private final Faction faction;

    /** 总波次（3 ~ 7） */
    private final int totalWaves;

    /** 已肃清的波次 */
    private int completedWaves = 0;

    /** 玩家在突袭中的死亡次数 */
    private int deaths = 0;

    /** 已进行的 tick 数（只统计在线时间） */
    private long elapsedTicks = 0L;

    /** 休整倒计时：>0 休整中，==0 当前波进行中 */
    private int intermission;

    /** 当前波的生物数量（算血条进度用） */
    private int waveMobTotal = 0;

    /** 当前波存活生物数（每 tick 扫描更新） */
    private int waveMobAlive = 0;

    /** 全部当前波生物未加载的持续 tick 数 */
    private int lostTicks = 0;

    /** 当前波生物 UUID */
    private final List<UUID> waveMobs =
            new ArrayList<>();

    /** 突袭血条（服务端），玩家上线后懒创建 */
    @Nullable
    private ServerBossEvent bossBar;

    /** 上次发到客户端的波次号（没变就不重发标题包） */
    private int lastBarWave = -1;

    /** 上次发到客户端的存活数（没变就不重发标题包） */
    private int lastBarAlive = -1;

    /** 上次发到客户端的进度（没变就不重发进度包） */
    private float lastBarProgress = -1.0F;

    FactionRaid(
            UUID playerId,
            Faction faction,
            int totalWaves
    ) {

        this.playerId = playerId;
        this.faction = faction;
        this.totalWaves =
                Mth.clamp(
                        totalWaves,
                        RaidManager.RAID_BASE_WAVES,
                        RaidManager.RAID_MAX_WAVES
                );

        // 开场先准备，再上第一波
        this.intermission =
                RaidManager.PREPARE_TICKS;
    }

    UUID getPlayerId() {
        return playerId;
    }

    Faction getFaction() {
        return faction;
    }

    int getTotalWaves() {
        return totalWaves;
    }

    int getDeaths() {
        return deaths;
    }

    // =========================================================
    // 主循环
    // =========================================================

    /**
     * 每 tick 调用（玩家在线时）。
     */
    void tick(ServerPlayer player) {

        if (bossBar == null) {
            createBossBar(player);
        }

        elapsedTicks++;

        // 超时失败
        if (elapsedTicks
                > RaidManager.RAID_DURATION_TICKS) {

            RaidManager.failRaid(
                    player,
                    this,
                    RaidManager.FailReason.TIME
            );

            return;
        }

        if (intermission > 0) {

            intermission--;

            if (intermission == 0) {
                spawnWave(player);
            }

        } else {
            tickWave(player);
        }

        updateBossBar();

        // 每 10 秒召回走失的突袭生物
        if (elapsedTicks % 200L == 0L) {
            recallStrayedMobs(player);
        }
    }

    /**
     * 当前波结算：
     * 全部生物确认死亡 -> 肃清，进入下一波或胜利。
     */
    private void tickWave(ServerPlayer player) {

        waveMobAlive = scanWave(player);

        if (waveMobAlive > 0) {
            lostTicks = 0;
            return;
        }

        // 全部确认死亡：本波肃清
        if (waveMobs.isEmpty()) {

            completedWaves++;

            RaidManager.markDirty();

            player.sendSystemMessage(
                    Component.translatable(
                            "raid.alone_adventure.wave_clear",
                            completedWaves
                    )
            );

            player.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.2F
            );

            LOGGER.info(
                    "[Faction] raid wave {}/{} cleared (player {}, faction {})",
                    completedWaves,
                    totalWaves,
                    player.getName().getString(),
                    faction.getId()
            );

            if (completedWaves >= totalWaves) {
                RaidManager.succeedRaid(player, this);
                return;
            }

            intermission =
                    RaidManager.WAVE_INTERMISSION_TICKS;

            return;
        }

        /*
         * 有生物既确认不了死亡也找不到：
         * 所在区块没加载（玩家跑远了）。
         * 持续超过时限就空降一支新的搜查队，
         * 防止波次卡死刷满 2 小时。
         */
        lostTicks++;

        if (lostTicks
                >= RaidManager.LOST_WAVE_TIMEOUT_TICKS) {

            player.sendSystemMessage(
                    Component.translatable(
                            "raid.alone_adventure.reinforcement"
                    )
            );

            waveMobs.clear();

            spawnWave(player);
        }
    }

    /**
     * 玩家在突袭中死亡。
     */
    void onPlayerDeath(ServerPlayer player) {

        deaths++;

        RaidManager.markDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "raid.alone_adventure.death_count",
                        deaths,
                        RaidManager.MAX_DEATHS
                )
        );

        if (deaths > RaidManager.MAX_DEATHS) {
            RaidManager.failRaid(
                    player,
                    this,
                    RaidManager.FailReason.DEATHS
            );
        }
    }

    // =========================================================
    // 生成
    // =========================================================

    /**
     * 生成下一波突袭部队。
     */
    private void spawnWave(ServerPlayer player) {

        ServerLevel level =
                player.serverLevel();

        RandomSource random =
                player.getRandom();

        int waveNumber =
                completedWaves + 1;

        // 第 1 波 6 名，第 7 波 15 名
        int count =
                6 + Math.round(
                        (waveNumber - 1) * 1.5F
                );

        int threat =
                RaidManager.getThreatLevel(
                        playerId
                );

        for (int i = 0; i < count; i++) {

            spawnMob(
                    player,
                    level,
                    random,
                    MobClass.random(random),
                    pickTier(
                            random,
                            waveNumber,
                            threat
                    )
            );
        }

        // 最终波追加一名最高军衔统领
        if (waveNumber == totalWaves) {

            spawnMob(
                    player,
                    level,
                    random,
                    MobClass.random(random),
                    PromotionTier.ELITE
            );
        }

        waveMobTotal =
                Math.max(1, waveMobs.size());

        waveMobAlive = waveMobTotal;

        lostTicks = 0;

        RaidManager.markDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "raid.alone_adventure.wave_start",
                        waveNumber,
                        waveMobs.size()
                )
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.RAID_HORN.value(),
                SoundSource.HOSTILE,
                1.2F,
                0.9F
        );

        LOGGER.info(
                "[Faction] raid wave {} spawned: {} mobs (player {}, faction {})",
                waveNumber,
                waveMobs.size(),
                player.getName().getString(),
                faction.getId()
        );
    }

    /**
     * 波次军衔掷骰（波次 + 威胁等级双轴）：
     *
     * - 一阶军衔概率 =
     *   min(80%, max(0, 波次 - 2) x 25% + 威胁 x 10%)
     *   威胁 0 级时第 3 波 25%，与旧版一致
     * - 二阶军衔从威胁 3 级起出现，
     *   概率 = (威胁 - 2) x 10%
     *
     * 仇恨决定波数（频率轴），
     * 威胁等级决定强度（升级轴）。
     */
    private PromotionTier pickTier(
            RandomSource random,
            int waveNumber,
            int threatLevel
    ) {

        float advancedChance =
                Math.min(
                        0.80F,
                        Math.max(
                                0,
                                waveNumber - 2
                        ) * 0.25F
                                + threatLevel * 0.10F
                );

        float eliteChance =
                threatLevel >= 3
                        ? (threatLevel - 2) * 0.10F
                        : 0.0F;

        float roll =
                random.nextFloat();

        if (roll < eliteChance) {
            return PromotionTier.ELITE;
        }

        if (roll < eliteChance + advancedChance) {
            return PromotionTier.ADVANCED;
        }

        return PromotionTier.BASE;
    }

    /**
     * 在玩家周围生成一名突袭生物。
     *
     * @return 是否生成成功
     */
    private boolean spawnMob(
            ServerPlayer player,
            ServerLevel level,
            RandomSource random,
            MobClass mobClass,
            PromotionTier tier
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

        FactionMobEntity mob =
                entry.getType()
                        .get()
                        .create(level);

        if (mob == null) {
            return false;
        }

        BlockPos pos =
                findSpawnPos(
                        player,
                        level,
                        random
                );

        mob.moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        // 直接锁定被突袭的玩家
        mob.setTarget(player);

        // 突袭成员：全身发光 + 派系主题色
        // （见 FactionMobRenderer.FactionRaidGlowLayer）
        mob.setRaidMember(true);

        // 原版发光标记（隔墙可见的白色描边）
        mob.setGlowingTag(true);

        mob.getPersistentData()
                .putBoolean(
                        RAID_MOB_TAG,
                        true
                );

        level.addFreshEntity(mob);

        waveMobs.add(
                mob.getUUID()
        );

        return true;
    }

    /**
     * 找一个生成点：玩家周围 24 ~ 40 格的地表。
     *
     * 突袭波次与巡逻战团共用
     * （PatrolManager 在此之上再避开蘑菇岛）。
     *
     * 高度图结果离玩家太远时（例如下界高度图
     * 顶到基岩层顶部）回落到玩家所在高度。
     */
    static BlockPos findSpawnPos(
            ServerPlayer player,
            ServerLevel level,
            RandomSource random
    ) {

        double angle =
                random.nextDouble()
                        * Math.PI
                        * 2.0D;

        double distance =
                SPAWN_MIN
                        + random.nextDouble()
                        * (SPAWN_MAX - SPAWN_MIN);

        int x =
                Mth.floor(
                        player.getX()
                                + Math.cos(angle)
                                * distance
                );

        int z =
                Mth.floor(
                        player.getZ()
                                + Math.sin(angle)
                                * distance
                );

        BlockPos pos =
                level.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING,
                        new BlockPos(x, 0, z)
                );

        if (Math.abs(
                pos.getY() - player.getY())
                > 24.0D) {

            return new BlockPos(
                    x,
                    player.getBlockY(),
                    z
            );
        }

        return pos;
    }

    // =========================================================
    // 走失召回
    // =========================================================

    /**
     * 按 UUID 找突袭生物：
     * 优先玩家所在维度，找不到再查其他维度。
     *
     * 未加载（区块卸载）的生物返回 null，
     * 与"死亡"区分开。
     */
    @Nullable
    private static Entity findMob(
            MinecraftServer server,
            ServerPlayer player,
            UUID id
    ) {

        Entity entity =
                player.serverLevel()
                        .getEntity(id);

        if (entity != null) {
            return entity;
        }

        for (ServerLevel level : server.getAllLevels()) {

            entity = level.getEntity(id);

            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    /**
     * 每 10 秒：
     * 走失的突袭生物传送回玩家附近继续追击；
     * 跨维度的弃旧补新。
     */
    private void recallStrayedMobs(ServerPlayer player) {

        MinecraftServer server =
                player.getServer();

        if (server == null) {
            return;
        }

        for (UUID id : List.copyOf(waveMobs)) {

            Entity entity =
                    findMob(
                            server,
                            player,
                            id
                    );

            if (entity == null
                    || !entity.isAlive()) {
                continue;
            }

            // 跨维度：旧生物追不上，原地补一名新的
            if (entity.level()
                    != player.level()) {

                entity.discard();

                waveMobs.remove(id);

                spawnMob(
                        player,
                        player.serverLevel(),
                        player.getRandom(),
                        MobClass.random(
                                player.getRandom()
                        ),
                        PromotionTier.BASE
                );

                continue;
            }

            // 距离过远：传送回玩家附近
            if (entity.distanceToSqr(player)
                    > RECALL_DISTANCE
                    * RECALL_DISTANCE) {

                BlockPos pos =
                        findSpawnPos(
                                player,
                                player.serverLevel(),
                                player.getRandom()
                        );

                entity.teleportTo(
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D
                );
            }
        }
    }

    /**
     * 扫描当前波：
     * 清掉确认死亡的 UUID，返回存活数。
     *
     * 未加载（区块卸载）的生物保留追踪，
     * 由 tickWave 的走失超时兜底。
     */
    private int scanWave(ServerPlayer player) {

        MinecraftServer server =
                player.getServer();

        if (server == null) {
            return waveMobs.size();
        }

        int alive = 0;

        Iterator<UUID> iterator =
                waveMobs.iterator();

        while (iterator.hasNext()) {

            Entity entity =
                    findMob(
                            server,
                            player,
                            iterator.next()
                    );

            if (entity == null) {
                continue;
            }

            if (!entity.isAlive()) {
                iterator.remove();
                continue;
            }

            alive++;
        }

        return alive;
    }

    // =========================================================
    // 血条
    // =========================================================

    /**
     * 创建突袭血条（颜色由派系定义）。
     */
    private void createBossBar(ServerPlayer player) {

        bossBar =
                new ServerBossEvent(
                        bossBarTitle(),
                        faction.getRaidBarColor(),
                        BossEvent.BossBarOverlay.PROGRESS
                );

        bossBar.addPlayer(player);
    }

    /**
     * 血条标题：
     * "<派系>突袭 · 第 X/Y 波 · 剩余 N 人"。
     *
     * 休整中（下一波还没上）显示 0。
     */
    private Component bossBarTitle() {

        return Component.translatable(
                "raid.alone_adventure.bossbar",
                faction.getDisplayName(),
                Math.min(
                        completedWaves + 1,
                        totalWaves
                ),
                totalWaves,
                waveMobAlive
        );
    }

    /**
     * 刷新血条进度：
     * (已肃清波次 + 当前进度) / 总波次。
     *
     * 只在有变化时发包，避免每 tick 刷屏。
     */
    private void updateBossBar() {

        if (bossBar == null) {
            return;
        }

        float waveFraction =
                intermission == 0
                        && waveMobTotal > 0
                        ? 1.0F
                        - (float) waveMobAlive
                        / waveMobTotal
                        : 0.0F;

        float progress =
                (completedWaves + waveFraction)
                        / totalWaves;

        progress =
                Mth.clamp(
                        progress,
                        0.0F,
                        1.0F
                );

        int wave =
                Math.min(
                        completedWaves + 1,
                        totalWaves
                );

        if (wave != lastBarWave
                || waveMobAlive
                != lastBarAlive) {

            lastBarWave = wave;

            lastBarAlive = waveMobAlive;

            bossBar.setName(
                    bossBarTitle()
            );
        }

        if (progress != lastBarProgress) {

            lastBarProgress = progress;

            bossBar.setProgress(progress);
        }
    }

    /**
     * 玩家上线：重建血条连接
     * （重登后 ServerPlayer 是新实例）。
     */
    void showBossBar(ServerPlayer player) {

        if (bossBar == null) {
            return;
        }

        bossBar.removeAllPlayers();

        bossBar.addPlayer(player);
    }

    /**
     * 玩家下线：移除血条。
     */
    void hideBossBar(ServerPlayer player) {

        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    // =========================================================
    // 收尾
    // =========================================================

    /**
     * 突袭结束（胜败都一样）：
     * 移除血条、清掉残余的突袭生物。
     */
    void terminate(ServerPlayer player) {

        hideBossBar(player);

        bossBar = null;

        MinecraftServer server =
                player.getServer();

        if (server != null) {

            for (UUID id : waveMobs) {

                Entity entity =
                        findMob(
                                server,
                                player,
                                id
                        );

                if (entity != null) {
                    entity.discard();
                }
            }
        }

        waveMobs.clear();

        waveMobTotal = 0;

        waveMobAlive = 0;
    }

    // =========================================================
    // NBT
    // =========================================================

    /**
     * 写入存档。
     */
    CompoundTag writeNBT() {

        CompoundTag tag =
                new CompoundTag();

        tag.putUUID(
                "Player",
                playerId
        );

        tag.putString(
                "Faction",
                faction.getId()
        );

        tag.putInt(
                "TotalWaves",
                totalWaves
        );

        tag.putInt(
                "CompletedWaves",
                completedWaves
        );

        tag.putInt(
                "Deaths",
                deaths
        );

        tag.putLong(
                "ElapsedTicks",
                elapsedTicks
        );

        tag.putInt(
                "Intermission",
                intermission
        );

        tag.putInt(
                "WaveMobTotal",
                waveMobTotal
        );

        ListTag mobs =
                new ListTag();

        for (UUID id : waveMobs) {
            mobs.add(
                    StringTag.valueOf(
                            id.toString()
                    )
            );
        }

        tag.put("WaveMobs", mobs);

        return tag;
    }

    /**
     * 从存档读取，派系已不存在时返回 null。
     */
    @Nullable
    static FactionRaid readNBT(CompoundTag tag) {

        if (!tag.hasUUID("Player")) {
            return null;
        }

        Faction faction =
                FactionManager.getFaction(
                        tag.getString("Faction")
                );

        if (faction == null) {
            return null;
        }

        FactionRaid raid =
                new FactionRaid(
                        tag.getUUID("Player"),
                        faction,
                        tag.getInt("TotalWaves")
                );

        raid.completedWaves =
                Math.max(
                        0,
                        tag.getInt("CompletedWaves")
                );

        raid.deaths =
                Math.max(
                        0,
                        tag.getInt("Deaths")
                );

        raid.elapsedTicks =
                Math.max(
                        0L,
                        tag.getLong("ElapsedTicks")
                );

        raid.intermission =
                Math.max(
                        0,
                        tag.getInt("Intermission")
                );

        raid.waveMobTotal =
                Math.max(
                        1,
                        tag.getInt("WaveMobTotal")
                );

        ListTag mobs =
                tag.getList(
                        "WaveMobs",
                        Tag.TAG_STRING
                );

        for (int i = 0; i < mobs.size(); i++) {

            try {

                raid.waveMobs.add(
                        UUID.fromString(
                                mobs.getString(i)
                        )
                );

            } catch (IllegalArgumentException ignored) {
            }
        }

        return raid;
    }
}
