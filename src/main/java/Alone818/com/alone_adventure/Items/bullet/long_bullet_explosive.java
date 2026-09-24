package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.BulletProjectile;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class long_bullet_explosive extends AmmoItem {

    private static final String EXPLOSION_TRIGGERED =
            "ExplosionTriggered";

    private static final float EXPLOSION_POWER = 1.2F;

    public long_bullet_explosive() {
        super(new Properties());
    }

    @Override
    public void onFired(
            ServerPlayer shooter,
            ItemStack gun,
            BulletProjectile bullet
    ) {
        bullet.getAmmoData().putBoolean("Explosive", true);
    }

    @Override
    public void onBulletHit(
            ServerLevel level,
            BulletProjectile bullet,
            Entity hit,
            float damage
    ) {
        explodeOnce(level, bullet);
    }

    @Override
    public boolean onBulletHitBlock(
            ServerLevel level,
            BulletProjectile bullet,
            BlockHitResult result
    ) {
        explodeOnce(level, bullet);
        return true;
    }

    private void explodeOnce(
            ServerLevel level,
            BulletProjectile bullet
    ) {
        if (bullet.getAmmoData().getBoolean(EXPLOSION_TRIGGERED)) {
            return;
        }

        bullet.getAmmoData().putBoolean(EXPLOSION_TRIGGERED, true);

        Vec3 pos = bullet.position();

        level.explode(
                null,
                pos.x,
                pos.y,
                pos.z,
                EXPLOSION_POWER,
                Level.ExplosionInteraction.MOB
        );

        bullet.discard();
    }

    @Override
    public int getAmmoBoxCost() {
        return 3;
    }
}
