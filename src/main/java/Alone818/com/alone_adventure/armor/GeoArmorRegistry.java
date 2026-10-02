package Alone818.com.alone_adventure.armor;


import net.minecraft.resources.ResourceLocation;

import static Alone818.com.alone_adventure.Alone_adventure.MODID;


public class GeoArmorRegistry {


    public static void register(){


        registerArmor(
                "test",
                "geo/test.geo.json",
                "textures/armor/test.png",
                "animations/test.animation.json",
                false,
                0xFFFFFF
        );


    }



    private static void registerArmor(
            String id,
            String model,
            String texture,
            String animation,
            boolean dyeable,
            int defaultColor
    ){


        GeoArmorManager.register(

                new GeoArmorConfig(

                        id,

                        new ResourceLocation(
                                MODID,
                                model
                        ),


                        new ResourceLocation(
                                MODID,
                                texture
                        ),


                        new ResourceLocation(
                                MODID,
                                animation
                        ),


                        dyeable,

                        defaultColor

                )

        );


    }


}