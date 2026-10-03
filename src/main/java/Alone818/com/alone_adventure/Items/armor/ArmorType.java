package Alone818.com.alone_adventure.Items.armor;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public interface ArmorType {


    /*
     * 装备穿上的时候
     */
    default void onEquip(
            Player player,
            ItemStack stack
    ){

    }



    /*
     * 装备脱下的时候
     */
    default void onUnequip(
            Player player,
            ItemStack stack
    ){

    }



    /*
     * 每tick检测
     */
    default void tick(
            Player player
    ){

    }



    /*
     * 攻击触发
     */
    default void onAttack(
            Player player
    ){

    }



    /*
     * 受到伤害
     */
    default void onHurt(
            Player player
    ){

    }


}