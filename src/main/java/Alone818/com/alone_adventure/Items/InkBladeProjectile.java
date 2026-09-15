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
 * 命中对目标造成 {@value #DAMAGE} 点伤害并施加虚弱 I（{@value #WEAKNESS_DURATION_TICKS} tick），
 * 随后自行消失（弹体不掉落、不可拾取——耐久由 {@link ink_blade} 在掷出时统一扣除）。
 * 不伤害驯兽的机制与 {@link painstrike_hammer} 的易伤一致。
 */
public class InkBladeProjectile extends ThrowableItemProjectile {

    /** 命中伤害 */
    public static final float DAMAGE = 2.0F;
    /** 虚弱持续时间（5 秒），等级固定 I（amplifier 0） */
    public static final int WEAKNESS_DURATION_TICKS = 100;
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
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, 0));
        }
    }
}
