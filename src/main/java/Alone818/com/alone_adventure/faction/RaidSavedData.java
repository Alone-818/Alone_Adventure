package Alone818.com.alone_adventure.faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 仇恨-突袭系统的存档。
 *
 * 挂在主世界的 DimensionDataStorage 上，
 * 整个存档（全维度）共享一份。
 *
 * 内容：
 *
 * 1. 仇恨值：玩家 -> 派系 -> 数值
 * 2. 下次周期检测时间：玩家 -> gameTime
 * 3. 短时间连杀记录：玩家 -> 派系 -> 击杀时刻列表
 * 4. 进行中的突袭：玩家 -> FactionRaid
 * 5. 同派系突袭冷却（60 分钟）：玩家 -> 派系 -> gameTime
 * 6. 全局突袭冷却（20 分钟）：玩家 -> gameTime
 * 7. 突袭等待队列：玩家 -> 排队派系 id 列表
 * 8. 累计击杀数：玩家 -> 总数（威胁等级用，永不重置）
 * 9. 下次巡逻战团：玩家 -> gameTime
 *
 * 只在服务端读写，
 * 客户端永远不碰这份存档。
 */
public class RaidSavedData
        extends SavedData {

    /** 存档文件名（data/alone_adventure_raids.dat） */
    public static final String DATA_NAME =
            "alone_adventure_raids";

    /** 仇恨值：玩家 UUID -> (派系 id -> 仇恨) */
    final Map<UUID, Map<String, Integer>> hatred =
            new HashMap<>();

    /** 下次周期检测：玩家 UUID -> gameTime */
    final Map<UUID, Long> nextCheck =
            new HashMap<>();

    /** 短时间连杀记录：玩家 UUID -> (派系 id -> 击杀时刻) */
    final Map<UUID, Map<String, List<Long>>> recentKills =
            new HashMap<>();

    /** 进行中的突袭：玩家 UUID -> 突袭实例 */
    final Map<UUID, FactionRaid> activeRaids =
            new HashMap<>();

    /** 同派系突袭冷却：玩家 UUID -> (派系 id -> 冷却结束 gameTime) */
    final Map<UUID, Map<String, Long>> factionCooldownUntil =
            new HashMap<>();

    /** 全局突袭冷却：玩家 UUID -> 冷却结束 gameTime */
    final Map<UUID, Long> globalCooldownUntil =
            new HashMap<>();

    /** 突袭等待队列：玩家 UUID -> 排队派系 id */
    final Map<UUID, List<String>> raidQueue =
            new HashMap<>();

    /**
     * 累计击杀派系生物总数：玩家 UUID -> 数量。
     *
     * 威胁等级的唯一依据，
     * 与会回落的仇恨不同，只增不减
     * （突袭期间的击杀也计入），
     * 保证升级曲线平滑向前。
     */
    final Map<UUID, Integer> killCount =
            new HashMap<>();

    /** 下次巡逻战团：玩家 UUID -> gameTime */
    final Map<UUID, Long> nextPatrol =
            new HashMap<>();

    /**
     * 取本存档的全局实例（不存在则创建）。
     */
    public static RaidSavedData get(MinecraftServer server) {

        DimensionDataStorage storage =
                server.overworld()
                        .getDataStorage();

        return storage.computeIfAbsent(
                RaidSavedData::load,
                RaidSavedData::new,
                DATA_NAME
        );
    }

    /**
     * 从 NBT 读档。
     */
    @Nullable
    private static RaidSavedData load(CompoundTag tag) {

        RaidSavedData data =
                new RaidSavedData();

        loadHatred(data, tag.getCompound("Hatred"));

        CompoundTag checkTag =
                tag.getCompound("NextCheck");

        for (String key : checkTag.getAllKeys()) {

            try {

                data.nextCheck.put(
                        UUID.fromString(key),
                        checkTag.getLong(key)
                );
            } catch (IllegalArgumentException ignored) {

                // 无效 UUID（换过档 / 手改过文件）：跳过
            }
        }

        loadRecentKills(data, tag.getCompound("RecentKills"));

        CompoundTag killCountTag =
                tag.getCompound("KillCount");

        for (String key : killCountTag.getAllKeys()) {

            try {

                data.killCount.put(
                        UUID.fromString(key),
                        killCountTag.getInt(key)
                );
            } catch (IllegalArgumentException ignored) {

                // 无效 UUID：跳过
            }
        }

        CompoundTag nextPatrolTag =
                tag.getCompound("NextPatrol");

        for (String key : nextPatrolTag.getAllKeys()) {

            try {

                data.nextPatrol.put(
                        UUID.fromString(key),
                        nextPatrolTag.getLong(key)
                );
            } catch (IllegalArgumentException ignored) {

                // 无效 UUID：跳过
            }
        }

        loadFactionCooldowns(
                data,
                tag.getCompound("FactionCooldown")
        );

        CompoundTag globalCooldownTag =
                tag.getCompound("GlobalCooldown");

        for (String key : globalCooldownTag.getAllKeys()) {

            try {
                data.globalCooldownUntil.put(
                        UUID.fromString(key),
                        globalCooldownTag.getLong(key)
                );
            } catch (IllegalArgumentException ignored) {

                // 无效 UUID：跳过
            }
        }

        CompoundTag queueTag =
                tag.getCompound("RaidQueue");

        for (String key : queueTag.getAllKeys()) {

            try {
                UUID playerId =
                        UUID.fromString(key);

                ListTag list =
                        queueTag.getList(
                                key,
                                Tag.TAG_STRING
                        );

                List<String> factions =
                        new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    factions.add(
                            list.getString(i)
                    );
                }

                data.raidQueue.put(playerId, factions);

            } catch (IllegalArgumentException ignored) {

                // 无效 UUID：跳过
            }
        }

        ListTag raids =
                tag.getList("ActiveRaids", Tag.TAG_COMPOUND);

        for (int i = 0; i < raids.size(); i++) {

            FactionRaid raid =
                    FactionRaid.readNBT(
                            raids.getCompound(i)
                    );

            if (raid != null) {
                data.activeRaids.put(
                        raid.getPlayerId(),
                        raid
                );
            }
        }

        return data;
    }

    private static void loadHatred(
            RaidSavedData data,
            CompoundTag hatredTag
    ) {

        for (String playerKey : hatredTag.getAllKeys()) {

            try {

                UUID playerId =
                        UUID.fromString(playerKey);

                CompoundTag perFaction =
                        hatredTag.getCompound(playerKey);

                Map<String, Integer> values =
                        new HashMap<>();

                for (String factionId : perFaction.getAllKeys()) {
                    values.put(
                            factionId,
                            perFaction.getInt(factionId)
                    );
                }

                data.hatred.put(playerId, values);

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void loadRecentKills(
            RaidSavedData data,
            CompoundTag killsTag
    ) {

        for (String playerKey : killsTag.getAllKeys()) {

            try {

                UUID playerId =
                        UUID.fromString(playerKey);

                CompoundTag perFaction =
                        killsTag.getCompound(playerKey);

                Map<String, List<Long>> values =
                        new HashMap<>();

                for (String factionId : perFaction.getAllKeys()) {

                    List<Long> times =
                            new ArrayList<>();

                    ListTag list =
                            perFaction.getList(
                                    factionId,
                                    Tag.TAG_LONG
                            );

                    for (int i = 0; i < list.size(); i++) {

                        Tag entry =
                                list.get(i);

                        if (entry instanceof LongTag longTag) {
                            times.add(
                                    longTag.getAsLong()
                            );
                        }
                    }

                    values.put(factionId, times);
                }

                data.recentKills.put(playerId, values);

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void loadFactionCooldowns(
            RaidSavedData data,
            CompoundTag cooldownTag
    ) {

        for (String playerKey : cooldownTag.getAllKeys()) {

            try {

                UUID playerId =
                        UUID.fromString(playerKey);

                CompoundTag perFaction =
                        cooldownTag.getCompound(playerKey);

                Map<String, Long> values =
                        new HashMap<>();

                for (String factionId : perFaction.getAllKeys()) {
                    values.put(
                            factionId,
                            perFaction.getLong(factionId)
                    );
                }

                data.factionCooldownUntil.put(playerId, values);

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {

        // 仇恨值
        CompoundTag hatredTag =
                new CompoundTag();

        for (Map.Entry<UUID, Map<String, Integer>> player
                : hatred.entrySet()) {

            CompoundTag perFaction =
                    new CompoundTag();

            for (Map.Entry<String, Integer> entry
                    : player.getValue().entrySet()) {

                perFaction.putInt(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            hatredTag.put(
                    player.getKey().toString(),
                    perFaction
            );
        }

        tag.put("Hatred", hatredTag);

        // 下次周期检测
        CompoundTag checkTag =
                new CompoundTag();

        for (Map.Entry<UUID, Long> entry
                : nextCheck.entrySet()) {

            checkTag.putLong(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        tag.put("NextCheck", checkTag);

        // 短时间连杀记录
        CompoundTag killsTag =
                new CompoundTag();

        for (Map.Entry<UUID, Map<String, List<Long>>> player
                : recentKills.entrySet()) {

            CompoundTag perFaction =
                    new CompoundTag();

            for (Map.Entry<String, List<Long>> entry
                    : player.getValue().entrySet()) {

                ListTag list =
                        new ListTag();

                for (Long time : entry.getValue()) {
                    list.add(
                            LongTag.valueOf(time)
                    );
                }

                perFaction.put(
                        entry.getKey(),
                        list
                );
            }

            killsTag.put(
                    player.getKey().toString(),
                    perFaction
            );
        }

        tag.put("RecentKills", killsTag);

        // 累计击杀数（威胁等级）
        CompoundTag killCountTag =
                new CompoundTag();

        for (Map.Entry<UUID, Integer> entry
                : killCount.entrySet()) {

            killCountTag.putInt(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        tag.put("KillCount", killCountTag);

        // 下次巡逻战团
        CompoundTag nextPatrolTag =
                new CompoundTag();

        for (Map.Entry<UUID, Long> entry
                : nextPatrol.entrySet()) {

            nextPatrolTag.putLong(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        tag.put("NextPatrol", nextPatrolTag);

        // 同派系突袭冷却
        CompoundTag factionCooldownTag =
                new CompoundTag();

        for (Map.Entry<UUID, Map<String, Long>> player
                : factionCooldownUntil.entrySet()) {

            CompoundTag perFaction =
                    new CompoundTag();

            for (Map.Entry<String, Long> entry
                    : player.getValue().entrySet()) {

                perFaction.putLong(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            factionCooldownTag.put(
                    player.getKey().toString(),
                    perFaction
            );
        }

        tag.put("FactionCooldown", factionCooldownTag);

        // 全局突袭冷却
        CompoundTag globalCooldownTag =
                new CompoundTag();

        for (Map.Entry<UUID, Long> entry
                : globalCooldownUntil.entrySet()) {

            globalCooldownTag.putLong(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        tag.put("GlobalCooldown", globalCooldownTag);

        // 突袭等待队列
        CompoundTag queueTag =
                new CompoundTag();

        for (Map.Entry<UUID, List<String>> entry
                : raidQueue.entrySet()) {

            ListTag list =
                    new ListTag();

            for (String factionId : entry.getValue()) {
                list.add(
                        StringTag.valueOf(factionId)
                );
            }

            queueTag.put(
                    entry.getKey().toString(),
                    list
            );
        }

        tag.put("RaidQueue", queueTag);

        // 进行中的突袭
        ListTag raids =
                new ListTag();

        for (FactionRaid raid : activeRaids.values()) {
            raids.add(
                    raid.writeNBT()
            );
        }

        tag.put("ActiveRaids", raids);

        return tag;
    }
}
