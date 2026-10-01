package Alone818.com.alone_adventure.Items.hunterItems;

import Alone818.com.alone_adventure.Curios.miscCurios.hunter_serum;
import Alone818.com.alone_adventure.init.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class injection_template extends Item {

    public static final double DURATION_RATIO = 0.8;
    public static final double USE_RATIO = 0.5;

    public static final double MIX_ASH_DURATION_RATIO = 0.6;
    public static final double MIX_BLOOD_DURATION_BONUS = 0.6;

    public static final int MAX_EFFECTS = 5;
    public static final int MAX_AMPLIFIER = 2;
    public static final int MAX_DURATION_TICKS = 20 * 60 * 30;

    private static final String TAG_STORED_EFFECTS =
            "alone_adventure.stored_effects";


    public injection_template(Properties props) {
        super(props);
    }


    public static List<MobEffectInstance> getEffects(ItemStack potion) {

        return new ArrayList<>(
                PotionUtils.getMobEffects(potion)
        );
    }


    public static ItemStack createSyringe(
            List<MobEffectInstance> effects,
            int count
    ) {
        return createSyringe(
                effects,
                count,
                DURATION_RATIO
        );
    }


    public static ItemStack createSyringe(
            List<MobEffectInstance> effects,
            int count,
            double ratio
    ) {

        ItemStack stack =
                new ItemStack(
                        ModItems.INJECTION_SYRINGE.get(),
                        count
                );


        ListTag list = new ListTag();


        for(MobEffectInstance effect : effects){

            if(effect == null)
                continue;


            int duration =
                    Math.min(
                            (int)(effect.getDuration() * ratio),
                            MAX_DURATION_TICKS
                    );


            duration = Math.max(duration,1);


            int amplifier =
                    Math.min(
                            effect.getAmplifier(),
                            MAX_AMPLIFIER
                    );


            MobEffectInstance fixed =
                    new MobEffectInstance(
                            effect.getEffect(),
                            duration,
                            amplifier,
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon()
                    );


            CompoundTag tag = new CompoundTag();

            fixed.save(tag);


            if(tag.contains("Id"))
                list.add(tag);
        }


        stack.getOrCreateTag()
                .put(
                        TAG_STORED_EFFECTS,
                        list
                );


        return stack;
    }


    public static List<MobEffectInstance> mixEffects(
            ItemStack a,
            ItemStack b
    ){

        Map<MobEffect,MobEffectInstance> map =
                new LinkedHashMap<>();


        mergeInto(
                map,
                readEffects(a)
        );


        mergeInto(
                map,
                readEffects(b)
        );


        return new ArrayList<>(
                map.values()
        );
    }


    private static void mergeInto(
            Map<MobEffect,MobEffectInstance> map,
            List<MobEffectInstance> list
    ){

        for(MobEffectInstance e:list){

            MobEffectInstance old =
                    map.get(e.getEffect());


            if(old == null){

                map.put(
                        e.getEffect(),
                        copy(e)
                );

            }else{

                map.put(
                        e.getEffect(),

                        new MobEffectInstance(
                                e.getEffect(),
                                Math.max(
                                        old.getDuration(),
                                        e.getDuration()
                                ),
                                Math.max(
                                        old.getAmplifier(),
                                        e.getAmplifier()
                                ),
                                old.isAmbient(),
                                old.isVisible(),
                                old.showIcon()
                        )
                );
            }
        }
    }


    public static List<MobEffectInstance> applyCatalysts(
            List<MobEffectInstance> effects,
            int ash,
            int blood
    ){

        double ratio =
                Math.pow(
                        MIX_ASH_DURATION_RATIO,
                        ash
                )
                        *
                        (1 +
                                MIX_BLOOD_DURATION_BONUS * blood);


        List<MobEffectInstance> result =
                new ArrayList<>();


        for(MobEffectInstance e:effects){

            int amplifier =
                    Math.min(
                            e.getAmplifier()+ash,
                            MAX_AMPLIFIER
                    );


            int duration =
                    Math.min(
                            (int)(e.getDuration()*ratio),
                            MAX_DURATION_TICKS
                    );


            result.add(
                    new MobEffectInstance(
                            e.getEffect(),
                            Math.max(duration,1),
                            amplifier,
                            e.isAmbient(),
                            e.isVisible(),
                            e.showIcon()
                    )
            );
        }


        return result;
    }


    private static MobEffectInstance copy(
            MobEffectInstance e
    ){

        return new MobEffectInstance(
                e.getEffect(),
                e.getDuration(),
                e.getAmplifier(),
                e.isAmbient(),
                e.isVisible(),
                e.showIcon()
        );
    }


    public static List<MobEffectInstance> readEffects(
            ItemStack stack
    ){

        List<MobEffectInstance> result =
                new ArrayList<>();


        CompoundTag tag =
                stack.getTag();


        if(tag == null)
            return result;


        if(!tag.contains(
                TAG_STORED_EFFECTS,
                Tag.TAG_LIST
        ))
            return result;


        ListTag list =
                tag.getList(
                        TAG_STORED_EFFECTS,
                        Tag.TAG_COMPOUND
                );


        for(int i=0;i<list.size();i++){

            CompoundTag effectTag =
                    list.getCompound(i);


            if(!effectTag.contains("Id"))
                continue;


            MobEffectInstance effect =
                    MobEffectInstance.load(effectTag);


            if(effect!=null)
                result.add(
                        clamp(effect)
                );
        }


        return result;
    }


    private static MobEffectInstance clamp(
            MobEffectInstance e
    ){

        return new MobEffectInstance(
                e.getEffect(),
                Math.min(
                        e.getDuration(),
                        MAX_DURATION_TICKS
                ),
                Math.min(
                        e.getAmplifier(),
                        MAX_AMPLIFIER
                ),
                e.isAmbient(),
                e.isVisible(),
                e.showIcon()
        );
    }    @Override
    public int getUseDuration(ItemStack stack) {
        return (int)(32 * USE_RATIO);
    }


    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ){

        player.startUsingItem(hand);

        return InteractionResultHolder.consume(
                player.getItemInHand(hand)
        );
    }


    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ){

        if(!level.isClientSide){

            List<MobEffectInstance> effects =
                    readEffects(stack);


            boolean serum =
                    entity instanceof Player player
                            &&
                            hunter_serum.isWearing(player);



            for(MobEffectInstance effect : effects){

                if(effect == null)
                    continue;


                int amplifier =
                        effect.getAmplifier();


                int duration =
                        effect.getDuration();



                if(serum){

                    amplifier =
                            Math.min(
                                    amplifier +
                                            hunter_serum.POTION_LEVEL_BONUS,
                                    MAX_AMPLIFIER
                            );


                    MobEffectInstance old =
                            entity.getEffect(
                                    effect.getEffect()
                            );


                    if(old != null){

                        duration +=
                                old.getDuration();


                        amplifier =
                                Math.max(
                                        amplifier,
                                        old.getAmplifier()
                                );
                    }


                    duration =
                            Math.min(
                                    duration,
                                    MAX_DURATION_TICKS
                            );
                }



                entity.addEffect(
                        new MobEffectInstance(
                                effect.getEffect(),
                                duration,
                                amplifier,
                                effect.isAmbient(),
                                effect.isVisible(),
                                effect.showIcon()
                        )
                );
            }
        }


        stack.shrink(1);

        return stack;
    }



    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ){

        List<MobEffectInstance> effects =
                readEffects(stack);



        if(effects.isEmpty()){

            tooltip.add(
                    Component.translatable(
                                    "tooltip.alone_adventure.injection_template.empty"
                            )
                            .withStyle(
                                    ChatFormatting.GRAY
                            )
            );

            return;
        }



        tooltip.add(
                Component.translatable(
                                "tooltip.alone_adventure.injection_template.header"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );


        PotionUtils.addPotionTooltip(
                effects,
                tooltip,
                1.0F
        );
    }
}