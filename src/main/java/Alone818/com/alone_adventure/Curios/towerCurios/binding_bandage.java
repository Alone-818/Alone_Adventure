package Alone818.com.alone_adventure.Curios.towerCurios;

import Alone818.com.alone_adventure.events.towerEvent.BandageEvent;
import Alone818.com.alone_adventure.init.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;


/**
 * 紧缚绷带
 *
 * 被动：
 * 1. 攻击命中概率获得耐力
 * 2. 攻击虚弱目标额外伤害
 * 3. 攻击概率叠加猛毒
 *
 * 主动：
 * 生成护盾
 *
 */
public class binding_bandage extends Item implements ICurioItem {


    //==============================
    // 被动：耐力
    //==============================


    /**
     * 攻击触发耐力概率
     */
    public static float ENDURANCE_CHANCE = 0.50F;


    /**
     * 耐力持续时间
     */
    public static int ENDURANCE_DURATION_TICKS = 60;




    //==============================
    // 被动：虚弱增伤
    //==============================


    /**
     * 虚弱等级 × 额外伤害
     */
    public static float WEAKNESS_BONUS_PER_LEVEL = 2.0F;




    //==============================
    // 被动：猛毒
    //==============================


    /**
     * 攻击触发猛毒概率
     */
    public static float TOXIC_CHANCE = 0.20F;


    /**
     * 每次触发增加猛毒等级
     *
     * 默认：
     * I → III → V → VII → VIII
     */
    public static int TOXIC_STACK_ADD = 2;


    /**
     * 最大猛毒等级
     *
     * 默认 VIII级
     */
    public static int TOXIC_MAX_LEVEL = 8;


    /**
     * 猛毒持续时间
     *
     * 默认30秒
     */
    public static int TOXIC_DURATION = 20 * 30;




    //==============================
    // 主动技能：护盾
    //==============================


    /**
     * 获得护盾数量
     */
    public static int SHIELD_GAIN = 1;


    /**
     * 护盾最大数量
     */
    public static int SHIELD_MAX = 3;


    /**
     * 技能冷却
     *
     * 45秒
     */
    public static int SKILL_COOLDOWN_TICKS = 900;




    //==============================
    // 主动技能：猛毒爆发
    //==============================


    /**
     * 猛毒技能范围
     */
    public static double TOXIC_SKILL_RANGE = 8;


    /**
     * 技能施加猛毒时间
     *
     * 默认3秒
     */
    public static int TOXIC_SKILL_DURATION = 20 * 3;





    //==============================
    // NBT
    //==============================


    public static final String NB_TAG_SHIELD =
            "BandageShield";


    public static final String TAG_SKILL_READY_AT =
            "BandageSkillReadyAt";





    public binding_bandage() {

        super(
                new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON)
        );
    }





    /**
     * 获取当前护盾
     */
    public static double getShield(ItemStack stack) {

        CompoundTag tag =
                stack.getTag();

        return tag == null
                ? 0
                : tag.getDouble(NB_TAG_SHIELD);
    }







    /**
     * 主动技能：
     *
     * 获得护盾
     */
    public static boolean activateSkill(ServerPlayer player) {


        Optional<SlotResult> curioOpt =
                CuriosApi.getCuriosHelper()
                        .findFirstCurio(
                                player,
                                ModItems.BINDING_BANDAGE.get()
                        );


        if(curioOpt.isEmpty())
            return false;



        SlotResult slotResult =
                curioOpt.get();



        ItemStack stack =
                slotResult.stack();



        CompoundTag tag =
                stack.getOrCreateTag();



        long now =
                player.level()
                        .getGameTime();



        if(tag.contains(TAG_SKILL_READY_AT)
                &&
                now < tag.getLong(TAG_SKILL_READY_AT)){

            return false;
        }





        double shield =
                tag.getDouble(NB_TAG_SHIELD);



        if(shield >= SHIELD_MAX)
            return false;




        tag.putDouble(
                NB_TAG_SHIELD,
                Math.min(
                        SHIELD_MAX,
                        shield + SHIELD_GAIN
                )
        );



        tag.putLong(
                TAG_SKILL_READY_AT,
                now + SKILL_COOLDOWN_TICKS
        );



        stack.setTag(tag);




        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->

                        handler.setEquippedCurio(

                                slotResult.slotContext()
                                        .identifier(),

                                slotResult.slotContext()
                                        .index(),

                                stack
                        )
                );




        player.level()
                .playSound(
                        null,
                        player,
                        SoundEvents.ARMOR_EQUIP_CHAIN,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.2F
                );


        return true;
    }







    /**
     * 剩余冷却
     */
    public static long getSkillCooldownRemaining(
            ItemStack stack,
            Level level
    ){

        CompoundTag tag =
                stack.getTag();


        if(tag == null
                ||
                !tag.contains(TAG_SKILL_READY_AT)
                ||
                level == null){

            return 0;
        }



        return Math.max(
                0,
                tag.getLong(TAG_SKILL_READY_AT)
                        -
                        level.getGameTime()
        );
    }







    @Override
    public void onUnequip(
            SlotContext slotContext,
            ItemStack newStack,
            ItemStack stack
    ){

        //保留护盾NBT

    }









    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ){


        tooltip.add(
                Component.translatable(
                                "item.alone_adventure.binding_bandage.tooltip.desc"
                        )
                        .withStyle(ChatFormatting.WHITE)
        );



        if(Screen.hasShiftDown()){


            tooltip.add(
                    Component.translatable(
                                    "item.alone_adventure.binding_bandage.tooltip.passive_desc"
                            )
                            .withStyle(ChatFormatting.DARK_GREEN)
            );


            tooltip.add(
                    Component.translatable(
                                    "item.alone_adventure.binding_bandage.tooltip.weakness_desc"
                            )
                            .withStyle(ChatFormatting.RED)
            );
            tooltip.add(
                    Component.translatable(
                                    "item.alone_adventure.binding_bandage.tooltip.toxic_desc"
                            )
                            .withStyle(ChatFormatting.GREEN)
            );

            tooltip.add(
                    Component.translatable(
                                    "item.alone_adventure.binding_bandage.tooltip.active_desc"
                            )
                            .withStyle(ChatFormatting.AQUA)
            );
            tooltip.add(
                    Component.translatable(
                                    "item.alone_adventure.binding_bandage.tooltip.toxic_skill_desc"
                            )
                            .withStyle(ChatFormatting.DARK_GREEN)
            );



            double shield =
                    getShield(stack);



            if(shield > 0){

                tooltip.add(
                        Component.translatable(
                                        "item.alone_adventure.binding_bandage.tooltip.shield",
                                        (int)shield,
                                        SHIELD_MAX
                                )
                                .withStyle(ChatFormatting.BLUE)
                );
            }



            long remain =
                    getSkillCooldownRemaining(
                            stack,
                            level
                    );



            if(remain > 0){

                tooltip.add(
                        Component.translatable(
                                        "item.alone_adventure.binding_bandage.tooltip.cooldown_left",
                                        Math.round(remain / 20.0F)
                                )
                                .withStyle(ChatFormatting.GRAY)
                );
            }


        }else{


            tooltip.add(
                    Component.translatable(
                                    "tooltip.alone_adventure.press_shift"
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY,
                                    ChatFormatting.ITALIC
                            )
            );
        }
    }

}