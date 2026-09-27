package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.util.Penetration;
import Alone818.com.alone_adventure.util.Ricochet;
import Alone818.com.alone_adventure.util.TargetTracker;
import Alone818.com.alone_adventure.util.Targeting;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * 子弹实体。
 *
 * 支持：
 * - 普通子弹
 * - 穿透
 * - 反弹
 * - 击退
 * - 射程伤害衰减
 * - 特殊弹药
 * - 缓慢追踪
 */
public class BulletProjectile extends ThrowableItemProjectile {

    /**
     * 基础伤害。
     */
    private float damage = 1.0F;

    /**
     * 穿透次数。
     */
    private int penetration = 0;

    /**
     * 击退。
     */
    private float knockback = 0.2F;

    /**
     * 有效射程。
     */
    private float effectiveRange = 30.0F;

    /**
     * 是否追踪。
     */
    private boolean tracking = false;

    /**
     * 目标追踪器。
     */
    private final TargetTracker targetTracker =
            new TargetTracker();

    /**
     * 子弹发射位置。
     */
    private Vec3 startPos = null;

    /**
     * 来源枪械。
     */
    @Nullable
    private GunItem sourceGun = null;

    /**
     * 当前弹药。
     */
    @Nullable
    private Item ammoItem = null;

    /**
     * 弹药数据。
     */
    private final CompoundTag ammoData =
            new CompoundTag();

    /**
     * 已经穿透过的实体。
     */
    private final Penetration.Record pierced =
            new Penetration.Record();

    /**
     * 反弹次数。
     */
    private static final EntityDataAccessor<Integer>
            DATA_RICOCHET =
            SynchedEntityData.defineId(
                    BulletProjectile.class,
                    EntityDataSerializers.INT
            );

    /**
     * 反弹速度保留比例。
     */
    private static final float RICOCHET_ENERGY = 0.7F;

    /**
     * 最大寿命。
     */
    private static final int MAX_LIFE_TICKS = 100;

    /**
     * 最低伤害比例。
     */
    private static final float MIN_DAMAGE_RATIO = 0.25F;

    /*
     * =========================================================
     * 追踪参数
     * =========================================================
     */

    /**
     * 追踪搜索范围。
     *
     * 只会在这个范围内寻找目标。
     */
    private static final double TRACKING_RANGE = 24.0D;

    /**
     * 已锁定目标的最大保持范围。
     */
    private static final double TRACKING_LOSE_RANGE = 32.0D;

    /**
     * 每多少 tick 扫描一次目标。
     */
    private static final long TRACKING_SCAN_INTERVAL = 2L;

    /**
     * 最大转向角。
     *
     * 这是之前强追踪版本的约 50%。
     *
     * 2° / tick。
     */
    private static final float TRACKING_TURN = 3.0F;

    /**
     * 目标预测时间。
     *
     * 防止完全没有提前量。
     */
    private static final double TRACKING_PREDICTION = 1.0D;

    /**
     * 保存子弹初始飞行速度。
     *
     * 只用于保证追踪过程中不会因为追踪
     * 而改变原本的飞行速度。
     */
    private double trackingSpeed = 0.0D;

