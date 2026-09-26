package Alone818.com.alone_adventure.Curios.gun;


import Alone818.com.alone_adventure.Items.gun.GunModItems;
import Alone818.com.alone_adventure.Items.gun.IGunStatTooltip;
import Alone818.com.alone_adventure.Items.gun.PlayerGunStats;
import Alone818.com.alone_adventure.Items.gun.GunStatTooltipBuilder;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;


import net.minecraft.world.entity.player.Player;


import top.theillusivec4.curios.api.CuriosApi;


import java.util.List;



public class SingleActionRapidFire
        extends GunTraitItem
        implements IGunStatTooltip {



    public SingleActionRapidFire(){


        super(
                new Properties()
                        .stacksTo(1)
        );



        GunModItems.registerProvider(
                "single_action_rapid_fire",
                this::getStats
        );


    }




    private PlayerGunStats getStats(Player player){


        PlayerGunStats stats =
                new PlayerGunStats();



        if(!isWearing(player)){


            return stats;


        }




        // 装填速度 +30%

        stats.reloadSpeedMultiplier = 1.2F;



        // 开火速度 +30%

        stats.fireRateMultiplier = 1.8F;



        // 扩散 +20%

        stats.spreadMultiplier = 1.5F;



        return stats;


    }





    private boolean isWearing(Player player){


        return CuriosApi
                .getCuriosInventory(player)
                .resolve()
                .map(handler ->

                        handler.findFirstCurio(
                                stack ->
                                        stack.getItem()
                                                instanceof SingleActionRapidFire

                        ).isPresent()

                )
                .orElse(false);


    }




    @Override
    public void addGunStatTooltip(
            List<Component> tooltip
    ) {

        tooltip.add(
                Component.translatable(
                                "trait.alone_adventure.single_action_rapid_fire.description"
                        )
                        .withStyle(
                                ChatFormatting.WHITE
                        )
        );
        // 特质属性标题
        tooltip.add(
                Component.translatable(
                                "curios.modifiers.trait"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );


        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "reload_speed",
                2.0F
        );


        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "fire_rate",
                1.8F
        );


        GunStatTooltipBuilder.addMultiplier(
                tooltip,
                "spread",
                1.5F
        );

    }
}