package Alone818.com.alone_adventure.faction;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 仇恨-突袭系统的中枢。
 *
 * 常量：
 *
 * - 仇恨范围 0 ~ 1000
 * - 中立 H < 200；敌对 200 <= H < 400；突袭临界 H >= 400
 * - 周期检测每 15 分钟；同派系突袭冷却 60 分钟；
 *   全局突袭冷却 20 分钟
 * - 波次 = 3 + (H - 400) / 80，夹在 3 ~ 7
 * - 失败：死亡超过 7 次（第 8 次死亡），
 *   或突袭开始后超过 2 小时
 *
 * 仇恨值（每玩家独立）：
 *
 * 1. 玩家击杀派系生物：该派系 +20
 * 2. 同时其他派系各 -5
 *    （打一个派系会稍微安抚其他派系）
 *
 * 态度（实时解析，不落盘）：
 *
 * - 恶魔 / 亡灵：永久敌对玩家
 * - 帝国 / 部落（中立派系）：
 *   仇恨 >= 敌对线（200） -> 视为敌对；
 *   降回敌对线以下 -> 恢复中立
 *
 * 突袭触发（每个玩家同时只有一场）：
 *
 * 1. 周期检测：每 15 分钟检测一次仇恨，
 *    仇恨 >= 400 且不在冷却中的派系里
 *    仇恨最高的那个发起突袭；
 *    同时过线的其他派系进入等待队列，
 *    全局冷却（20 分钟）未过则全部排队
 * 2. 短时连杀：20 分钟内击杀同一派系
 *    30 名生物 -> 立即突袭
 *    （仇恨不足 200 时只拉到 200
 *    进入敌对，不立即突袭）
 *
 * 突袭期间仇恨冻结（核心规则）：
 *
 * - 击杀不增加、不减少任何派系仇恨
 * - 不触发短时连杀，不触发周期检测
 * - 突袭中的击杀只用于推进波次、掉落、
 *   任务进度、死亡次数和时间
 * - 结算完成后解除冻结，恢复正常仇恨计算
 * - 例外：威胁等级的累计击杀照常计入
 *   （见下），升级曲线不因突袭中断
 *
 * 威胁等级（升级机制，每玩家独立）：
 *
 * 累计击杀派系生物总数只增不减，
 * 跨突袭、跨下线、跨重启保留，
 * 阈值 50 / 150 / 400 / 900 对应威胁 1 ~ 4 级。
 *
 * 与仇恨的分工：
 *
 * - 仇恨（会回落）决定突袭的"频率与波数"
 * - 威胁等级（只增不减）决定敌人的"强度"：
 *   巡逻战团规模（见 PatrolManager）、
 *   自然结群 / 巡逻战团 / 突袭波次的军衔构成
 *
 * 结算：
 *
 * - 胜利（肃清全部波次）：
 *   本派系仇恨清零，其他派系各 -100
 * - 失败（第 8 次死亡 / 超时 2 小时）：
 *   本派系仇恨减半（下限 200），
 *   其他派系各 +20
 * - 两种结果都让本派系冷却 60 分钟、
 *   全局冷却 20 分钟
 *
 * 全部数据随世界存档（见 RaidSavedData），
 * 仇恨与突袭状态跨下线、跨重启保留。
 */
public final class RaidManager {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    // =========================================================
    // 数值
    // =========================================================

    /** 击杀派系生物：该派系仇恨 +20 */
    public static final int KILL_HATE = 20;

    /** 击杀派系生物：其他派系仇恨各 -5 */
    public static final int KILL_OTHER_DECAY = 5;

    /**
     * 威胁等级阈值：
     * 累计击杀 50 / 150 / 400 / 900 -> 威胁等级 1 ~ 4。
     *
     * 节奏对齐主要玩法循环：
     * 一场突袭约提供 15 ~ 40 次击杀
     * （随波数浮动），外加巡逻战团与
     * 自然结群的零散击杀。
     *
     * - 威胁 I：约 2 ~ 3 场突袭后到手
     *   （早期反馈，让玩家知道系统存在）
     * - 威胁 II：约 6 ~ 8 场突袭
     * - 威胁 III：约 15 ~ 20 场突袭
     * - 威胁 IV：约 35 场以上，
     *   长期目标，对应数十小时的派系战争
     *
     * 曲线近似几何递增（每级约 x2.5），
     * 前快后慢：早期反馈不断档，
     * 高威胁不会过早泛滥。
     */
    public static final int[] THREAT_LEVEL_KILLS =
            {50, 150, 400, 900};

    /** 威胁等级上限（0 ~ 4） */
    public static final int MAX_THREAT_LEVEL =
            THREAT_LEVEL_KILLS.length;

