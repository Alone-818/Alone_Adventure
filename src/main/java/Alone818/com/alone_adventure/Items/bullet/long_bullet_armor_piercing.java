package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class long_bullet_armor_piercing extends AmmoItem {

    private static final float ARMOR_DAMAGE_RATIO = 0.5F;

    public long_bullet_armor_piercing() {
        super(new Properties());
    }

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

        int armor = living.getArmorValue();

        if (armor <= 0) {
            return;
        }

        float bonusDamage = armor * ARMOR_DAMAGE_RATIO;

        living.invulnerableTime = 0;

        living.hurt(
                level.damageSources().indirectMagic(
                        bullet,
                        bullet.getOwner()
                ),
                bonusDamage
        );
    }

    @Override
    public int getAmmoBoxCost() {
        return 3;
    }
}