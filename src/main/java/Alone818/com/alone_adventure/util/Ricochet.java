package Alone818.com.alone_adventure.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 反弹算法 —— 服务于攻击反弹类物品（弹射子弹、反弹法术等）。
 *
 * <b>节省内存的设计</b>：纯函数集合，<b>无任何静态缓存 / 全局状态 / 保留引用</b>；
 * 反弹次数等状态由调用方（通常是投射物自身的 NBT 整数字段）维护。
 *
 * 提供三层 API（按需取用）：
 * <ol>
 *   <li>{@link #reflect} —— 最底层：向量镜面反射 v' = v − 2(v·n)n，
 *       可附能量保留系数（反弹后速度按比例衰减）</li>
 *   <li>{@link #surfaceNormal} —— 从方块命中结果取命中面法线</li>
 *   <li>{@link #applyBounce} —— 实用封装：任意投射物实体一步反弹
 *       （反射速度 + 位置回退到命中面外，防止下一 tick 重复命中同一面）</li>
 * </ol>
 *
 * 反弹索敌（台球式）可与其组合：反弹后调用 {@link Targeting#steer}
 * 朝 {@link Targeting#findTarget} 选出的新目标转向即可。
 */
public final class Ricochet {

    private Ricochet() {
    }

    /**
     * 向量镜面反射：v' = v − 2(v·n)n（入射角 = 反射角），速度大小按
     * 能量保留系数衰减（1.0 = 完全弹性，0.5 = 反弹后速度减半）。
     *
     * @param velocity        入射速度
     * @param normal          命中面法线（无需归一化）
     * @param energyRetention 能量保留系数 0~1
     * @return 反射后的速度
     */
    public static Vec3 reflect(Vec3 velocity, Vec3 normal, float energyRetention) {
        Vec3 n = normal.normalize();
        double dot = velocity.dot(n);
        Vec3 reflected = velocity.subtract(n.scale(2.0D * dot));
        return reflected.scale(Mth.clamp(energyRetention, 0.0F, 1.0F));
    }

    /** 从方块命中结果取命中面法线（朝向命中面外侧） */
    public static Vec3 surfaceNormal(BlockHitResult hit) {
        Direction face = hit.getDirection();
        return new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    /**
     * 实用反弹：对任意投射物实体一步完成——
     * 速度按命中面镜面反射（附能量保留），位置回退到命中点沿反射方向外侧，
     * 避免下一 tick 再次判定同一命中面。
     *
     * @param projectile      投射物实体
     * @param hit             本次方块命中结果
     * @param energyRetention 反弹能量保留系数 0~1
     * @return 是否成功反弹（法线或速度异常时 false，调用方应按普通命中处理）
     */
    public static boolean applyBounce(Entity projectile, BlockHitResult hit, float energyRetention) {
        Vec3 normal = surfaceNormal(hit);
        Vec3 velocity = projectile.getDeltaMovement();
        if (normal.lengthSqr() < 1.0E-6D || velocity.lengthSqr() < 1.0E-9D) {
            return false;
        }

        Vec3 reflected = reflect(velocity, normal, energyRetention);
        projectile.setDeltaMovement(reflected);

        // 回退到命中面外（沿反射方向少量偏移）
        Vec3 at = hit.getLocation();
        projectile.setPos(
                at.x + reflected.x * 0.05D,
                at.y + reflected.y * 0.05D,
                at.z + reflected.z * 0.05D);
        return true;
    }
}
