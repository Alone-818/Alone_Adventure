package Alone818.com.alone_adventure.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;


/**
 * 枪械子弹追踪辅助。
 *
 * 设计目标：
 *
 * 1. 追踪能力明显强于原版本
 * 2. 不产生瞬间转向
 * 3. 对移动目标进行提前预测
 * 4. 敌对生物优先
 * 5. 排除枪手自己
 * 6. 排除枪手驯服的动物
 * 7. 所有实际伤害与目标选择仍由服务端控制
 */
public final class Targeting {

    private Targeting() {
    }


    // =========================================================
    // 追踪参数
    // =========================================================

    /**
     * 默认搜索范围。
     */
    public static final double TRACKING_RANGE = 32.0D;


    /**
     * 当前目标最大保持距离。
     *
     * 比搜索距离更大。
     */
    public static final double TRACKING_LOSE_RANGE = 48.0D;


    /**
     * 每 tick 最大转向角。
     *
     * 原来只有 4°。
     *
     * 现在提高到 12°。
     *
     * 12°/tick 已经属于明显的强追踪，
     * 但不会像导弹一样瞬间拐弯。
     */
    private static final float MAX_TRACK_TURN = 12.0F;


    /**
     * 移动目标预测时间。
     *
     * 4 tick = 0.2 秒。
     *
     * 对玩家横向移动会明显更容易命中。
     */
    private static final double PREDICTION_TICKS = 4.0D;


    // =========================================================
    // 寻找目标
    // =========================================================

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


            // -------------------------------------------------
            // 基础过滤
            // -------------------------------------------------

            if (!entity.isAlive()) {
                continue;
            }


            if (entity.isSpectator()) {
                continue;
            }


            if (entity == ignore) {
                continue;
            }


            // -------------------------------------------------
            // 不追踪枪手自己的宠物
            // -------------------------------------------------

            if (entity instanceof TamableAnimal tame
                    && ignore != null
                    && tame.getOwnerUUID() != null
                    && tame.getOwnerUUID().equals(
                    ignore.getUUID()
            )) {

                continue;
            }


            // -------------------------------------------------
            // ArmorStand 不作为追踪目标
            // -------------------------------------------------

            if (entity instanceof ArmorStand) {
                continue;
            }


            double distance =
                    entity.distanceToSqr(center);


            // -------------------------------------------------
            // 敌对生物
            // -------------------------------------------------

            if (entity instanceof Enemy) {

                if (distance < bestEnemyDist) {

                    bestEnemy =
                            entity;

                    bestEnemyDist =
                            distance;
                }

                continue;
            }


            // -------------------------------------------------
            // 玩家
            // -------------------------------------------------

            if (entity instanceof Player) {

                if (distance < bestPlayerDist) {

                    bestPlayer =
                            entity;

                    bestPlayerDist =
                            distance;
                }

                continue;
            }


            // -------------------------------------------------
            // 普通生物
            // -------------------------------------------------

            if (distance < bestCreatureDist) {

                bestCreature =
                        entity;

                bestCreatureDist =
                        distance;
            }
        }


        // =====================================================
        // 目标优先级
        // =====================================================

        if (bestEnemy != null) {
            return bestEnemy;
        }


        if (bestCreature != null) {
            return bestCreature;
        }


        return bestPlayer;
    }


    // =========================================================
    // 根据 UUID 获取实体
    // =========================================================

    @Nullable
    public static LivingEntity resolve(
            Level level,
            UUID id
    ) {

        if (level instanceof net.minecraft.server.level.ServerLevel server) {

            net.minecraft.world.entity.Entity entity =
                    server.getEntity(id);

            if (entity instanceof LivingEntity living) {

                return living;
            }
        }

        return null;
    }


    // =========================================================
    // 方块视线检测
    // =========================================================

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


    // =========================================================
    // 强力转向
    // =========================================================

    public static Vec3 steer(
            Vec3 velocity,
            Vec3 from,
            Vec3 to,
            float maxTurnDegrees
    ) {

        if (velocity.lengthSqr() < 1.0E-8D) {
            return velocity;
        }


        Vec3 desired =
                to.subtract(from);


        if (desired.lengthSqr() < 1.0E-8D) {
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


        // -----------------------------------------------------
        // 最大转向限制
        // -----------------------------------------------------

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


    // =========================================================
    // 强力追踪
    // =========================================================

    public static Vec3 slowSteer(
            Vec3 velocity,
            Vec3 from,
            LivingEntity target
    ) {

        if (target == null
                || !target.isAlive()) {

            return velocity;
        }


        // -----------------------------------------------------
        // 目标眼睛位置
        // -----------------------------------------------------

        Vec3 targetPosition =
                target.getEyePosition();


        // -----------------------------------------------------
        // 目标移动预测
        // -----------------------------------------------------

        Vec3 targetVelocity =
                target.getDeltaMovement();


        targetPosition =
                targetPosition.add(
                        targetVelocity.scale(
                                PREDICTION_TICKS
                        )
                );


        // -----------------------------------------------------
        // 强力转向
        // -----------------------------------------------------

        return steer(
                velocity,
                from,
                targetPosition,
                MAX_TRACK_TURN
        );
    }
}