package Alone818.com.alone_adventure.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;


/**
 * 子弹目标追踪器。
 *
 * 每颗子弹独立保存自己的目标 UUID。
 *
 * 不使用静态 Map。
 * 不保存实体强引用。
 */
public final class TargetTracker {

    public static final String TAG_TARGET =
            "TrackTarget";

    public static final String TAG_NEXT_SCAN =
            "TrackNextScan";


    @Nullable
    private UUID targetId;


    private long nextScanAt =
            Long.MIN_VALUE;


    // =========================================================
    // 更新目标
    // =========================================================

    @Nullable
    public LivingEntity update(
            Level level,
            Vec3 center,
            double scanRadius,
            double loseRange,
            long gameTime,
            long scanIntervalTicks,
            @Nullable LivingEntity ignore
    ) {


        LivingEntity current = null;


        // =====================================================
        // 获取当前目标
        // =====================================================

        if (targetId != null) {

            current =
                    Targeting.resolve(
                            level,
                            targetId
                    );
        }


        // =====================================================
        // 当前目标是否有效
        // =====================================================

        boolean currentValid =
                current != null
                        && current.isAlive()
                        && center.distanceToSqr(
                        current.position()
                )
                        <= loseRange * loseRange;


        // =====================================================
        // 每 tick 扫描
        // =====================================================

        if (gameTime >= nextScanAt) {

            nextScanAt =
                    gameTime
                            + Math.max(
                            1L,
                            scanIntervalTicks
                    );


            LivingEntity found =
                    Targeting.findTarget(
                            level,
                            center,
                            scanRadius,
                            ignore
                    );


            // -------------------------------------------------
            // 找到新目标
            // -------------------------------------------------

            if (found != null) {

                targetId =
                        found.getUUID();

                return found;
            }


            // -------------------------------------------------
            // 没找到新目标
            //
            // 但旧目标仍然有效
            // -------------------------------------------------

            if (currentValid) {

                return current;
            }


            // -------------------------------------------------
            // 完全丢失
            // -------------------------------------------------

            targetId = null;

            return null;
        }


        // =====================================================
        // 非扫描 tick
        // =====================================================

        if (currentValid) {

            return current;
        }


        return null;
    }


    // =========================================================
    // 查看当前目标
    // =========================================================

    @Nullable
    public LivingEntity peek(
            Level level
    ) {

        if (targetId == null) {
            return null;
        }


        LivingEntity target =
                Targeting.resolve(
                        level,
                        targetId
                );


        if (target == null
                || !target.isAlive()) {

            return null;
        }


        return target;
    }


    // =========================================================
    // 清除目标
    // =========================================================

    public void clear() {

        targetId = null;

        nextScanAt =
                Long.MIN_VALUE;
    }


    // =========================================================
    // 是否存在目标
    // =========================================================

    public boolean hasTarget() {

        return targetId != null;
    }


    // =========================================================
    // 保存
    // =========================================================

    public void save(
            CompoundTag tag
    ) {

        if (targetId != null) {

            tag.putUUID(
                    TAG_TARGET,
                    targetId
            );
        }


        if (nextScanAt != Long.MIN_VALUE) {

            tag.putLong(
                    TAG_NEXT_SCAN,
                    nextScanAt
            );
        }
    }


    // =========================================================
    // 加载
    // =========================================================

    public void load(
            CompoundTag tag
    ) {

        targetId =
                tag.hasUUID(TAG_TARGET)
                        ? tag.getUUID(TAG_TARGET)
                        : null;


        nextScanAt =
                tag.contains(TAG_NEXT_SCAN)
                        ? tag.getLong(TAG_NEXT_SCAN)
                        : Long.MIN_VALUE;
    }
}