    public BulletProjectile(
            EntityType<? extends BulletProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    public BulletProjectile(
            EntityType<? extends BulletProjectile> type,
            LivingEntity shooter,
            Level level
    ) {
        super(type, shooter, level);
    }

    @Override
    protected void defineSynchedData() {

        super.defineSynchedData();

        this.entityData.define(
                DATA_RICOCHET,
                0
        );
    }

    /**
     * 获取剩余反弹次数。
     */
    public int getBouncesLeft() {

        return this.entityData.get(
                DATA_RICOCHET
        );
    }

    /**
     * 配置普通子弹。
     */
    public void configure(
            GunItem sourceGun,
            Item ammoItem,
            float damage,
            int penetration,
            int ricochet,
            float knockback,
            float effectiveRange
    ) {

        configure(
                sourceGun,
                ammoItem,
                damage,
                penetration,
                ricochet,
                knockback,
                effectiveRange,
                false
        );
    }

    /**
     * 配置子弹。
     */
    public void configure(
            GunItem sourceGun,
            Item ammoItem,
            float damage,
            int penetration,
            int ricochet,
            float knockback,
            float effectiveRange,
            boolean tracking
    ) {

        this.sourceGun = sourceGun;
        this.ammoItem = ammoItem;

        this.damage = damage;
        this.penetration = penetration;
        this.knockback = knockback;
        this.effectiveRange = effectiveRange;

        this.tracking = tracking;

        this.startPos = position();

        /*
         * 记录发射时速度。
         *
         * 追踪不会修改这个速度。
         */
        double speed =
                getDeltaMovement().length();

        if (speed > 0.001D) {

            this.trackingSpeed = speed;

        } else {

            this.trackingSpeed = 0.0D;
        }

        this.entityData.set(
                DATA_RICOCHET,
                Math.max(
                        0,
                        ricochet
                )
        );
    }

    /**
     * 获取弹药数据。
     */
    public CompoundTag getAmmoData() {

        return ammoData;
    }

    /**
     * 获取弹药。
     */
    @Nullable
    public Item getAmmoItem() {

        return ammoItem;
    }

    @Override
    protected Item getDefaultItem() {

        return ModItems.BULLET.get();
    }

    /**
     * 子弹无重力。
     */
    @Override
    protected float getGravity() {

        return 0.0F;
    }

    /**
     * 子弹 Tick。
     */
    @Override
    public void tick() {

        super.tick();

        /*
         * =====================================================
         * 缓慢追踪
         * =====================================================
         */
        // =========================================================
// 强力追踪系统
// =========================================================
//
// 搜索范围：32 格
// 锁定保持：48 格
// 每 tick 更新
// 最大转向：12° / tick
// 移动预测：4 tick
//
// 服务端计算，防止客户端修改追踪结果。
// =========================================================

        if (!level().isClientSide
                && tracking
                && getOwner() instanceof LivingEntity owner) {


            // =====================================================
            // 获取目标
            // =====================================================

            LivingEntity target =
                    targetTracker.update(
                            level(),
                            position(),
                            32.0D,
                            48.0D,
                            level().getGameTime(),
                            1L,
                            owner
                    );


            // =====================================================
            // 有目标
            // =====================================================

            if (target != null
                    && target.isAlive()) {


                Vec3 velocity =
                        getDeltaMovement();


                double speed =
                        velocity.length();


                if (speed > 0.001D) {


                    // =================================================
                    // 目标预测
                    // =================================================
                    //
                    // 根据目标当前移动速度提前瞄准。
                    //
                    // 4 tick = 0.2 秒。
                    // =================================================

                    Vec3 predictedPosition =
                            target.getEyePosition()
                                    .add(
                                            target.getDeltaMovement()
                                                    .scale(4.0D)
                                    );


                    // =================================================
                    // 强力转向
                    // =================================================

                    Vec3 steered =
                            Targeting.steer(
                                    velocity,
                                    position(),
                                    predictedPosition,
                                    12.0F
                            );


                    // =================================================
                    // 应用速度
                    //
                    // steer 本身保持原速度大小，
                    // 所以追踪不会降低子弹初速。
                    // =================================================

                    setDeltaMovement(
                            steered
                    );


                    hasImpulse = true;
                }
            }
        }
        /*
         * =====================================================
         * 客户端曳光粒子
         * =====================================================
         */
        if (level().isClientSide) {

            level().addParticle(
                    ParticleTypes.CRIT,
                    getX(),
                    getY(),
                    getZ(),
                    0.0D,
                    0.0D,
                    0.0D
            );

        } else if (
                this.tickCount > MAX_LIFE_TICKS
        ) {

            discard();
        }
    }

    /**
     * 防止重复命中已经穿透过的实体。
     */
    @Override
    protected boolean canHitEntity(
            Entity target
    ) {

        return super.canHitEntity(target)
                && !pierced.hasHit(
                target.getId()
        );
    }

    /**
     * 命中实体。
     */
    @Override
    protected void onHitEntity(
            EntityHitResult result
    ) {

        if (level().isClientSide) {
            return;
        }

        Entity hit =
                result.getEntity();

        float actualDamage =
                damageAt();

        /*
         * 枪械命中回调。
         */
        if (level() instanceof ServerLevel serverLevel) {

            if (sourceGun != null) {

                sourceGun.onBulletHitEntity(
                        serverLevel,
                        this,
                        hit,
                        actualDamage
                );
            }
        }

        /*
         * 清除无敌时间。
         */
        if (hit instanceof LivingEntity living) {

            living.invulnerableTime = 0;
        }

        /*
         * 造成伤害。
         */
        hit.hurt(
                damageSources().thrown(
                        this,
                        getOwner()
                ),
                actualDamage
        );

        /*
         * 特殊弹药。
         */
        if (level() instanceof ServerLevel serverLevel) {

            if (ammoItem instanceof AmmoItem ammo) {

                ammo.onBulletHit(
                        serverLevel,
                        this,
                        hit,
                        actualDamage
                );
            }
        }

        /*
         * 击退。
         */
        if (knockback > 0) {

            Vec3 dir =
                    getDeltaMovement()
                            .normalize();

            hit.push(
                    dir.x * knockback,
                    0.1D * knockback + 0.05D,
                    dir.z * knockback
            );

            if (hit instanceof ServerPlayer serverPlayer) {

                serverPlayer.connection.send(
                        new ClientboundSetEntityMotionPacket(
                                serverPlayer
                        )
                );
            }
        }

        /*
         * 命中粒子。
         */
        if (level() instanceof ServerLevel server) {

            server.sendParticles(
                    ParticleTypes.CRIT,
                    hit.getX(),
                    hit.getY()
                            + hit.getBbHeight() * 0.5D,
                    hit.getZ(),
                    5,
                    0.15D,
                    0.2D,
                    0.15D,
                    0.08D
            );
        }

        level().playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.ARROW_HIT_PLAYER,
                SoundSource.PLAYERS,
                0.3F,
                1.6F
        );

        /*
         * 记录已经命中的实体。
         */
        pierced.record(
                hit.getId()
        );

        /*
         * 穿透耗尽。
         */
        if (pierced.exhausted(
                penetration
        )) {

            discard();
        }
    }

