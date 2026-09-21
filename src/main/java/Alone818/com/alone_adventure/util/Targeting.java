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

/**
 * 目标追踪算法 —— 服务于攻击追踪类物品（追踪弹、自动瞄准哨戒等）。
 *
 * <b>节省内存的设计</b>：
 * <ul>
 *   <li>纯函数集合，<b>无任何静态缓存 / 全局状态</b>，不持有实体引用；
 *       跨 tick 的目标一律以 UUID 记录（配合 {@link TargetTracker}），
 *       每次使用时从世界按 UUID 重解析，避免持有已卸载区块实体的强引用导致内存泄漏</li>
 *   <li>单次扫描只做一次 AABB 查询 + 一遍线性遍历，无流 / 装箱 / 迭代器垃圾</li>
 *   <li>建议用 {@link TargetTracker} 节流扫描（默认每 10 tick 一次），
 *       有效目标在两次扫描之间零开销（仅一次 UUID 哈希查表）</li>
 * </ul>
 *
 * <b>目标优先级</b>（优先攻击非玩家敌对生物）：
 * <ol>
 *   <li>敌对生物（{@link Enemy}，非玩家）—— 同级取最近</li>
 *   <li>其他非玩家生物（动物、中立等）—— 同级取最近</li>
 *   <li>玩家（最后考虑）—— 最近</li>
 * </ol>
 *
 * 用法示例（追踪弹体每 tick）：
 * <pre>{@code
 * TargetTracker tracker = new TargetTracker();
 * LivingEntity target = tracker.update(level, position(), 16.0D, 32.0D,
 *         level.getGameTime(), 10L, owner);
 * if (target != null) {
 *     setDeltaMovement(Targeting.steer(getDeltaMovement(), position(),
 *             target.getEyePosition(), 6.0F));
 * }
 * }</pre>
 */
public final class Targeting {

    private Targeting() {
    }

    /**
     * 扫描范围内选取优先级最高的目标。
     *
     * @param level   世界（服务端/客户端皆可）
     * @param center  扫描中心
     * @param radius  扫描半径（格）
     * @param ignore  需要排除的实体（通常是持有者/发射者），可为 null
     * @return 优先级最高且最近的目标；无可用目标返回 null
     */
    @Nullable
    public static LivingEntity findTarget(Level level, Vec3 center, double radius,
                                          @Nullable LivingEntity ignore) {
        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);

        // 三个优先级层的最优候选（层内取最近）
        LivingEntity bestEnemy = null;
        LivingEntity bestCreature = null;
        LivingEntity bestPlayer = null;
        double bestEnemyDist = Double.MAX_VALUE;
        double bestCreatureDist = Double.MAX_VALUE;
        double bestPlayerDist = Double.MAX_VALUE;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive() || entity.isSpectator()) continue;
            if (entity == ignore) continue;
            if (entity instanceof ArmorStand) continue; // 盔甲架不是有效目标
            // 不打发射者自己驯养的宠物
            if (entity instanceof TamableAnimal tame && ignore != null
                    && ignore.getUUID().equals(tame.getOwnerUUID())) continue;

            double dist = entity.distanceToSqr(center.x, center.y, center.z);
            if (entity instanceof Enemy) {
                if (dist < bestEnemyDist) {
                    bestEnemy = entity;
                    bestEnemyDist = dist;
                }
            } else if (entity instanceof Player) {
                if (dist < bestPlayerDist) {
                    bestPlayer = entity;
                    bestPlayerDist = dist;
                }
            } else {
                if (dist < bestCreatureDist) {
                    bestCreature = entity;
                    bestCreatureDist = dist;
                }
            }
        }

        // 优先级：敌对生物 > 其他非玩家生物 > 玩家
        if (bestEnemy != null) return bestEnemy;
        if (bestCreature != null) return bestCreature;
        return bestPlayer;
    }

    /**
     * 按 UUID 安全重解析目标实体（不持有引用，跨区块安全）。
     * UUID 实体表只在服务端存在，追踪计算应放在服务端权威侧（伤害/转向均在服务端结算，
     * 弹体位置由原版同步到客户端）；客户端调用恒返回 null。
     */
    @Nullable
    public static LivingEntity resolve(Level level, java.util.UUID id) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            return server.getEntity(id) instanceof LivingEntity living ? living : null;
        }
        return null;
    }

    /**
     * 两点间是否有视线（单个方块射线检测；调用方自行节流）。
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to) {
        BlockHitResult result = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return result.getType() == HitResult.Type.MISS;
    }

    /**
     * 追踪转向：把当前速度方向朝目标平滑旋转，保持速度大小不变。
     *
     * @param velocity         当前速度
     * @param from             弹体当前位置
     * @param to               目标位置（通常为目标眼部）
     * @param maxTurnDegrees   每 tick 最大转向角（度）
     * @return 转向后的新速度
     */
    public static Vec3 steer(Vec3 velocity, Vec3 from, Vec3 to, float maxTurnDegrees) {
        Vec3 desired = to.subtract(from);
        double speed = velocity.length();
        if (desired.lengthSqr() < 1.0E-4D || speed < 1.0E-4D) {
            return velocity;
        }

        Vec3 currentDir = velocity.scale(1.0D / speed);
        Vec3 desiredDir = desired.normalize();
        double dot = Mth.clamp(currentDir.dot(desiredDir), -1.0D, 1.0D);
        double angle = Math.toDegrees(Math.acos(dot));
        if (angle < 1.0E-3D) {
            return velocity; // 已对准
        }

        // 线性插值 + 归一化近似球面插值（小角度误差可忽略，零额外分配成本）
        double t = Math.min(1.0D, maxTurnDegrees / angle);
        Vec3 newDir = currentDir.scale(1.0D - t).add(desiredDir.scale(t)).normalize();
        return newDir.scale(speed);
    }
}
