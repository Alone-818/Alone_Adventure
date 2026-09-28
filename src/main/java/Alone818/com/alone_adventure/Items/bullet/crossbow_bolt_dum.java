package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.gun.BulletProjectile;
import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

import Alone818.com.alone_adventure.Alone_adventure;

public class crossbow_bolt_dum extends AmmoItem {

    private static final int LACERATION_DURATION = 20; // 1 秒
    private static final int LACERATION_AMPLIFIER = 14; // 15 级

    private static final ResourceLocation LACERATION_ID =
            new ResourceLocation(
                    Alone_adventure.MODID,
                    "laceration"
            );

    public crossbow_bolt_dum() {
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

    @Override
    public int getAmmoBoxAmount() {
        return 1;
    }
}
