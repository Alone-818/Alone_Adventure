package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import Alone818.com.alone_adventure.Items.gun.GunItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class short_bullet_poison extends AmmoItem {

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
     * 命中施加：
     *
     * 中毒 III
     * 10 秒
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

        living.addEffect(
                new MobEffectInstance(
                        MobEffects.POISON,
                        200,
                        2,
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
}