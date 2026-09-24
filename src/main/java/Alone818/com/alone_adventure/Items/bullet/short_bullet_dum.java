package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

public class short_bullet_dum extends AmmoItem {

    private static final int LACERATION_DURATION = 80; // 4 秒
    private static final int LACERATION_AMPLIFIER = 7; // 8 级

    private static final ResourceLocation LACERATION_ID =
            new ResourceLocation(
                    Alone_adventure.MODID,
                    "laceration"
            );

    public short_bullet_dum() {
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

        /*
         * 从注册表取得你模组的 Laceration。
         *
         * 8 级 = amplifier 7。
         */
        var laceration =
                ForgeRegistries.MOB_EFFECTS
                        .getValue(LACERATION_ID);

        if (laceration != null) {

            living.addEffect(
                    new MobEffectInstance(
                            laceration,
                            LACERATION_DURATION,
                            LACERATION_AMPLIFIER,
                            false,
                            true,
                            true
                    )
            );
        }
    }
    @Override
    public int getAmmoBoxCost() {
        return 2;
    }
}