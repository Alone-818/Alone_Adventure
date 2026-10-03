package Alone818.com.alone_adventure.Items.armor.type;


import Alone818.com.alone_adventure.Items.armor.ArmorType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;


public class KnightArmorType
        implements ArmorType {



    @Override
    public void tick(
            Player player
    ){


        if(hasFullArmor(player)){


            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE,
                            40,
                            0
                    )
            );


        }


    }



    private boolean hasFullArmor(
            Player player
    ){

        return
                !player.getInventory()
                        .armor
                        .get(0)
                        .isEmpty()
                        &&
                        !player.getInventory()
                                .armor
                                .get(1)
                                .isEmpty()
                        &&
                        !player.getInventory()
                                .armor
                                .get(2)
                                .isEmpty()
                        &&
                        !player.getInventory()
                                .armor
                                .get(3)
                                .isEmpty();

    }

}