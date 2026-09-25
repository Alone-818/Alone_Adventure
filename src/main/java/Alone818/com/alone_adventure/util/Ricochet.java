package Alone818.com.alone_adventure.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 反弹算法 —— 服务于攻击反弹类物品。
 *
 * <p>
 * 适用于：
 * </p>
 * <ul>
 *     <li>反弹子弹</li>
 *     <li>弹射法术</li>
 *     <li>反弹投射物</li>
 * </ul>
 *
 * <p>
 * 设计为纯工具类：
 * </p>
 * <ul>
 *     <li>无静态缓存</li>
 *     <li>无全局状态</li>
 *     <li>不保存实体引用</li>
 *     <li>反弹次数由调用方维护</li>
 * </ul>
 */
public final class Ricochet {

    /**
     * 防止反弹后再次卡入方块的基础偏移量。
     */
    private static final double POSITION_EPSILON =
            0.05D;

    /**
     * 防止极小速度导致异常计算。
     */
    private static final double MIN_VELOCITY_SQR =
            1.0E-9D;

    private Ricochet() {
    }

    /**
     * 向量镜面反射。
     *
     * <p>
     * 公式：
     * </p>
     *
     * <pre>
     * v' = v - 2(v·n)n
     * </pre>
     *
     * @param velocity 入射速度
     * @param normal 命中面法线
     * @param energyRetention 能量保留比例
     * @return 反射后的速度
     */
    public static Vec3 reflect(
            Vec3 velocity,
            Vec3 normal,
            float energyRetention
    ) {

        /*
         * 法线为空。
         */
        if (normal.lengthSqr()
                < MIN_VELOCITY_SQR) {

            return velocity;
        }

        /*
         * Direction 产生的法线本身就是单位向量，
         * 但这个 API 也允许外部传入普通 Vec3，
         * 所以这里仍然进行 normalize。
         */
        Vec3 n =
                normal.normalize();

        /*
         * 入射速度在法线方向上的投影。
         */
        double dot =
                velocity.dot(n);

        /*
         * 镜面反射。
         */
        Vec3 reflected =
                velocity.subtract(
                        n.scale(
                                2.0D * dot
                        )
                );

        /*
         * 限制能量保留范围。
         */
        float retention =
                Mth.clamp(
                        energyRetention,
                        0.0F,
                        1.0F
                );

        return reflected.scale(
                retention
        );
    }

    /**
     * 从方块命中结果取得表面法线。
     *
     * @param hit 方块命中结果
     * @return 单位表面法线
     */
    public static Vec3 surfaceNormal(
            BlockHitResult hit
    ) {

        Direction face =
                hit.getDirection();

        /*
         * Direction 的 step 值天然是：
         *
         * -1 / 0 / 1
         *
         * 因此这里不需要 normalize。
         */
        return new Vec3(
                face.getStepX(),
                face.getStepY(),
                face.getStepZ()
        );
    }

    /**
     * 判断速度是否正在撞向表面。
     *
     * <p>
     * dot < 0：
     * </p>
     *
     * <pre>
     * 子弹正在朝表面运动。
     * </pre>
     *
     * <p>
     * dot >= 0：
     * </p>
     *
     * <pre>
     * 子弹已经朝离开表面的方向运动。
     * </pre>
     *
     * @param velocity 当前速度
     * @param normal 表面法线
     * @return 是否正在撞向表面
     */
    public static boolean isApproaching(
            Vec3 velocity,
            Vec3 normal
    ) {

        return velocity.dot(normal) < 0.0D;
    }

    /**
     * 对投射物执行一次反弹。
     *
     * <p>
     * 完成：
     * </p>
     *
     * <ol>
     *     <li>取得命中面法线</li>
     *     <li>计算镜面反射</li>
     *     <li>应用能量保留</li>
     *     <li>重新设置速度</li>
     *     <li>将实体推出命中面</li>
     * </ol>
     *
     * @param projectile 投射物
     * @param hit 方块命中结果
     * @param energyRetention 能量保留比例
     * @return 是否成功反弹
     */
    public static boolean applyBounce(
            Entity projectile,
            BlockHitResult hit,
            float energyRetention
    ) {

        /*
         * 防止空引用。
         */
        if (projectile == null
                || hit == null) {

            return false;
        }

        /*
         * 获取法线。
         */
        Vec3 normal =
                surfaceNormal(hit);

        if (normal.lengthSqr()
                < MIN_VELOCITY_SQR) {

            return false;
        }

        /*
         * 获取当前速度。
         */
        Vec3 velocity =
                projectile.getDeltaMovement();

        if (velocity.lengthSqr()
                < MIN_VELOCITY_SQR) {

            return false;
        }

        /*
         * 如果子弹已经不是朝表面运动，
         * 不应该再次进行镜面反射。
         *
         * 这样可以减少某些特殊碰撞情况下
         * 的连续反弹。
         */
        if (!isApproaching(
                velocity,
                normal
        )) {

            return false;
        }

        /*
         * 计算反射速度。
         */
        Vec3 reflected =
                reflect(
                        velocity,
                        normal,
                        energyRetention
                );

        if (reflected.lengthSqr()
                < MIN_VELOCITY_SQR) {

            return false;
        }

        /*
         * 设置反弹速度。
         */
        projectile.setDeltaMovement(
                reflected
        );

        /*
         * =====================================================
         * 位置修正
         * =====================================================
         *
         * 原来的实现：
         *
         *     hitPoint + reflected * 0.05
         *
         * 这个方向并不一定是方块表面的外侧。
         *
         * 例如斜着撞墙时，
         * reflected 同时包含切向和法向分量，
         * 因此可能仍然停留在碰撞面附近。
         *
         * 现在改为：
         *
         *     hitPoint + normal * epsilon
         *
         * 直接沿命中面的法线推出去。
         */
        Vec3 at =
                hit.getLocation();

        projectile.setPos(
                at.x
                        + normal.x
                        * POSITION_EPSILON,

                at.y
                        + normal.y
                        * POSITION_EPSILON,

                at.z
                        + normal.z
                        * POSITION_EPSILON
        );

        /*
         * 告知 Minecraft 实体运动发生变化。
         */
        projectile.hasImpulse = true;

        return true;
    }
}