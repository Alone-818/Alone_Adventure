package Alone818.com.alone_adventure.Curios.gun;

import Alone818.com.alone_adventure.Items.gun.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.GunStatTooltipBuilder;
import Alone818.com.alone_adventure.Items.gun.IGunStatTooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 特质：人枪合一
 *
 * 枪械基础倍率削弱到 70%。
 *
 * 每一次开枪：
 * 命中（这次开枪至少有一发子弹命中实体）倍率 +10%，上限 150%
 * 空枪（这次开枪的所有子弹都没有命中实体）倍率 -20%，最低回落到 70%
 *
 * 倍率存储在玩家持久化 NBT 中，
 * 死亡后自动回到基础倍率 70%。
 */
public class OneWithGun
        extends GunTraitItem
        implements IGunStatTooltip {

    /** 基础倍率，也是空枪的最低回落值：70% */
    public static final float BASE_MULTIPLIER = 0.70F;

    /** 倍率上限：150% */
    public static final float MAX_MULTIPLIER = 1.50F;

    /** 每次开枪命中：+10% */
    public static final float HIT_BONUS = 0.10F;

    /** 每次空枪：-20% */
    public static final float MISS_PENALTY = 0.20F;

    /** 玩家持久化 NBT 键：当前倍率 */
    private static final String TAG_MULTIPLIER =
            "AloneAdventure_OneWithGunMultiplier";

    /**
     * 进行中的开枪记录。
     *
     * 每次开枪生成一个编号，
     * 这次开枪打出的所有子弹携带同一个编号。
     *
     * 这次开枪的所有子弹都消失后结算：
     * 有任意子弹命中过实体 -> 命中
     * 全部没有命中 -> 空枪
     */
    private static final Map<Long, ShotRecord> ACTIVE_SHOTS =
            new HashMap<>();

    /** 全局开枪编号，从 1 开始；0 表示"未参与追踪" */
    private static long nextShotId = 1L;

    /**
     * 开枪记录最长保留时间（tick）。
     *
     * 子弹最长寿命 100 tick。
     * 超过这个时间还没有结算的记录，
     * 说明子弹因为区块卸载等原因丢失，
     * 静默丢弃，不算命中也不算空枪。
     */
    private static final long STALE_TICKS = 300L;

    /** 一次开枪的结算状态 */
    private static class ShotRecord {

        /** 开枪者 */
        final UUID owner;

        /** 还没有消失的子弹数量 */
        int pending;

        /** 是否有子弹命中过实体 */
        boolean hit;

        /** 开枪时刻 */
        final long createdAt;

        ShotRecord(
                UUID owner,
                int pending,
                long createdAt
        ) {
            this.owner = owner;
            this.pending = pending;
            this.createdAt = createdAt;
        }
    }

    public OneWithGun() {
        super(
                new Properties()
                        .stacksTo(1)
        );
    }

    /**
     * 判断玩家是否佩戴了这个特质。
     */
    public static boolean isWearing(Player player) {

        return CuriosApi
                .getCuriosInventory(player)
                .resolve()
                .map(handler ->
                        handler.findFirstCurio(
                                stack ->
                                        stack.getItem()
                                                instanceof OneWithGun
                        ).isPresent()
                )
                .orElse(false);
    }

    /**
     * 获取当前生效的伤害倍率。
     *
     * 未佩戴时返回 1.0。
     *
     * 佩戴时返回存储的倍率，
     * 没有记录时视为基础倍率 70%。
     */
    public static float getDamageMultiplier(
            Player player
    ) {

        if (!isWearing(player)) {
            return 1.0F;
        }

        CompoundTag data =
                player.getPersistentData();

        float value =
                data.contains(TAG_MULTIPLIER)
                        ? data.getFloat(TAG_MULTIPLIER)
                        : BASE_MULTIPLIER;

        return
                Mth.clamp(
                        value,
                        BASE_MULTIPLIER,
                        MAX_MULTIPLIER
                );
    }

    /**
     * 开枪时调用：登记一次开枪。
     *
     * @param player      开枪玩家
     * @param bulletCount 这次开枪生成的子弹数量
     *
     * @return 这次开枪的编号；未佩戴特质时返回 0
     */
    public static long beginShot(
            ServerPlayer player,
            int bulletCount
    ) {

        if (!isWearing(player)) {
            return 0L;
        }

        if (bulletCount <= 0) {
            return 0L;
        }

        long now =
                player.level()
                        .getGameTime();

        purgeStaleShots(now);

        long shotId = nextShotId++;

        ACTIVE_SHOTS.put(
                shotId,
                new ShotRecord(
                        player.getUUID(),
                        bulletCount,
                        now
                )
        );

        return shotId;
    }

    /**
     * 子弹命中实体时调用：标记这次开枪命中。
     */
    public static void onBulletHitEntity(
            BulletProjectile bullet
    ) {

        long shotId =
                bullet.getOneWithGunShot();

        if (shotId == 0L) {
            return;
        }

        ShotRecord record =
                ACTIVE_SHOTS.get(shotId);

        if (record != null) {
            record.hit = true;
        }
    }

    /**
     * 子弹消失时调用。
     *
     * 这次开枪的所有子弹都消失后结算：
     *
     * 命中 +10%（上限 150%）
     * 空枪 -20%（最低 70%）
     */
    public static void onBulletDiscarded(
            BulletProjectile bullet
    ) {

        long shotId =
                bullet.getOneWithGunShot();

        if (shotId == 0L) {
            return;
        }

        /*
         * 每颗子弹只允许结算一次，
         * 防止 discard() 被重复调用。
         */
        bullet.setOneWithGunShot(0L);

        ShotRecord record =
                ACTIVE_SHOTS.get(shotId);

        if (record == null) {
            return;
        }

        record.pending--;

        if (record.pending > 0) {
            return;
        }

        ACTIVE_SHOTS.remove(shotId);

        ServerPlayer shooter =
                bullet.level().getServer() != null
                        ? bullet.level()
                        .getServer()
                        .getPlayerList()
                        .getPlayer(
                                record.owner
                        )
                        : null;

        /*
         * 玩家已经下线：
         * 这次开枪不结算。
         */
        if (shooter == null) {
            return;
        }

        float current =
                getDamageMultiplier(shooter);

        float next =
                record.hit
                        ? Math.min(
                        current + HIT_BONUS,
                        MAX_MULTIPLIER
                )
                        : Math.max(
                        current - MISS_PENALTY,
                        BASE_MULTIPLIER
                );

        shooter.getPersistentData()
                .putFloat(
                        TAG_MULTIPLIER,
                        next
                );

        /*
         * 动作栏反馈当前倍率。
         */
        shooter.displayClientMessage(
                Component.translatable(
                                "trait.alone_adventure.one_with_gun.status",
                                Math.round(
                                        next * 100.0F
                                )
                        )
                        .withStyle(
                                record.hit
                                        ? ChatFormatting.GOLD
                                        : ChatFormatting.RED
                        ),
                true
        );
    }

    /**
     * 清理过期的开枪记录。
     *
     * 子弹因为区块卸载等原因丢失时，
     * 对应记录会一直挂着，
     * 这里按时间静默丢弃。
     */
    private static void purgeStaleShots(long now) {

        Iterator<Map.Entry<Long, ShotRecord>> iterator =
                ACTIVE_SHOTS
                        .entrySet()
                        .iterator();

        while (iterator.hasNext()) {

            ShotRecord record =
                    iterator.next()
                            .getValue();

            if (now - record.createdAt
                    > STALE_TICKS) {

                iterator.remove();
            }
        }
    }

    @Override
    public void addGunStatTooltip(
            List<Component> tooltip
    ) {

        tooltip.add(
                Component.translatable(
                                "trait.alone_adventure.one_with_gun.description"
                        )
                        .withStyle(
                                ChatFormatting.WHITE
                        )
        );

        tooltip.add(
                Component.translatable(
                                "curios.modifiers.trait"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );

        tooltip.add(
                Component.translatable(
                                "trait.alone_adventure.one_with_gun.effect"
                        )
                        .withStyle(
                                ChatFormatting.WHITE
                        )
        );

        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "damage",
                1.5F
        );
    }
}
