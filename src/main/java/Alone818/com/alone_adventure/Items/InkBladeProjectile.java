package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 墨制刀刃的投掷弹体。
 *
 * 命中对目标造成 {@value #DAMAGE} 点伤害并叠加虚弱（每击 +1 级，上限
 * {@value #WEAKNESS_MAX_LEVEL} 级，持续 {@value #WEAKNESS_DURATION_TICKS} tick，命中刷新），
 * 随后自行消失（弹体不掉落、不可拾取——耐久由 {@link ink_blade} 在掷出时统一扣除）。
 * 不伤害驯兽的机制与 {@link painstrike_hammer} 的易伤一致。
 */
public class InkBladeProjectile extends ThrowableItemProjectile {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 命中伤害 */
    public static float DAMAGE = 3.0F;
    /** 虚弱叠加持续时间（5 秒），每次命中刷新 */
    public static int WEAKNESS_DURATION_TICKS = 100;
    /** 虚弱叠加上限：5 级（amplifier 最高 4） */
    public static int WEAKNESS_MAX_LEVEL = 5;
    /** 飞行下坠重力：原版投掷物为 0.03，减半使弹道更平直 */
    private static final float GRAVITY = 0.01F;

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    public InkBladeProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public InkBladeProjectile(EntityType<? extends ThrowableItemProjectile> type,
                              LivingEntity thrower, Level level, ItemStack blade) {
        super(type, thrower, level);
        setItem(blade);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INK_BLADE.get();
    }

    /**
     * 命中任意目标（实体或方块）后化墨消散。
     * 音效用原版"溅水"声模拟刀刃钉入的反馈。
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().playSound(null, this,
                    net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 1.4F);
            discard();
        } else {
            for (int i = 0; i < 6; ++i) {
                level().addParticle(ParticleTypes.SPLASH,
                        this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;

        Entity hit = result.getEntity();
        hit.hurt(damageSources().thrown(this, getOwner()), DAMAGE);

        if (hit instanceof LivingEntity living && !(living instanceof TamableAnimal)) {
            addWeakness(living);
        }
    }

    /**
     * 叠加虚弱：新等级 = 原有等级 + 1（上限 {@value #WEAKNESS_MAX_LEVEL} 级），并刷新持续时间。
     * amplifier = 等级 - 1，对 amplifier 直接相加即等价于等级相加（与链锯剑叠撕裂同款）。
     */
    public static void addWeakness(LivingEntity target) {
        MobEffectInstance existing = target.getEffect(MobEffects.WEAKNESS);
        int newAmplifier = existing == null ? 0
                : Math.min(WEAKNESS_MAX_LEVEL - 1, existing.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, newAmplifier));
    }
}