    /** 威胁等级显示名（聊天提示用，1 ~ 4 级） */
    private static final String[] THREAT_NAMES =
            {"I", "II", "III", "IV"};

    /**
     * 环境生成的军衔升级系数：
     * 威胁每升 1 级，一阶军衔概率 +15%，
     * 二阶军衔从威胁 3 级起出现，每级 +10%。
     */
    private static final float AMBIENT_ADVANCED_PER_LEVEL =
            0.15F;

    private static final float AMBIENT_ELITE_PER_LEVEL =
            0.10F;

    /** 二阶军衔（环境生成）从该威胁等级起出现 */
    private static final int AMBIENT_ELITE_MIN_THREAT = 3;

    /** 仇恨上限 */
    public static final int HATRED_CAP = 1000;

    /** 敌对线：仇恨达到即视为敌对（低于突袭临界） */
    public static final int HOSTILE_THRESHOLD = 200;

    /** 突袭临界仇恨 */
    public static final int RAID_THRESHOLD = 400;

    /** 仇恨周期检测间隔：15 分钟 */
    public static final long CHECK_INTERVAL_TICKS =
            18_000L;

    /** 短时连杀窗口：20 分钟 */
    public static final long BURST_WINDOW_TICKS =
            24_000L;

    /** 窗口内击杀该数量 -> 立即突袭 */
    public static final int BURST_KILL_TRIGGER = 30;

    /** 突袭基础波次 */
    public static final int RAID_BASE_WAVES = 3;

    /** 突袭最高波次 */
    public static final int RAID_MAX_WAVES = 7;

    /** 每超出临界多少仇恨 +1 波 */
    public static final int WAVES_PER_EXCESS = 80;

    /** 突袭中允许的最大死亡次数（第 8 次死亡判负） */
    public static final int MAX_DEATHS = 7;

    /** 突袭时限：2 小时（只统计在线时间） */
    public static final long RAID_DURATION_TICKS =
            144_000L;

    /** 同派系突袭冷却：60 分钟 */
    public static final long FACTION_COOLDOWN_TICKS =
            72_000L;

    /** 全局突袭冷却：20 分钟 */
    public static final long GLOBAL_COOLDOWN_TICKS =
            24_000L;

    /** 开场准备时间：10 秒 */
    public static final int PREPARE_TICKS = 200;

    /** 波间休整：20 秒 */
    public static final int WAVE_INTERMISSION_TICKS =
            400;

    /** 当前波全部生物未加载的兜底时限：30 秒 */
    public static final int LOST_WAVE_TIMEOUT_TICKS =
            600;

    /** 胜利：其他派系仇恨各 -100 */
    public static final int SUCCESS_OTHER_REDUCTION = 100;

    /** 失败：其他派系仇恨各 +20 */
    public static final int FAIL_OTHER_INCREASE = 20;

    /** 失败：本派系仇恨减半后的下限 */
    public static final int FAIL_OWN_MIN_HATRED = 200;

    /** 周期检测的轮询粒度：10 秒 */
    private static final long CHECK_POLL_TICKS = 200L;

    /**
     * 突袭失败原因。
     */
    public enum FailReason {

        /** 死亡超过 7 次 */
        DEATHS("raid.alone_adventure.fail_deaths"),

        /** 超出 2 小时时限 */
        TIME("raid.alone_adventure.fail_time");

        final String translationKey;

