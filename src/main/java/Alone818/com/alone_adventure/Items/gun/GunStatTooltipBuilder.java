package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.List;


public class GunStatTooltipBuilder {


    /**
     * 倍率属性
     * 例如:
     * 1.3F -> +30%
     * 0.8F -> -20%
     */
    public static void addMultiplier(
            List<Component> tooltip,
            String type,
            float multiplier
    ){

        float value = (multiplier - 1F) * 100F;


        if(value == 0){
            return;
        }


        boolean positive = value > 0;


        String name = getName(type);


        String text =
                (positive ? "+" : "-")
                        +
                        String.format(
                                "%.0f",
                                Math.abs(value)
                        )
                        +
                        "% "
                        +
                        name;



        tooltip.add(
                Component.literal(text)
                        .withStyle(
                                positive
                                        ?
                                        ChatFormatting.BLUE
                                        :
                                        ChatFormatting.RED
                        )
        );

    }





    /**
     * 百分比属性
     * 例如:
     * 0.2F -> +20%
     */
    public static void addPercent(
            List<Component> tooltip,
            String type,
            float value
    ){

        if(value == 0){
            return;
        }


        boolean positive=value>0;


        String text =
                (positive ? "+" : "-")
                        +
                        String.format(
                                "%.0f",
                                Math.abs(value*100)
                        )
                        +
                        "% "
                        +
                        getName(type);



        tooltip.add(
                Component.literal(text)
                        .withStyle(
                                positive
                                        ?
                                        ChatFormatting.BLUE
                                        :
                                        ChatFormatting.RED
                        )
        );

    }





    /**
     * 固定数值
     */
    public static void addFlat(
            List<Component> tooltip,
            String type,
            float value
    ){

        if(value==0){
            return;
        }


        boolean positive=value>0;


        String text =
                (positive ? "+" : "-")
                        +
                        String.format(
                                "%.0f",
                                Math.abs(value)
                        )
                        +
                        " "
                        +
                        getName(type);



        tooltip.add(
                Component.literal(text)
                        .withStyle(
                                positive
                                        ?
                                        ChatFormatting.BLUE
                                        :
                                        ChatFormatting.RED
                        )
        );

    }





    private static String getName(String type){

        return switch(type){

            case "reload_speed" ->
                    "装填速度";

            case "fire_rate" ->
                    "开火速度";

            case "spread" ->
                    "扩散";

            case "damage" ->
                    "伤害";

            case "bullet_speed" ->
                    "子弹速度";

            case "recoil" ->
                    "后坐力";

            case "critical_chance" ->
                    "暴击率";

            case "critical_damage" ->
                    "暴击伤害";

            default ->
                    type;
        };

    }


}