    /**
     * 命中方块。
     */
    @Override
    protected void onHitBlock(
            BlockHitResult result
    ) {

        /*
         * 特殊弹药。
         */
        if (!level().isClientSide
                && ammoItem instanceof AmmoItem ammo) {

            if (level() instanceof ServerLevel serverLevel) {

                ammo.onBulletHit(
                        serverLevel,
                        this,
                        null,
                        damageAt()
                );
            }

            /*
             * 保留原有爆炸弹药特殊处理。
             */
            if (ammo.getClass()
                    .getSimpleName()
                    .equals(
                            "short_bullet_explosive"
                    )) {

                return;
            }
        }

        /*
         * =====================================================
         * 反弹
         * =====================================================
         */
        if (getBouncesLeft() > 0
                && Ricochet.applyBounce(
                this,
                result,
                RICOCHET_ENERGY
        )) {

            this.entityData.set(
                    DATA_RICOCHET,
                    getBouncesLeft() - 1
            );

            /*
             * 反弹之后重新记录速度。
             *
             * 防止追踪速度继续使用反弹前速度。
             */
            if (tracking) {

                trackingSpeed =
                        getDeltaMovement().length();
            }

            if (!level().isClientSide) {

                level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        SoundEvents.ARROW_HIT,
                        SoundSource.PLAYERS,
                        0.3F,
                        2.2F
                );

                if (level() instanceof ServerLevel server) {

                    server.sendParticles(
                            ParticleTypes.CRIT,
                            getX(),
                            getY(),
                            getZ(),
                            3,
                            0.1D,
                            0.1D,
                            0.1D,
                            0.05D
                    );
                }
            }

            return;
        }

        /*
         * =====================================================
         * 普通方块命中
         * =====================================================
         */
        if (!level().isClientSide) {

            if (level() instanceof ServerLevel server) {

                server.sendParticles(
                        ParticleTypes.SMOKE,
                        getX(),
                        getY(),
                        getZ(),
                        4,
                        0.05D,
                        0.05D,
                        0.05D,
                        0.01D
                );
            }

            level().playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.ARROW_HIT,
                    SoundSource.PLAYERS,
                    0.4F,
                    1.8F
            );

