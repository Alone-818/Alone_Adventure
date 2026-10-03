package Alone818.com.alone_adventure.armor;


import net.minecraft.resources.ResourceLocation;

import static Alone818.com.alone_adventure.Alone_adventure.MODID;



public class GeoArmorRegistry {



    public static void register(){

        registerTestArmor();

    }





    private static void registerTestArmor(){



        GeoArmorConfig test =
                registerArmor(
                        "test",
                        "geo/test.geo.json",
                        "textures/armor/test_base.png",
                        "animations/test.animation.json",
                        true,
                        0xffffff
                );



        // 图案层，不染色
        test.addLayer(

                new ArmorTextureLayer(

                        new ResourceLocation(
                                MODID,
                                "textures/armor/test_pattern.png"
                        ),

                        LayerType.NORMAL,

                        0xffffff

                )

        );



        // 染色层
        test.addLayer(

                new ArmorTextureLayer(

                        new ResourceLocation(
                                MODID,
                                "textures/armor/test_dye.png"
                        ),

                        LayerType.DYE,

                        0xffffff

                )

        );


    }







    private static GeoArmorConfig registerArmor(
            String id,
            String model,
            String texture,
            String animation,
            boolean dyeable,
            int defaultColor
    ){


        GeoArmorConfig config =
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

                );


        GeoArmorManager.register(config);


        return config;

    }


}