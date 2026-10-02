package Alone818.com.alone_adventure.client.armor;


import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;


import net.minecraft.resources.ResourceLocation;


import software.bernie.geckolib.model.GeoModel;



public class DynamicGeoArmorModel
        extends GeoModel<GeoArmorItem>{



    @Override
    public ResourceLocation getModelResource(
            GeoArmorItem animatable
    ){


        GeoArmorConfig config =
                animatable.getConfig();


        if(config == null){


            return new ResourceLocation(
                    "alone_adventure",
                    "geo/test.geo.json"
            );


        }


        return config.model;


    }@Override
    public ResourceLocation getTextureResource(
            GeoArmorItem animatable
    ){


        GeoArmorConfig config =
                animatable.getConfig();


        if(config == null){

            return new ResourceLocation(
                    "alone_adventure",
                    "textures/armor/test.png"
            );

        }


        return config.texture;

    }

    @Override
    public ResourceLocation getAnimationResource(
            GeoArmorItem animatable
    ){


        GeoArmorConfig config =
                animatable.getConfig();


        if(config == null){

            return new ResourceLocation(
                    "alone_adventure",
                    "animations/test.animation.json"
            );

        }


        return config.animation;

    }

}