        FailReason(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    /** 存档实例（服务端运行期间缓存） */
    @Nullable
    private static RaidSavedData data;

    private RaidManager() {
    }

    // =========================================================
    // 服务端生命周期
    // =========================================================

    /**
     * 服务端启动：载入存档。
     */
    public static void initServer(MinecraftServer server) {

        data = RaidSavedData.get(server);

        LOGGER.info(
                "[Faction] raid data loaded: {} players with hatred, {} active raids",
                data.hatred.size(),
                data.activeRaids.size()
        );
    }

    /**
     * 服务端停止：清掉缓存引用。
     */
    public static void serverStopped() {
        data = null;
    }

    /**
     * 标记存档已修改。
     */
    static void markDirty() {

        if (data != null) {
            data.setDirty();
        }
    }

    /**
     * 存档实例（同包的 PatrolManager 共用一份）。
     */
    static RaidSavedData savedData() {
        return data;
    }

    // =========================================================
    // 仇恨值
    // =========================================================

    /**
     * 查询玩家对某派系的仇恨值。
     */
    public static int getHatred(
            UUID playerId,
            Faction faction
    ) {

        if (data == null) {
            return 0;
        }

        return data.hatred
                .getOrDefault(playerId, Map.of())
                .getOrDefault(
                        faction.getId(),
                        0
                );
    }

    /**
     * 增减仇恨值（0 ~ 上限）。
     *
     * 突袭进行中仇恨冻结：
     * 击杀带来的增减一律忽略
     * （胜败结算的清算在移除突袭后调用，
     * 不受冻结影响）。
     */
    public static void addHatred(
            UUID playerId,
            Faction faction,
            int amount
    ) {

        if (data == null || amount == 0) {
            return;
        }

        // 仇恨冻结：该玩家有进行中的突袭
        if (data.activeRaids.containsKey(playerId)) {
            return;
        }

        Map<String, Integer> perFaction =
                data.hatred.computeIfAbsent(
                        playerId,
                        key -> new HashMap<>()
                );

        int current =
                perFaction.getOrDefault(
                        faction.getId(),
                        0
                );

        int updated =
                Mth.clamp(
                        current + amount,
                        0,
                        HATRED_CAP
                );

        if (updated != current) {

            perFaction.put(
                    faction.getId(),
                    updated
            );

            data.setDirty();
        }
    }

    /**
     * 直接设置仇恨值（0 ~ 上限）。
     *
     * 给结算和连杀拉仇恨用，
     * 不做冻结检查（调用方保证时机）。
     */
    private static void setHatred(
            UUID playerId,
            Faction faction,
            int value
    ) {

        if (data == null) {
            return;
        }

        data.hatred
                .computeIfAbsent(
                        playerId,
                        key -> new HashMap<>()
                )
                .put(
                        faction.getId(),
                        Mth.clamp(
                                value,
                                0,
                                HATRED_CAP
                        )
                );

        data.setDirty();
    }

    /**
     * 击杀派系生物的入口
     * （由 FactionLevelEvent 调用）：
     * 积累仇恨 + 短时间连杀检测 + 威胁等级成长。
     *
     * 威胁等级的累计击杀不受突袭冻结影响
     * （突袭中的击杀照常计入），
     * 升级曲线不因突袭中断。
     *
     * 突袭进行中仇恨冻结：
     * 击杀不增不减、不计连杀。
     * 突袭中的击杀只用于推进波次、
     * 掉落和任务进度（那些走各自的事件，
     * 不经过这里）。
     */
    public static void onPlayerKillFactionMob(
            ServerPlayer player,
            Faction victimFaction
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        // 威胁等级成长：累计击杀只增不减
        int oldThreat =
                getThreatLevel(playerId);

        data.killCount.merge(
                playerId,
                1,
                Integer::sum
        );

        data.setDirty();

        int newThreat =
                getThreatLevel(playerId);

        if (newThreat > oldThreat) {

            player.sendSystemMessage(
                    Component.translatable(
                            "raid.alone_adventure.threat_up",
                            THREAT_NAMES[newThreat - 1]
                    )
            );

            player.playNotifySound(
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.HOSTILE,
                    0.6F,
                    1.4F
            );
        }

        // 突袭期间：一切仇恨变化冻结
        if (data.activeRaids.containsKey(playerId)) {
            return;
        }

        // 该派系 +20，其他派系各 -5
        addHatred(
                playerId,
                victimFaction,
                KILL_HATE
        );

        for (Faction other : ModFactions.all()) {

            if (other == victimFaction) {
                continue;
            }

            addHatred(
                    playerId,
                    other,
                    -KILL_OTHER_DECAY
            );
        }

        // 短时连杀：窗口内击杀数达标 -> 立即突袭
        long now =
                player.serverLevel()
                        .getGameTime();

        Map<String, List<Long>> perPlayer =
                data.recentKills.computeIfAbsent(
                        playerId,
                        key -> new HashMap<>()
                );

        List<Long> times =
                perPlayer.computeIfAbsent(
                        victimFaction.getId(),
                        key -> new ArrayList<>()
                );

        times.add(now);

        // 只保留窗口（20 分钟）内的击杀
        times.removeIf(
                time -> time < now - BURST_WINDOW_TICKS
        );

        // 只留最近 BURST_KILL_TRIGGER 条，防止列表无限增长
        while (times.size() > BURST_KILL_TRIGGER) {
            times.remove(0);
        }

        data.setDirty();

        if (times.size() < BURST_KILL_TRIGGER) {
            return;
        }

        times.clear();

        int hatred =
                getHatred(playerId, victimFaction);

        // 仇恨不足敌对线：拉到 200 进入敌对，不立即突袭
        if (hatred < HOSTILE_THRESHOLD) {

            setHatred(
                    playerId,
                    victimFaction,
                    HOSTILE_THRESHOLD
            );

            player.sendSystemMessage(
                    Component.translatable(
                            "raid.alone_adventure.burst_hostile",
                            victimFaction.getDisplayName()
                    )
            );

            return;
        }

        // 仇恨 >= 200：立即突袭（冷却挡住时进等待队列）
        tryStartRaid(player, victimFaction);
    }

    // =========================================================
    // 威胁等级（升级机制）
    // =========================================================

    /**
     * 玩家累计击杀派系生物总数（只增不减）。
     */
    public static int getTotalKills(UUID playerId) {

        if (data == null) {
            return 0;
        }

        return data.killCount
                .getOrDefault(playerId, 0);
    }

    /**
     * 玩家当前的威胁等级（0 ~ 4）。
     *
     * 由累计击杀数解析，不单独落盘。
     */
    public static int getThreatLevel(UUID playerId) {

        int kills =
                getTotalKills(playerId);

        int level = 0;

        for (int threshold : THREAT_LEVEL_KILLS) {

            if (kills >= threshold) {
                level++;
            }
        }

        return level;
    }

    /**
     * 环境生成（巡逻战团 / 自然结群）的军衔掷骰：
     *
     * - 一阶军衔概率 = 威胁等级 x 15%
     * - 二阶军衔从威胁 3 级起出现，
     *   概率 = (威胁等级 - 2) x 10%
     *
     * 威胁 0 级时永远基础兵
     * （新手期不被高强度伏击），
     * 威胁 4 级时 60% 一阶 + 20% 二阶
     * （世界已全面开战）。
     *
     * 突袭波次有自己的波次感知公式
     * （见 FactionRaid.pickTier）。
     */
    public static PromotionTier rollAmbientTier(
            RandomSource random,
            int threatLevel
    ) {

        float eliteChance =
                threatLevel >= AMBIENT_ELITE_MIN_THREAT
                        ? (threatLevel - AMBIENT_ELITE_MIN_THREAT + 1)
                        * AMBIENT_ELITE_PER_LEVEL
                        : 0.0F;

        float advancedChance =
                threatLevel
                        * AMBIENT_ADVANCED_PER_LEVEL;

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

    // =========================================================
    // 态度查询（索敌用，实时解析）
    // =========================================================

    /**
     * 派系当前是否敌对该玩家。
     *
     * 恒敌对派系（恶魔 / 亡灵）永远 true；
     * 中立派系（帝国 / 部落）：
     * 突袭进行中或仇恨过敌对线（200）才 true。
     */
    public static boolean isHostileToPlayer(
            Faction faction,
            UUID playerId
    ) {

        if (faction.isHostileToPlayers()) {
            return true;
        }

        if (data == null) {
            return false;
        }

        FactionRaid raid =
                data.activeRaids.get(playerId);

        if (raid != null
                && raid.getFaction() == faction) {
            return true;
        }

        return getHatred(playerId, faction)
                >= HOSTILE_THRESHOLD;
    }

    /**
     * 派系生物是否因仇恨主动索敌该玩家
     * （FactionEnemyTargetGoal 调用）。
     */
    public static boolean isAngeredAt(
            LivingEntity mob,
            Player player
    ) {

        if (data == null) {
            return false;
        }

        Faction faction =
                FactionManager.getFaction(mob);

        return faction != null
                && isHostileToPlayer(
                faction,
                player.getUUID()
        );
    }

    // =========================================================
    // 突袭状态查询（仇恨之书显示用）
    // =========================================================

    /**
     * 玩家当前进行中的突袭派系
     * （无突袭返回 null）。
     */
    @Nullable
    public static Faction getActiveRaidFaction(
            UUID playerId
    ) {

        if (data == null) {
            return null;
        }

        FactionRaid raid =
                data.activeRaids.get(playerId);

        return raid != null
                ? raid.getFaction()
                : null;
    }

    /**
     * 同派系突袭冷却剩余 tick
     * （0 = 已冷却完毕）。
     */
    public static long getFactionCooldownRemaining(
            ServerPlayer player,
            Faction faction
    ) {

        if (data == null) {
            return 0L;
        }

        long remaining =
                getFactionCooldownUntil(
                        player.getUUID(),
                        faction
                ) - player.serverLevel()
                        .getGameTime();

        return Math.max(
                0L,
                remaining
        );
    }

    /**
     * 全局突袭冷却剩余 tick
     * （0 = 已冷却完毕）。
     */
    public static long getGlobalCooldownRemaining(
            ServerPlayer player
    ) {

        if (data == null) {
            return 0L;
        }

        long remaining =
                getGlobalCooldownUntil(
                        player.getUUID()
                ) - player.serverLevel()
                        .getGameTime();

        return Math.max(
                0L,
                remaining
        );
    }

    /**
     * 派系是否在该玩家的突袭等待队列中
     * （仇恨过临界但暂时轮不到开打）。
     */
    public static boolean isRaidQueued(
            UUID playerId,
            Faction faction
    ) {

        if (data == null) {
            return false;
        }

        List<String> queue =
                data.raidQueue.get(playerId);

        return queue != null
                && queue.contains(
                faction.getId()
        );
    }

    /**
     * 距离下一次周期检测的剩余 tick
     * （仇恨之书倒计时显示用）。
     *
     * 周期检测（15 分钟一次）是突袭的
     * 判定时机：仇恨过临界且无冷却压制时，
     * 下一次检测到点即会突袭。
     */
    public static long getNextCheckRemaining(
            ServerPlayer player
    ) {

        if (data == null) {
            return 0L;
        }

        Long next =
                data.nextCheck.get(
                        player.getUUID()
                );

        // 未初始化：首次检测在一整个周期后
        if (next == null) {
            return CHECK_INTERVAL_TICKS;
        }

        return Math.max(
                0L,
                next - player.serverLevel()
                        .getGameTime()
        );
    }

    // =========================================================
    // 主循环
    // =========================================================

    /**
     * 服务端每 tick：
     * 推进所有进行中的突袭 + 周期检测仇恨
     * + 轮询巡逻战团（每 10 秒一次）。
     */
    public static void tick(MinecraftServer server) {

        if (data == null) {
            data = RaidSavedData.get(server);
        }

        boolean pollTick =
                server.getTickCount()
                        % CHECK_POLL_TICKS
                        == 0;

        for (ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()) {

            FactionRaid raid =
                    data.activeRaids.get(
                            player.getUUID()
                    );

            if (raid != null) {
                raid.tick(player);
            }

            if (pollTick) {

                periodicCheck(player);

                // 巡逻战团（突袭中 / 不满足条件时内部自行跳过）
                PatrolManager.checkPatrol(player);
            }
        }
    }

    /**
     * 周期检测（每 15 分钟一次，
     * 突袭进行中不触发）：
     * 仇恨 >= 400 且同派系冷却已过的派系里
     * 仇恨最高的那个发起突袭；
     * 全局冷却未过则全部排队等待。
     */
    private static void periodicCheck(
            ServerPlayer player
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        long now =
                player.serverLevel()
                        .getGameTime();

        // 首次记录：15 分钟后才做第一次检测
        Long next =
                data.nextCheck.get(playerId);

        if (next == null) {

            data.nextCheck.put(
                    playerId,
                    now + CHECK_INTERVAL_TICKS
            );

            data.setDirty();

            return;
        }

        if (now < next) {
            return;
        }

        data.nextCheck.put(
                playerId,
                now + CHECK_INTERVAL_TICKS
        );

        data.setDirty();

        // 突袭期间不做周期检测
        if (data.activeRaids.containsKey(playerId)) {
            return;
        }

        // 清理等待队列里仇恨已回落的派系
        pruneQueue(playerId);

        // 候选：仇恨过临界且不在同派系冷却中
        List<Faction> candidates =
                new ArrayList<>();

        for (Faction faction : ModFactions.all()) {

            if (getHatred(playerId, faction)
                    >= RAID_THRESHOLD
                    && now >= getFactionCooldownUntil(
                    playerId,
                    faction
            )) {

                candidates.add(faction);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        // 全局冷却中：全部进等待队列
        if (now < getGlobalCooldownUntil(playerId)) {

            for (Faction faction : candidates) {
                enqueueRaid(player, faction);
            }

            return;
        }

        // 仇恨最高的先动手（其余由 startRaid 排进等待队列）
        Faction worstFaction = null;

        int worstHatred =
                RAID_THRESHOLD - 1;

        for (Faction faction : candidates) {

            int hatred =
                    getHatred(playerId, faction);

            if (hatred > worstHatred) {

                worstFaction = faction;

                worstHatred = hatred;
            }
        }

        if (worstFaction != null) {

            startRaid(player, worstFaction);
        }
    }

    // =========================================================
    // 等待队列 / 冷却
    // =========================================================

    /**
     * 派系进入突袭等待队列：
     * 仇恨过临界但暂时轮不到开打
     * （已有突袭 / 冷却未过）。
     */
    private static void enqueueRaid(
            ServerPlayer player,
            Faction faction
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        List<String> queue =
                data.raidQueue.computeIfAbsent(
                        playerId,
                        key -> new ArrayList<>()
                );

        if (queue.contains(faction.getId())) {
            return;
        }

        queue.add(faction.getId());

        data.setDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "raid.alone_adventure.queued",
                        faction.getDisplayName()
                )
        );

        LOGGER.info(
                "[Faction] raid queued: faction={} player={}",
                faction.getId(),
                player.getName().getString()
        );
    }

    /**
     * 清理等待队列：
     * 仇恨回落到临界以下
     * （例如另一场突袭胜利结算 -100）的派系出队。
     */
    private static void pruneQueue(
            UUID playerId
    ) {

        List<String> queue =
                data.raidQueue.get(playerId);

        if (queue == null) {
            return;
        }

        queue.removeIf(factionId -> {

            Faction faction =
                    FactionManager.getFaction(factionId);

            return faction == null
                    || getHatred(playerId, faction)
                    < RAID_THRESHOLD;
        });

        data.setDirty();
    }

    /**
     * 同派系突袭冷却结束时刻（gameTime）。
     */
    private static long getFactionCooldownUntil(
            UUID playerId,
            Faction faction
    ) {

        if (data == null) {
            return 0L;
        }

        return data.factionCooldownUntil
                .getOrDefault(playerId, Map.of())
                .getOrDefault(faction.getId(), 0L);
    }

    /**
     * 全局突袭冷却结束时刻（gameTime）。
     */
    private static long getGlobalCooldownUntil(
            UUID playerId
    ) {

        if (data == null) {
            return 0L;
        }

        return data.globalCooldownUntil
                .getOrDefault(playerId, 0L);
    }

    /**
     * 突袭结算冷却：
     * 本派系 60 分钟内不会再突袭，
     * 全局 20 分钟内任何派系都不突袭。
     */
    private static void applyRaidCooldowns(
            ServerPlayer player,
            Faction faction
    ) {

        UUID playerId =
                player.getUUID();

        long now =
                player.serverLevel()
                        .getGameTime();

        data.factionCooldownUntil
                .computeIfAbsent(
                        playerId,
                        key -> new HashMap<>()
                )
                .put(
                        faction.getId(),
                        now + FACTION_COOLDOWN_TICKS
                );

        data.globalCooldownUntil.put(
                playerId,
                now + GLOBAL_COOLDOWN_TICKS
        );
    }

    // =========================================================
    // 突袭生命周期
    // =========================================================

    /**
     * 尝试立即发起突袭（短时连杀入口）。
     *
     * 已有突袭 / 同派系冷却 / 全局冷却中
     * 都不能开打；仇恨过临界的改排队等待，
     * 否则作罢（等下次连杀）。
     */
    private static void tryStartRaid(
            ServerPlayer player,
            Faction faction
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        long now =
                player.serverLevel()
                        .getGameTime();

        boolean blocked =
                data.activeRaids.containsKey(playerId)
                        || now < getFactionCooldownUntil(
                        playerId,
                        faction
                )
                        || now < getGlobalCooldownUntil(playerId);

        if (!blocked) {
            startRaid(player, faction);
            return;
        }

        if (getHatred(playerId, faction)
                >= RAID_THRESHOLD) {

            enqueueRaid(player, faction);

        } else {

            LOGGER.info(
                    "[Faction] burst raid on {} suppressed by cooldown (player {})",
                    faction.getId(),
                    player.getName().getString()
            );
        }
    }

    /**
     * 对玩家发起一场突袭。
     *
     * 波次 = 3 + (仇恨 - 400) / 80，
     * 夹在 3 ~ 7 波；
     * 连杀在仇恨未过临界时触发则固定 3 波
     * （公式的负数部分被夹住）。
     */
    public static void startRaid(
            ServerPlayer player,
            Faction faction
    ) {

        if (data == null
                || data.activeRaids.containsKey(
                player.getUUID()
        )) {
            return;
        }

        UUID playerId =
                player.getUUID();

        long now =
                player.serverLevel()
                        .getGameTime();

        int hatred =
                getHatred(playerId, faction);

        int waves =
                Mth.clamp(
                        RAID_BASE_WAVES
                                + Math.floorDiv(
                                hatred - RAID_THRESHOLD,
                                WAVES_PER_EXCESS
                        ),
                        RAID_BASE_WAVES,
                        RAID_MAX_WAVES
                );

        data.activeRaids.put(
                playerId,
                new FactionRaid(
                        playerId,
                        faction,
                        waves
                )
        );

        // 该派系已开打，出队
        List<String> queue =
                data.raidQueue.get(playerId);

        if (queue != null) {
            queue.remove(faction.getId());
        }

        // 其余仇恨过线的派系进入等待队列
        for (Faction other : ModFactions.all()) {

            if (other == faction) {
                continue;
            }

            if (getHatred(playerId, other)
                    >= RAID_THRESHOLD
                    && now >= getFactionCooldownUntil(
                    playerId,
                    other
            )) {

                enqueueRaid(player, other);
            }
        }

        // 结算后再排下一次检测，防止连锁开战
        data.nextCheck.put(
                playerId,
                now + CHECK_INTERVAL_TICKS
        );

        data.setDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "raid.alone_adventure.incoming",
                        faction.getDisplayName()
                )
        );

        player.level()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.RAID_HORN.value(),
                        SoundSource.HOSTILE,
                        2.0F,
                        1.0F
                );

