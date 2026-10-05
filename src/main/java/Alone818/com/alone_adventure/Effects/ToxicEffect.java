package Alone818.com.alone_adventure.Effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;


public class ToxicEffect extends MobEffect {


    public ToxicEffect() {

        super(
                MobEffectCategory.HARMFUL,
                0x55FF00
        );

        /*
         * 护甲削弱：每级 -2。
         *
         * 数值随效果等级缩放：
         * III 级（amplifier 2）= -6
         *
         * 护甲属性下限为 0，
         * 不会削成负数。
         */
        this.addAttributeModifier(
                Attributes.ARMOR,
                "a79b386e-e83d-48e2-991b-9ec363eab3f8",
                -2.0D,
                AttributeModifier.Operation.ADDITION
        );

        /*
         * 护甲韧性削弱：每级 -1。
         *
         * III 级 = -3
         */
        this.addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                "1a1007e4-2178-4bad-a874-b66c03ba497f",
                -1.0D,
                AttributeModifier.Operation.ADDITION
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