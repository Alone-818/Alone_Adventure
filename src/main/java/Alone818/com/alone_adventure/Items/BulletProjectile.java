package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.init.ModEntities;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.util.Penetration;
import Alone818.com.alone_adventure.util.Ricochet;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 通用枪械的子弹实体。
 *
 * 开火时由 {@code GunItem.tryFire} 携带枪械属性 + 玩家加成生成：
 * <ul>
 *   <li>伤害（单发）</li>
 *   <li>穿透能力 —— 命中实体后不消失，继续穿透最多 N 个实体（N=0 时命中即停）；
 *       命中记录用 {@link Penetration.Record}（原始 int 数组，每实体 4 字节，
 *       较 {@code Set<UUID>} 省一个数量级内存，霰弹齐射时差距明显）</li>
 *   <li>反弹能力 —— 命中方块后按命中面镜面反射继续飞行，最多 M 次
 *       （M=0 时命中即碎）；反射运算经 {@link Ricochet#applyBounce}，
 *       每次反弹速度按 {@value #RICOCHET_ENERGY} 比例衰减</li>
 *   <li>击退能力 —— 命中后沿弹道方向击退目标</li>
 *   <li>有效射程 —— 有效射程内全额伤害，超出后线性衰减，最低保留 25%</li>
 * </ul>
 * 子弹直线飞行（无重力），5 秒兜底寿命；曳光由客户端每 tick 的粒子模拟。
 * 命中同一实体的多发子弹（霰弹）共用 {@code invulnerableTime} 归零机制（与链锯剑扫射一致），
 * 保证每颗弹丸都真正结算伤害。
 */
public class BulletProjectile extends ThrowableItemProjectile {

    // 逐发弹体属性（开火时写入；不参与 Config——由枪械属性与玩家加成决定）
    /** 单发伤害 */
    private float damage = 1.0F;
    /** 穿透能力：可穿透的实体数 */
    private int penetration = 0;
    /** 击退能力 */
    private float knockback = 0.5F;
    /** 有效射程（格） */
    private float effectiveRange = 30.0F;
    /** 出膛位置（射程衰减的距离基准） */
    private Vec3 startPos = null;
    /** 发射本弹的枪械（击中钩子回调用；跨存档按注册名恢复） */
    @Nullable
    private GunItem sourceGun = null;
    /** 本弹的弹药种类（激发/命中钩子回调用；跨存档按注册名恢复） */
    @Nullable
    private Item ammoItem = null;
    /**
     * 特种弹药数据（弹体 NBT 标签）：激发时由 {@link AmmoItem#onFired} 写入
     * （爆炸标记、元素附加、计时器等），命中时由 {@link AmmoItem#onBulletHit} 读取结算；
     * 随弹体存档。弹体实体类型不随弹药变化，弹药差异全部经此标签携带。
     */
    private final CompoundTag ammoData = new CompoundTag();

    /**
     * 已穿透命中过的实体（不重复判定）。
     * {@link Penetration.Record}：原始 int 数组记实体 ID，每实体 4 字节，
     * 无 UUID 装箱与哈希表项开销；穿透目标天然个位数，线性查找也最快。
     */
    private final Penetration.Record pierced = new Penetration.Record();

    /**
     * 剩余反弹次数（同步数据）：反弹是纯确定性向量运算，客户端拿到初值后
     * 本地同样模拟（双端一致，无持续同步流量），服务端额外结算音效/粒子。
     */
    private static final EntityDataAccessor<Integer> DATA_RICOCHET =
            SynchedEntityData.defineId(BulletProjectile.class, EntityDataSerializers.INT);
    /** 反弹能量保留系数：每次反弹后速度按此比例衰减（0.7 = 保留 70%） */
    private static final float RICOCHET_ENERGY = 0.7F;

    /** 兜底寿命：5 秒 */
    private static final int MAX_LIFE_TICKS = 100;
    /** 超出有效射程后的最低伤害保留比例 */
    private static final float MIN_DAMAGE_RATIO = 0.25F;

    public BulletProjectile(EntityType<? extends BulletProjectile> type, Level level) {
        super(type, level);
    }

    public BulletProjectile(EntityType<? extends BulletProjectile> type,
                            LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RICOCHET, 0);
    }

    /** 剩余反弹次数（双端一致；开火时随 spawn 数据下发） */
    public int getBouncesLeft() {
        return this.entityData.get(DATA_RICOCHET);
    }

    /** 开火时写入弹体属性、来源枪械、弹药种类并记录出膛位置（仅服务端调用） */
    public void configure(GunItem sourceGun, Item ammoItem, float damage, int penetration,
                          int ricochet, float knockback, float effectiveRange) {
        this.sourceGun = sourceGun;
        this.ammoItem = ammoItem;
        this.damage = damage;
        this.penetration = penetration;
        this.knockback = knockback;
        this.effectiveRange = effectiveRange;
        this.startPos = position();
        // 反弹次数走同步数据：configure 在 spawn 前调用，初值随实体数据一次性下发
        this.entityData.set(DATA_RICOCHET, Math.max(0, ricochet));
    }

    /** 特种弹药数据（激发时写入，命中时读取；随弹体存档） */
    public CompoundTag getAmmoData() {
        return ammoData;
    }

    /** 本弹的弹药种类（无记录时为 null） */
    @Nullable
    public Item getAmmoItem() {
        return ammoItem;
    }

    @Override
    protected Item getDefaultItem() {
        // 旧档兜底：正常情况下开火时已 setItem 为对应弹药
        return ModItems.SHORT_BULLET.get();
    }

    /** 子弹直线飞行：无重力 */
    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        // 客户端曳光轨迹
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CRIT,
                    getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.tickCount > MAX_LIFE_TICKS) {
            discard();
        }
    }

    /** 已被穿透过的实体不再重复判定（实体 ID 线性查 int 数组，零分配） */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !pierced.hasHit(target.getId());
    }

    /** 命中实体：结算伤害、击退与穿透 */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;

        Entity hit = result.getEntity();
        // 击中钩子：回调来源枪械与弹药（伤害结算前，damage 为经射程衰减后的值）
        if (level() instanceof ServerLevel serverLevel) {
            if (sourceGun != null) {
                sourceGun.onBulletHitEntity(serverLevel, this, hit, damageAt());
            }
            if (ammoItem instanceof AmmoItem ammo) {
                ammo.onBulletHit(serverLevel, this, hit, damageAt());
            }
        }
        // 归零无敌帧：保证霰弹的多颗弹丸与连续射击每发都结算（与链锯剑扫射一致）
        if (hit instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        hit.hurt(damageSources().thrown(this, getOwner()), damageAt());

        // 击退：沿弹道方向推动目标（对玩家需额外发送速度包）
        if (knockback > 0) {
            Vec3 dir = getDeltaMovement().normalize();
            hit.push(dir.x * knockback, 0.1D * knockback + 0.05D, dir.z * knockback);
            if (hit instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
        }

        // 命中反馈
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT,
                    hit.getX(), hit.getY() + hit.getBbHeight() * 0.5D, hit.getZ(),
                    5, 0.15D, 0.2D, 0.15D, 0.08D);
        }
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.3F, 1.6F);

        // 穿透结算：已命中实体数超过穿透能力则停止
        pierced.record(hit.getId());
        if (pierced.exhausted(penetration)) {
            discard();
        }
    }

    /**
     * 命中方块：仍有反弹余量则按命中面镜面反射继续飞行（{@link Ricochet#applyBounce}，
     * 入射角 = 反射角 + 位置回退到命中面外），耗尽后弹着点火花并消失。
     * 反弹段<b>双端执行</b>：纯确定性向量运算，客户端本地模拟同一轨迹，
     * 数值经 {@link #DATA_RICOCHET} 随 spawn 数据一次性下发，无持续同步开销。
     */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (getBouncesLeft() > 0 && Ricochet.applyBounce(this, result, RICOCHET_ENERGY)) {
            this.entityData.set(DATA_RICOCHET, getBouncesLeft() - 1);
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY(), getZ(),
                        SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.3F, 2.2F);
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT,
                            getX(), getY(), getZ(), 3, 0.1D, 0.1D, 0.1D, 0.05D);
                }
            }
            return;
        }
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE,
                        getX(), getY(), getZ(), 4, 0.05D, 0.05D, 0.05D, 0.01D);
            }
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.4F, 1.8F);
            discard();
        }
    }

    /** 按飞行距离结算伤害：射程内全额，超出后线性衰减，最低 25% */
    private float damageAt() {
        if (startPos == null) return damage;
        double dist = position().distanceTo(startPos);
        if (dist <= effectiveRange) return damage;
        float over = (float) (dist - effectiveRange);
        float ratio = Math.max(MIN_DAMAGE_RATIO, 1.0F - over / effectiveRange);
        return damage * ratio;
    }

    // ===== 存档 =====

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BulletDamage", damage);
        tag.putInt("BulletPenetration", penetration);
        tag.putInt("BulletRicochet", getBouncesLeft());
        tag.putFloat("BulletKnockback", knockback);
        tag.putFloat("BulletRange", effectiveRange);
        if (sourceGun != null) {
            ResourceLocation gunId = BuiltInRegistries.ITEM.getKey(sourceGun);
            tag.putString("BulletGun", gunId.toString());
        }
        if (ammoItem != null) {
            ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammoItem);
            tag.putString("BulletAmmo", ammoId.toString());
        }
        if (!ammoData.isEmpty()) {
            tag.put("BulletAmmoData", ammoData);
        }
        if (startPos != null) {
            tag.putDouble("BulletStartX", startPos.x);
            tag.putDouble("BulletStartY", startPos.y);
            tag.putDouble("BulletStartZ", startPos.z);
        }
        pierced.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("BulletDamage");
        this.penetration = tag.getInt("BulletPenetration");
        this.entityData.set(DATA_RICOCHET, tag.getInt("BulletRicochet"));
        this.knockback = tag.getFloat("BulletKnockback");
        this.effectiveRange = tag.getFloat("BulletRange");
        if (tag.contains("BulletGun")) {
            Item gunItem = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("BulletGun")));
            this.sourceGun = gunItem instanceof GunItem gun ? gun : null;
        }
        if (tag.contains("BulletAmmo")) {
            this.ammoItem = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("BulletAmmo")));
        }
        if (tag.contains("BulletAmmoData")) {
            this.ammoData.merge(tag.getCompound("BulletAmmoData"));
        }
        if (tag.contains("BulletStartX")) {
            this.startPos = new Vec3(
                    tag.getDouble("BulletStartX"),
                    tag.getDouble("BulletStartY"),
                    tag.getDouble("BulletStartZ"));
        }
        pierced.load(tag);
    }
}
