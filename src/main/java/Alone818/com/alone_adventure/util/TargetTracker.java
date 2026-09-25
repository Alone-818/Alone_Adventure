package Alone818.com.alone_adventure.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TargetTracker {

    public static final String TAG_TARGET =
            "TrackTarget";

    public static final String TAG_NEXT_SCAN =
            "TrackNextScan";

    @Nullable
    private UUID targetId;

    private long nextScanAt =
            Long.MIN_VALUE;

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

        /*
         * 获取当前锁定目标。
         */
        if (targetId != null) {

            current =
                    Targeting.resolve(
                            level,
                            targetId
                    );
        }

        /*
         * 当前目标是否还有效。
         */
        boolean currentValid =
                current != null
                        && current.isAlive()
                        && center.distanceToSqr(
                        current.position()
                )
                        <= loseRange * loseRange;

        /*
         * 还没到扫描时间。
         *
         * 直接保持当前目标。
         */
        if (gameTime < nextScanAt) {

            return currentValid
                    ? current
                    : null;
        }

        /*
         * 下一次扫描。
         */
        nextScanAt =
                gameTime
                        + Math.max(
                        1L,
                        scanIntervalTicks
                );

        /*
         * 如果当前目标还有效，
         * 不重新换目标。
         *
         * 这样追踪会更加稳定。
         */
        if (currentValid) {
            return current;
        }

        /*
         * 当前目标已经丢失，
         * 才寻找新目标。
         */
        LivingEntity found =
                Targeting.findTarget(
                        level,
                        center,
                        scanRadius,
                        ignore
                );

        if (found != null) {

            targetId =
                    found.getUUID();

            return found;
        }

        targetId = null;

        return null;
    }

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

    public void clear() {

        targetId = null;
        nextScanAt =
                Long.MIN_VALUE;
    }

    public boolean hasTarget() {

        return targetId != null;
    }

    public void save(
            CompoundTag tag
    ) {

        if (targetId != null) {

            tag.putUUID(
                    TAG_TARGET,
                    targetId
            );
        }

        if (nextScanAt
                != Long.MIN_VALUE) {

            tag.putLong(
                    TAG_NEXT_SCAN,
                    nextScanAt
            );
        }
    }

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