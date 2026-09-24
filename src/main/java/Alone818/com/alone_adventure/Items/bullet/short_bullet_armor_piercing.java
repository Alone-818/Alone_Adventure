package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class short_bullet_armor_piercing extends AmmoItem {

    /**
     * 根据目标护甲值计算额外伤害：
     *
     * 额外伤害 = 护甲值 × 0.5
     */
    private static final float ARMOR_DAMAGE_RATIO = 0.5F;

    public short_bullet_armor_piercing() {
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

        int armor =
                living.getArmorValue();

        if (armor <= 0) {
            return;
        }

        float bonusDamage =
                armor * ARMOR_DAMAGE_RATIO;

        /*
         * 清掉上一段基础子弹伤害造成的无敌帧，
         * 保证额外穿甲伤害能够立即结算。
         */
        living.invulnerableTime = 0;

        /*
         * 魔法伤害用于让这部分“护甲值 × 0.5”
         * 额外伤害绕过普通护甲减伤。
         *
         * 原始子弹伤害仍然按照正常伤害流程计算。
         */
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
        return 2;
    }
}