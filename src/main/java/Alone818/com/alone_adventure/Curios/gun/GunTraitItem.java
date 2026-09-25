package Alone818.com.alone_adventure.Curios.gun;


import Alone818.com.alone_adventure.Items.gun.IGunStatTooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;



public class GunTraitItem extends Item {


    public GunTraitItem(Properties properties){

        super(properties);

    }



    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ){


        if(this instanceof IGunStatTooltip provider){


            provider.addGunStatTooltip(
                    tooltip
            );


        }

    }


}