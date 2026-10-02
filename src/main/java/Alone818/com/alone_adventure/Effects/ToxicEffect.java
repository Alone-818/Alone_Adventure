package Alone818.com.alone_adventure.Effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;


public class ToxicEffect extends MobEffect {


    public ToxicEffect() {

        super(
                MobEffectCategory.HARMFUL,
                0x55FF00
        );

    }



    /**
     * 毒伤
     * 每20tick触发
     */
    @Override
    public boolean isDurationEffectTick(
            int duration,
            int amplifier
    ){

        return duration % 20 == 0;
    }



    @Override
    public void applyEffectTick(
            LivingEntity entity,
            int amplifier
    ){

        if(entity.level().isClientSide())
            return;


        /*
         * 等级：
         * amplifier 0 = I级
         *
         * 伤害:
         * 等级*0.5+1
         */

        float damage =
                (amplifier + 1) * 0.5F + 1F;



        entity.hurt(
                entity.damageSources().magic(),
                damage
        );

    }


}