package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 投掷电击器的弹体。
 *
 * 掷出后（含飞行途中）每 {@value #SHOCK_INTERVAL_TICKS} tick 对周围
 * {@value #SHOCK_RADIUS} 格内的所有生物放电一次，每次造成
 * {@value #SHOCK_DAMAGE} 点伤害（不伤投掷者）；
 * 命中实体或方块后不再消失，而是就地锚定、继续放电
 * {@value #SHOCK_DURATION_TICKS} tick 后烧毁消散。
 *
 * 高频低伤的实现与 {@link chainsawsword} 扫射一致：命中前把
 * {@code invulnerableTime} 归零以突破原版无敌帧，否则每秒只有第一击真正生效。
 */
public class ShockDeviceProjectile extends ThrowableItemProjectile {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 电击间隔：每 0.1 秒（2 tick）放电一次，即每秒 10 次 */
    public static int SHOCK_INTERVAL_TICKS = 2;
    /** 每次电击的伤害（点） */
    public static float SHOCK_DAMAGE = 0.5F;
    /** 电击半径（格） */
    public static float SHOCK_RADIUS = 4.0F;
    /** 命中锚定后的持续放电时长（tick，100 = 5 秒） */
    public static int SHOCK_DURATION_TICKS = 100;

    /** 飞行阶段兜底寿命（30 秒）：防止一直不落地的弹体常驻世界 */
    private static final int MAX_LIFE_TICKS = 600;

    /** 锚定标记（同步客户端，使其同样停止本地运动模拟） */
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
            SynchedEntityData.defineId(ShockDeviceProjectile.class, EntityDataSerializers.BOOLEAN);

    /** NBT 标签：锚定后已放电的 tick 数 */
    private static final String TAG_ANCHORED_TICKS = "ShockAnchoredTicks";

    /** 锚定后经过的 tick 数（服务端权威） */
    private int anchoredTicks;

    public ShockDeviceProjectile(EntityType<? extends ShockDeviceProjectile> type, Level level) {
        super(type, level);
    }

    public ShockDeviceProjectile(EntityType<? extends ShockDeviceProjectile> type,
                                 LivingEntity thrower, Level level, ItemStack device) {
        super(type, thrower, level);
        setItem(device);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ANCHORED, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SHOCK_DEVICE.get();
    }

    public boolean isAnchored() {
        return this.entityData.get(DATA_ANCHORED);
    }

    // ===== 主循环 =====

    @Override
    public void tick() {
        if (isAnchored()) {
            // 锚定后跳过原版投掷物逻辑（命中检测/重力/位移全部不走），
            // 只保留基础 tick（着火、水流等环境结算），位置由服务端权威保持
            this.baseTick();
        } else {
            super.tick();
        }
        if (!level().isClientSide) {
            serverTick();
        } else {
            spawnAmbientSparks();
        }
    }

    /** 服务端每 tick 结算：寿命/烧毁判定 + 按间隔放电 */
    private void serverTick() {
        if (isAnchored()) {
            // 放完持续时长即烧毁
            if (++anchoredTicks > SHOCK_DURATION_TICKS) {
                burnOut();
                return;
            }
        } else if (this.tickCount > MAX_LIFE_TICKS) {
            discard();
            return;
        }
        // 间隔节流：用实体自身 tickCount 判定，与链锯剑扫射一致
        if (this.tickCount % SHOCK_INTERVAL_TICKS != 0) return;
        discharge();
    }

    /**
     * 放电：对半径内所有生物造成一次低伤电击。
     * 命中前把 {@code invulnerableTime} 归零，使高频低伤真正生效。
     */
    private void discharge() {
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.15F, 1.9F);

        AABB area = new AABB(
                getX() - SHOCK_RADIUS, getY() - SHOCK_RADIUS, getZ() - SHOCK_RADIUS,
                getX() + SHOCK_RADIUS, getY() + SHOCK_RADIUS, getZ() + SHOCK_RADIUS);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == getOwner()) continue; // 不电投掷者自己
            target.invulnerableTime = 0;
            target.hurt(damageSources().thrown(this, getOwner()), SHOCK_DAMAGE);
            // 取消击退：清零水平速度，仅保留竖直分量
            target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
            // 被电击的生物获得缓慢 I（1.5 秒，90 tick）
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 90, 0));
            // 命中处迸出电火花（服务端广播，所有人可见）
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        3, 0.2D, 0.3D, 0.2D, 0.1D);
            }
        }
    }

    /** 电能耗尽：火花四溅后消散 */
    private void burnOut() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 0.1D, getZ(), 40, 0.5D, 0.4D, 0.5D, 0.3D);
            server.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY() + 0.1D, getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 0.7F);
        discard();
    }

    /** 客户端氛围粒子：本体周围持续冒出电火花（飞行时即电光轨迹） */
    private void spawnAmbientSparks() {
        for (int i = 0; i < 2; ++i) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    getY() + this.random.nextDouble() * 0.4D,
                    getZ() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    // ===== 命中锚定 =====

    /** 命中实体：不造成撞击伤害，就地锚定（命中者随即进入电击半径） */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        anchor();
    }

    /** 命中方块：就地锚定 */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        anchor();
    }

    /** 锚定：固定当前位置，开始持续放电（仅服务端，客户端靠同步数据跟进） */
    private void anchor() {
        if (level().isClientSide || isAnchored()) return;
        setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_ANCHORED, true);
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 1.5F);
    }

    // ===== 存档 =====

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Anchored", isAnchored());
        tag.putInt(TAG_ANCHORED_TICKS, anchoredTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_ANCHORED, tag.getBoolean("Anchored"));
        this.anchoredTicks = tag.getInt(TAG_ANCHORED_TICKS);
    }
}