            discard();
        }
    }

    /**
     * 根据距离计算伤害。
     */
    private float damageAt() {

        if (startPos == null) {
            return damage;
        }

        double dist =
                position().distanceTo(
                        startPos
                );

        if (dist <= effectiveRange) {
            return damage;
        }

        float over =
                (float) (
                        dist - effectiveRange
                );

        float ratio =
                Math.max(
                        MIN_DAMAGE_RATIO,
                        1.0F
                                - over
                                / effectiveRange
                );

        return damage * ratio;
    }

    /**
     * 保存 NBT。
     */
    @Override
    public void addAdditionalSaveData(
            CompoundTag tag
    ) {

        super.addAdditionalSaveData(tag);

        tag.putFloat(
                "BulletDamage",
                damage
        );

        tag.putInt(
                "BulletPenetration",
                penetration
        );

        tag.putInt(
                "BulletRicochet",
                getBouncesLeft()
        );

        tag.putFloat(
                "BulletKnockback",
                knockback
        );

        tag.putFloat(
                "BulletRange",
                effectiveRange
        );

        tag.putBoolean(
                "BulletTracking",
                tracking
        );

        tag.putDouble(
                "BulletTrackingSpeed",
                trackingSpeed
        );

        /*
         * 保存追踪目标。
         */
        targetTracker.save(tag);

        /*
         * 保存枪械。
         */
        if (sourceGun != null) {

            ResourceLocation gunId =
                    BuiltInRegistries.ITEM.getKey(
                            sourceGun
                    );

            if (gunId != null) {

                tag.putString(
                        "BulletGun",
                        gunId.toString()
                );
            }
        }

        /*
         * 保存弹药。
         */
        if (ammoItem != null) {

            ResourceLocation ammoId =
                    BuiltInRegistries.ITEM.getKey(
                            ammoItem
                    );

            if (ammoId != null) {

                tag.putString(
                        "BulletAmmo",
                        ammoId.toString()
                );
            }
        }

        /*
         * 保存特殊弹药数据。
         */
        if (!ammoData.isEmpty()) {

            tag.put(
                    "BulletAmmoData",
                    ammoData.copy()
            );
        }

        /*
         * 保存初始位置。
         */
        if (startPos != null) {

            tag.putDouble(
                    "BulletStartX",
                    startPos.x
            );

            tag.putDouble(
                    "BulletStartY",
                    startPos.y
            );

            tag.putDouble(
                    "BulletStartZ",
                    startPos.z
            );
        }

        /*
         * 保存穿透记录。
         */
        pierced.save(tag);
    }

    /**
     * 读取 NBT。
     */
    @Override
    public void readAdditionalSaveData(
            CompoundTag tag
    ) {

        super.readAdditionalSaveData(tag);

        this.damage =
                tag.getFloat(
                        "BulletDamage"
                );

        this.penetration =
                tag.getInt(
                        "BulletPenetration"
                );

        this.entityData.set(
                DATA_RICOCHET,
                tag.getInt(
                        "BulletRicochet"
                )
        );

        this.knockback =
                tag.getFloat(
                        "BulletKnockback"
                );

        this.effectiveRange =
                tag.getFloat(
                        "BulletRange"
                );

        this.tracking =
                tag.getBoolean(
                        "BulletTracking"
                );

        this.trackingSpeed =
                tag.contains(
                        "BulletTrackingSpeed"
                )
                        ? tag.getDouble(
                        "BulletTrackingSpeed"
                )
                        : 0.0D;

        /*
         * 恢复目标。
         */
        targetTracker.load(tag);

        /*
         * 恢复枪械。
         */
        if (tag.contains("BulletGun")) {

            Item gunItem =
                    BuiltInRegistries.ITEM.get(
                            new ResourceLocation(
                                    tag.getString(
                                            "BulletGun"
                                    )
                            )
                    );

            this.sourceGun =
                    gunItem instanceof GunItem gun
                            ? gun
                            : null;
        }

        /*
         * 恢复弹药。
         */
        if (tag.contains("BulletAmmo")) {

            this.ammoItem =
                    BuiltInRegistries.ITEM.get(
                            new ResourceLocation(
                                    tag.getString(
                                            "BulletAmmo"
                                    )
                            )
                    );
        }

        /*
         * 恢复弹药数据。
         */
        if (tag.contains(
                "BulletAmmoData"
        )) {

            this.ammoData.merge(
                    tag.getCompound(
                            "BulletAmmoData"
                    )
            );
        }

        /*
         * 恢复初始位置。
         */
        if (tag.contains(
                "BulletStartX"
        )) {

            this.startPos =
                    new Vec3(
                            tag.getDouble(
                                    "BulletStartX"
                            ),
                            tag.getDouble(
                                    "BulletStartY"
                            ),
                            tag.getDouble(
                                    "BulletStartZ"
                            )
                    );
        }

        /*
         * 恢复穿透记录。
         */
        pierced.load(tag);
    }
}