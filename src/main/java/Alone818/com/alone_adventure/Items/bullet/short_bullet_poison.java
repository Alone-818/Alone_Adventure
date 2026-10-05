package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.gun.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.init.ModEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class short_bullet_poison extends AmmoItem {

    /** 猛毒初始等级：III */
    public static final int TOXIC_BASE_LEVEL = 3;

    /** 每次命中在当前等级上叠加：+2 级 */
    public static final int TOXIC_STACK_ADD = 2;

    /** 猛毒等级上限：VIII（与绷带一致） */
    public static final int TOXIC_MAX_LEVEL = 8;

    /** 猛毒持续时间：10 秒，每次命中刷新 */
    public static final int TOXIC_DURATION = 200;

    public short_bullet_poison() {
        super(new Properties());
    }

    /**
     * 伤害降低 70%：
     *
     * 最终伤害 = 原伤害 × 0.3
     */
    @Override
    public float getDamageMultiplier(
            GunItem gun,
            ItemStack gunStack
    ) {
        return 0.3F;
    }

    /**
     * 命中施加猛毒，可以叠加：
     *
     * 首次命中：猛毒 III
     * 后续命中：每次 +2 级
     * 上限 VIII 级（与绷带共用）
     * 每次命中刷新 10 秒持续时间
     *
     * 若目标已有绷带施加的猛毒，
     * 在其当前等级上继续叠加。
     */
    @Override
    public void onBulletHit(
            ServerLevel level,
            BulletProjectile bullet,
            Entity hit,
            float damage
    ) {

        if (!(hit instanceof LivingEntity living)) {
            return;
        }

        MobEffectInstance old =
                living.getEffect(
                        ModEffects.TOXIC.get()
                );

        // 当前等级数（I 级 = 1），没有猛毒时为 0
        int currentLevel =
                old != null
                        ? old.getAmplifier() + 1
                        : 0;

        /*
         * 首次命中保底 III 级，
         * 后续命中每次 +2 级，
         * 上限 VIII 级。
         */
        int newLevel =
                Math.min(
                        Math.max(
                                TOXIC_BASE_LEVEL,
                                currentLevel + TOXIC_STACK_ADD
                        ),
                        TOXIC_MAX_LEVEL
                );

        living.addEffect(
                new MobEffectInstance(
                        ModEffects.TOXIC.get(),
                        TOXIC_DURATION,
                        newLevel - 1,
                        false,
                        true,
                        true
                )
        );
    }
    @Override
    public int getAmmoBoxCost() {
        return 2;
    }
    @Override
    public int getAmmoBoxAmount() {
        return 1;
    }
}