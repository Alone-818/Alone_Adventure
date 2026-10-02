package Alone818.com.alone_adventure.events.towerEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.towerCurios.binding_bandage;
import Alone818.com.alone_adventure.events.miscEvent.ShieldEvent;
import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;


/**
 * 紧缚绷带 - 效果处理
 *
 * 1. 攻击命中：
 *    50%概率获得耐力 I
 *
 * 2. 攻击命中：
 *    20%概率施加猛毒
 *    每次增加2级
 *    最大8级
 *
 * 3. 攻击虚弱目标：
 *    额外造成虚弱等级×2伤害
 *
 * 4. 护盾：
 *    抵挡一次伤害
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class BandageEvent {


    //==============================
    // 猛毒配置
    //==============================

    public static final float TOXIC_CHANCE = 0.20F;

    public static final int TOXIC_STACK_ADD = 2;

    public static final int TOXIC_MAX_LEVEL = 8;

    public static final int TOXIC_DURATION = 20 * 30;



    /**
     * 判断是否佩戴绷带
     */
    private static Optional<SlotResult> findBandage(Player player){

        return CuriosApi.getCuriosHelper()
                .findFirstCurio(
                        player,
                        ModItems.BINDING_BANDAGE.get()
                );
    }



    /**
     * 施加/叠加猛毒
     */
    private static void applyToxic(
            LivingEntity target,
            Player attacker
    ){

        if(attacker.getRandom().nextFloat()
                >= TOXIC_CHANCE)
            return;



        MobEffectInstance old =
                target.getEffect(
                        ModEffects.TOXIC.get()
                );



        int level = 0;



        if(old != null){

            level =
                    old.getAmplifier()
                            + 1;
        }



        int newLevel =
                level
                        + TOXIC_STACK_ADD;



        newLevel =
                Math.min(
                        newLevel,
                        TOXIC_MAX_LEVEL
                );



        target.addEffect(
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







    /**
     * 攻击触发效果
     */
    @SubscribeEvent
    public static void onLivingHurt(
            LivingHurtEvent event
    ){


        if(!(event.getSource()
                .getEntity()
                instanceof Player attacker))
            return;



        if(attacker.level().isClientSide())
            return;



        LivingEntity target =
                event.getEntity();



        if(target == attacker)
            return;



        if(findBandage(attacker).isEmpty())
            return;




        /*
         * 耐力
         */
        if(attacker.getRandom().nextFloat()
                < binding_bandage.ENDURANCE_CHANCE){



            if(!attacker.hasEffect(
                    ModEffects.ENDURANCE.get()
            )){


                attacker.addEffect(
                        new MobEffectInstance(
                                ModEffects.ENDURANCE.get(),

                                binding_bandage.ENDURANCE_DURATION_TICKS,

                                0,

                                false,
                                true
                        )
                );
            }
        }




        /*
         * 猛毒
         */
        applyToxic(
                target,
                attacker
        );

    }







    /**
     * 虚弱增伤
     */
    @SubscribeEvent
    public static void onAttackDamage(
            LivingDamageEvent event
    ){


        if(!(event.getSource()
                .getEntity()
                instanceof Player attacker))
            return;



        if(attacker.level().isClientSide())
            return;



        if(event.isCanceled())
            return;



        LivingEntity target =
                event.getEntity();



        if(findBandage(attacker).isEmpty())
            return;




        MobEffectInstance weakness =
                target.getEffect(
                        MobEffects.WEAKNESS
                );



        if(weakness == null)
            return;



        event.setAmount(
                event.getAmount()
                        +
                        (weakness.getAmplifier()+1)
                                *
                                binding_bandage.WEAKNESS_BONUS_PER_LEVEL
        );

    }







    /**
     * 护盾结算
     */
    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent event
    ){


        if(!(event.getEntity()
                instanceof Player player))
            return;



        if(event.isCanceled())
            return;



        if(event.getSource()
                .is(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return;



        Optional<SlotResult> optional =
                findBandage(player);



        if(optional.isEmpty())
            return;



        SlotResult slot =
                optional.get();



        ItemStack stack =
                slot.stack();



        CompoundTag tag =
                stack.getOrCreateTag();



        double shield =
                tag.getDouble(
                        binding_bandage.NB_TAG_SHIELD
                );



        if(shield <= 0)
            return;



        if(event.getAmount() <= 0)
            return;



        tag.putDouble(
                binding_bandage.NB_TAG_SHIELD,
                shield - 1
        );


        stack.setTag(tag);



        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->

                        handler.setEquippedCurio(
                                slot.slotContext()
                                        .identifier(),

                                slot.slotContext()
                                        .index(),

                                stack
                        )
                );



        player.addEffect(
                new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        40,
                        3,
                        false,
                        true,
                        true
                )
        );



        event.setCanceled(true);

    }

}