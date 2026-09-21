package Alone818.com.alone_adventure.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 轻量目标追踪器 —— 攻击追踪类物品的逐目标状态。
 *
 * <b>节省内存的设计</b>：
 * <ul>
 *   <li>整个追踪器只有两个字段（目标 UUID + 下次扫描时刻），随宿主实体存在，
 *       无任何静态 Map / 全局缓存；</li>
 *   <li>目标以 <b>UUID</b> 记录而非实体引用——每次使用时经
 *       {@link Targeting#resolve} 从世界重解析（一次哈希查表，零分配），
 *       不会持有已卸载区块实体的强引用造成内存泄漏；</li>
 *   <li>扫描按间隔节流（{@code scanIntervalTicks}，建议 ≥10），
 *       两次扫描之间只做一次 UUID 查表 + 距离平方比较；</li>
 *   <li>扫描时会自动<b>升级目标</b>（出现更高优先级/更近的目标时切换），
 *       目标死亡或超出追踪距离才丢失。</li>
 * </ul>
 *
 * 目标优先级与扫描细节见 {@link Targeting}（敌对生物 > 非玩家生物 > 玩家）。
 * 状态可经 {@link #save} / {@link #load} 持久化到宿主 NBT（键
 * {@value #TAG_TARGET} 等），跨存档继续追踪。
 */
public final class TargetTracker {

    /** NBT 键：目标 UUID（字符串形式） */
    public static final String TAG_TARGET = "TrackTarget";
    /** NBT 键：下次可扫描的游戏刻 */
    public static final String TAG_NEXT_SCAN = "TrackNextScan";

    /** 当前目标（UUID；null = 无目标） */
    @Nullable
    private UUID targetId;
    /** 下次允许扫描的游戏刻（节流；Long.MIN_VALUE = 立即可扫描） */
    private long nextScanAt = Long.MIN_VALUE;

    /**
     * 每 tick 调用的主入口：返回当前应追踪的目标。
     *
     * @param level            世界
     * @param center           追踪中心（通常是弹体/物品位置）
     * @param scanRadius       扫描半径（格）
     * @param loseRange        目标超出该距离则丢失（≥ 扫描半径，追踪段大于搜索段）
     * @param gameTime         当前游戏刻（level.getGameTime()）
     * @param scanIntervalTicks 扫描节流间隔（tick，建议 ≥10；0 视为 1）
     * @param ignore           排除的实体（通常是发射者），可为 null
     * @return 当前目标；无目标返回 null
     */
    @Nullable
    public LivingEntity update(Level level, Vec3 center, double scanRadius, double loseRange,
                                long gameTime, long scanIntervalTicks,
                                @Nullable LivingEntity ignore) {
        // 已有目标的有效性（UUID 查表 + 距离平方，零分配）
        LivingEntity current = targetId == null ? null : Targeting.resolve(level, targetId);
        boolean valid = current != null && current.isAlive()
                && center.distanceToSqr(current.position()) <= loseRange * loseRange;

        // 到达扫描时刻：重新扫描（可升级到更高优先级/更近的目标）
        if (gameTime >= nextScanAt) {
            nextScanAt = gameTime + Math.max(1L, scanIntervalTicks);
            LivingEntity found = Targeting.findTarget(level, center, scanRadius, ignore);
            if (found != null) {
                targetId = found.getUUID();
                return found;
            }
            // 扫描无结果：仍有效的旧目标继续用，否则真正丢失
            if (valid) {
                return current;
            }
            targetId = null;
            return null;
        }

        return valid ? current : null;
    }

    /** 当前目标（仅重解析，不扫描）；无目标或已失效返回 null */
    @Nullable
    public LivingEntity peek(Level level) {
        if (targetId == null) return null;
        LivingEntity target = Targeting.resolve(level, targetId);
        return target != null && target.isAlive() ? target : null;
    }

    /** 清空目标并立即可再扫描（停火 / 换目标时机调用） */
    public void clear() {
        targetId = null;
        nextScanAt = Long.MIN_VALUE;
    }

    /** 是否已有目标（不解析，不扫描） */
    public boolean hasTarget() {
        return targetId != null;
    }

    /** 持久化到宿主 NBT（子弹等长命宿主跨存档用） */
    public void save(CompoundTag tag) {
        if (targetId != null) {
            tag.putUUID(TAG_TARGET, targetId);
        }
        if (nextScanAt != Long.MIN_VALUE) {
            tag.putLong(TAG_NEXT_SCAN, nextScanAt);
        }
    }

    /** 从宿主 NBT 恢复（缺键时为空追踪器） */
    public void load(CompoundTag tag) {
        this.targetId = tag.hasUUID(TAG_TARGET) ? tag.getUUID(TAG_TARGET) : null;
        this.nextScanAt = tag.contains(TAG_NEXT_SCAN) ? tag.getLong(TAG_NEXT_SCAN) : Long.MIN_VALUE;
    }
}
