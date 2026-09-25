package Alone818.com.alone_adventure.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Targeting {

    private Targeting() {
    }

    /**
     * 追踪弹搜索目标范围。
     */
    public static final double TRACKING_RANGE = 16.0D;

    /**
     * 目标最大保持距离。
     */
    public static final double TRACKING_LOSE_RANGE = 20.0D;

    /**
     * 最大转向角。
     *
     * 数值越小，追踪越缓慢。
     */
    private static final float MAX_TRACK_TURN = 4.0F;

    /**
     * 目标移动预测时间。
     *
     * 数值越小越不容易出现“导弹感”。
     */
    private static final double PREDICTION_TICKS = 1.0D;

    /**
     * 寻找目标。
     */
    @Nullable
    public static LivingEntity findTarget(
            Level level,
            Vec3 center,
            double radius,
            @Nullable LivingEntity ignore
    ) {

        AABB box = new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );

        LivingEntity bestEnemy = null;
        LivingEntity bestCreature = null;
        LivingEntity bestPlayer = null;

        double bestEnemyDist = Double.MAX_VALUE;
        double bestCreatureDist = Double.MAX_VALUE;
        double bestPlayerDist = Double.MAX_VALUE;

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box
                )) {

            if (!entity.isAlive()) {
                continue;
            }

            if (entity.isSpectator()) {
                continue;
            }

            if (entity == ignore) {
                continue;
            }

            if (entity instanceof ArmorStand) {
                continue;
            }

            /*
             * 不追踪发射者自己的驯服动物。
             */
            if (entity instanceof TamableAnimal tame
                    && ignore != null
                    && tame.getOwnerUUID() != null
                    && tame.getOwnerUUID().equals(
                    ignore.getUUID()
            )) {

                continue;
            }

            double distance =
                    entity.distanceToSqr(
                            center
                    );

            if (entity instanceof Enemy) {

                if (distance < bestEnemyDist) {
                    bestEnemy = entity;
                    bestEnemyDist = distance;
                }

            } else if (entity instanceof Player) {

                if (distance < bestPlayerDist) {
                    bestPlayer = entity;
                    bestPlayerDist = distance;
                }

            } else {

                if (distance < bestCreatureDist) {
                    bestCreature = entity;
                    bestCreatureDist = distance;
                }
            }
        }

        if (bestEnemy != null) {
            return bestEnemy;
        }

        if (bestCreature != null) {
            return bestCreature;
        }

        return bestPlayer;
    }

    /**
     * 根据 UUID 获取目标。
     */
    @Nullable
    public static LivingEntity resolve(
            Level level,
            UUID id
    ) {

        if (level instanceof net.minecraft.server.level.ServerLevel server) {

            EntityHolder:
            {
                net.minecraft.world.entity.Entity entity =
                        server.getEntity(id);

                if (entity instanceof LivingEntity living) {
                    return living;
                }
            }
        }

        return null;
    }

    /**
     * 检查视线。
     */
    public static boolean hasLineOfSight(
            Level level,
            Vec3 from,
            Vec3 to
    ) {

        BlockHitResult result =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                null
                        )
                );

        return result.getType()
                == HitResult.Type.MISS;
    }

    /**
     * 普通转向。
     */
    public static Vec3 steer(
            Vec3 velocity,
            Vec3 from,
            Vec3 to,
            float maxTurnDegrees
    ) {

        Vec3 desired =
                to.subtract(from);

        if (desired.lengthSqr()
                < 1.0E-6D) {

            return velocity;
        }

        double speed =
                velocity.length();

        if (speed < 0.001D) {
            return velocity;
        }

        Vec3 currentDirection =
                velocity.normalize();

        Vec3 desiredDirection =
                desired.normalize();

        double dot =
                Mth.clamp(
                        currentDirection.dot(
                                desiredDirection
                        ),
                        -1.0D,
                        1.0D
                );

        double angle =
                Math.toDegrees(
                        Math.acos(dot)
                );

        if (angle < 0.001D) {
            return velocity;
        }

        /*
         * 每 tick 最多转指定角度。
         */
        double turnRatio =
                Math.min(
                        1.0D,
                        maxTurnDegrees / angle
                );

        Vec3 newDirection =
                currentDirection
                        .scale(
                                1.0D - turnRatio
                        )
                        .add(
                                desiredDirection.scale(
                                        turnRatio
                                )
                        );

        if (newDirection.lengthSqr()
                < 1.0E-8D) {

            return velocity;
        }

        return newDirection
                .normalize()
                .scale(speed);
    }

    /**
     * 缓慢追踪。
     *
     * 不改变子弹原本速度。
     */
    public static Vec3 slowSteer(
            Vec3 velocity,
            Vec3 from,
            LivingEntity target
    ) {

        if (target == null
                || !target.isAlive()) {

            return velocity;
        }

        /*
         * 目标位置。
         */
        Vec3 targetPosition =
                target.getEyePosition();

        /*
         * 非常轻微的目标预测。
         */
        Vec3 targetVelocity =
                target.getDeltaMovement();

        targetPosition =
                targetPosition.add(
                        targetVelocity.scale(
                                PREDICTION_TICKS
                        )
                );

        /*
         * 每 tick 最多只转 4°。
         */
        return steer(
                velocity,
                from,
                targetPosition,
                MAX_TRACK_TURN
        );
    }
}