        LOGGER.info(
                "[Faction] raid started: faction={} waves={} player={}",
                faction.getId(),
                waves,
                player.getName().getString()
        );
    }

    /**
     * 挑衅号角：立刻对仇恨最高且已达突袭临界
     * 的派系发起突袭，无视所有冷却。
     *
     * 规则：
     *
     * - 已有突袭进行中：拒绝（同时只能打一场）
     * - 只挑仇恨 >= 400（突袭临界）的派系，
     *   仇恨最高的那一个
     * - 同派系冷却 / 全局冷却 / 等待队列
     *   全部跳过
     *
     * @return 是否成功发起突袭
     */
    public static boolean provokeRaid(
            ServerPlayer player
    ) {

        if (data == null) {
            return false;
        }

        UUID playerId =
                player.getUUID();

        // 同时只能打一场
        if (data.activeRaids.containsKey(playerId)) {
            return false;
        }

        Faction target = null;

        int bestHatred =
                RAID_THRESHOLD - 1;

        for (Faction faction : ModFactions.all()) {

            int hatred =
                    getHatred(
                            playerId,
                            faction
                    );

            if (hatred > bestHatred) {

                target = faction;

                bestHatred = hatred;
            }
        }

        // 没有仇恨达标的派系
        if (target == null) {
            return false;
        }

        // 该派系已开打，从等待队列里清掉
        List<String> queue =
                data.raidQueue.get(playerId);

        if (queue != null) {
            queue.remove(target.getId());
        }

        data.setDirty();

        LOGGER.info(
                "[Faction] {} provoked a raid on {} (hatred {})",
                player.getName().getString(),
                target.getId(),
                bestHatred
        );

        // startRaid 不检查冷却：挑衅的语义就是无视冷却
        startRaid(player, target);

        return true;
    }

    /**
     * 突袭胜利：
     * 本派系仇恨清零，其他派系各 -100；
     * 本派系冷却 60 分钟，全局冷却 20 分钟。
     */
    public static void succeedRaid(
            ServerPlayer player,
            FactionRaid raid
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        // 先移除突袭解除仇恨冻结，结算才能生效
        data.activeRaids.remove(playerId);

        // 本派系仇恨清零
        setHatred(
                playerId,
                raid.getFaction(),
                0
        );

        for (Faction other : ModFactions.all()) {

            if (other == raid.getFaction()) {
                continue;
            }

            addHatred(
                    playerId,
                    other,
                    -SUCCESS_OTHER_REDUCTION
            );
        }

        applyRaidCooldowns(
                player,
                raid.getFaction()
        );

        data.nextCheck.put(
                playerId,
                player.serverLevel()
                        .getGameTime()
                        + CHECK_INTERVAL_TICKS
        );

        data.setDirty();

        raid.terminate(player);

        player.sendSystemMessage(
                Component.translatable(
                        "raid.alone_adventure.success",
                        raid.getFaction()
                                .getDisplayName()
                )
        );

        player.playNotifySound(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        LOGGER.info(
                "[Faction] raid success: player {} beat {}",
                player.getName().getString(),
                raid.getFaction()
                        .getId()
        );
    }

    /**
     * 突袭失败（第 8 次死亡 / 超时 2 小时）：
     * 本派系仇恨减半（下限 200），
     * 其他派系各 +20；冷却同胜利结算。
     */
    public static void failRaid(
            ServerPlayer player,
            FactionRaid raid,
            FailReason reason
    ) {

        if (data == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        // 先移除突袭解除仇恨冻结，结算才能生效
        data.activeRaids.remove(playerId);

        int hatred =
                getHatred(
                        playerId,
                        raid.getFaction()
                );

        setHatred(
                playerId,
                raid.getFaction(),
                Math.max(
                        FAIL_OWN_MIN_HATRED,
                        hatred / 2
                )
        );

        for (Faction other : ModFactions.all()) {

            if (other == raid.getFaction()) {
                continue;
            }

            addHatred(
                    playerId,
                    other,
                    FAIL_OTHER_INCREASE
            );
        }

        applyRaidCooldowns(
                player,
                raid.getFaction()
        );

        data.nextCheck.put(
                playerId,
                player.serverLevel()
                        .getGameTime()
                        + CHECK_INTERVAL_TICKS
        );

        data.setDirty();

        raid.terminate(player);

        player.sendSystemMessage(
                Component.translatable(
                        reason.translationKey,
                        raid.getFaction()
                                .getDisplayName()
                )
        );

        player.playNotifySound(
                SoundEvents.WITHER_DEATH,
                SoundSource.HOSTILE,
                0.8F,
                0.8F
        );

        LOGGER.info(
                "[Faction] raid failed ({}): player {} vs {}",
                reason.name(),
                player.getName().getString(),
                raid.getFaction()
                        .getId()
        );
    }

    // =========================================================
    // 玩家上下线 / 死亡
    // =========================================================

    /**
     * 玩家上线：
     * 初始化周期检测与巡逻计划、
     * 重连进行中的突袭血条。
     */
    public static void onPlayerLogin(
            ServerPlayer player
    ) {

        if (data == null) {

            MinecraftServer server =
                    player.getServer();

            if (server == null) {
                return;
            }

            data = RaidSavedData.get(server);
        }

        UUID playerId =
                player.getUUID();

        if (!data.nextCheck.containsKey(playerId)) {

            data.nextCheck.put(
                    playerId,
                    player.serverLevel()
                            .getGameTime()
                            + CHECK_INTERVAL_TICKS
            );

            data.setDirty();
        }

        PatrolManager.onPlayerLogin(player);

        FactionRaid raid =
                data.activeRaids.get(playerId);

        if (raid != null) {
            raid.showBossBar(player);
        }
    }

    /**
     * 玩家下线：移除血条（突袭本身挂起保留）。
     */
    public static void onPlayerLogout(
            ServerPlayer player
    ) {

        if (data == null) {
            return;
        }

        FactionRaid raid =
                data.activeRaids.get(
                        player.getUUID()
                );

        if (raid != null) {
            raid.hideBossBar(player);
        }
    }

    /**
     * 玩家死亡：突袭死亡计数 +1。
     */
    public static void onPlayerDeath(
            ServerPlayer player
    ) {

        if (data == null) {
            return;
        }

        FactionRaid raid =
                data.activeRaids.get(
                        player.getUUID()
                );

        if (raid != null) {
            raid.onPlayerDeath(player);
        }
    }

    // =========================================================
    // 玩家派系归属
    // =========================================================

    /**
     * 玩家当前加入的派系
     * （null 表示未加入任何派系）。
     */
    @Nullable
    public static Faction getPlayerFaction(
            UUID playerId
    ) {

        if (data == null) {
            return null;
        }

        String factionId =
                data.playerFaction.get(playerId);

        if (factionId == null
                || factionId.isEmpty()) {
            return null;
        }

        return FactionManager.getFaction(factionId);
    }

    /**
     * 玩家是否已加入某个派系。
     */
    public static boolean hasPlayerFaction(
            UUID playerId
    ) {

        return getPlayerFaction(playerId) != null;
    }

    /**
     * 玩家加入派系：
     * 清零对该派系的仇恨，重置该派系突袭冷却。
     *
     * 限制条件：
     * - 玩家必须当前未加入任何派系
     * - 目标派系对玩家必须态度中立（仇恨 < 200）
     *
     * @return true 表示成功加入，false 表示失败
     */
    public static boolean joinFaction(
            ServerPlayer player,
            Faction faction
    ) {

        if (data == null) {
            return false;
        }

        UUID playerId =
                player.getUUID();

        // 检查：玩家是否已经加入了某个派系
        if (hasPlayerFaction(playerId)) {
            Faction current = getPlayerFaction(playerId);
            player.sendSystemMessage(
                    Component.translatable(
                            "faction.alone_adventure.already_joined"
                    ).withStyle(ChatFormatting.YELLOW)
            );
            return false;
        }

        // 检查：目标派系对玩家是否态度中立
        if (isHostileToPlayer(faction, playerId)) {
            player.sendSystemMessage(
                    Component.translatable(
                            "faction.alone_adventure.cannot_join_hostile"
                    ).withStyle(ChatFormatting.RED)
            );
            return false;
        }

        // 清零仇恨
        setHatred(playerId, faction, 0);

        // 清除等待队列中该派系
        List<String> queue =
                data.raidQueue.get(playerId);

        if (queue != null) {
            queue.remove(faction.getId());
        }

        // 记录玩家派系
        data.playerFaction.put(playerId, faction.getId());

        data.setDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "faction.alone_adventure.joined",
                        faction.getDisplayName()
                )
        );

        player.playNotifySound(
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        return true;
    }

    /**
     * 玩家退出当前派系。
     *
     * 退出后：
     * - 对该派系增加 500 点仇恨值
     * - 清除玩家派系归属
     */
    public static void leaveFaction(
            ServerPlayer player
    ) {

        if (data == null
                || !hasPlayerFaction(player.getUUID())) {
            return;
        }

        UUID playerId =
                player.getUUID();

        Faction faction =
                getPlayerFaction(playerId);

        // 退出派系：增加 500 点仇恨值
        if (faction != null) {
            addHatred(playerId, faction, 500);
        }

        data.playerFaction.remove(playerId);

        data.setDirty();

        player.sendSystemMessage(
                Component.translatable(
                        "faction.alone_adventure.left",
                        faction != null
                                ? faction.getDisplayName()
                                : Component.literal("Unknown")
                )
        );

        player.playNotifySound(
                SoundEvents.FIREWORK_ROCKET_BLAST,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }
}
