package Alone818.com.alone_adventure.client.armor;


import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;


import net.minecraft.resources.ResourceLocation;


import software.bernie.geckolib.model.GeoModel;



public class GeoArmorModel
        extends GeoModel<GeoArmorItem> {



    @Override
    public ResourceLocation getModelResource(
            GeoArmorItem animatable
    ){

        GeoArmorConfig config =
                animatable.getConfig();


        return config.model;

    }



    @Override
    public ResourceLocation getTextureResource(
            GeoArmorItem animatable
    ){

        GeoArmorConfig config =
                animatable.getConfig();


        return config.texture;

    }



    @Override
    public ResourceLocation getAnimationResource(
            GeoArmorItem animatable
    ){

        GeoArmorConfig config =
                animatable.getConfig();


        return config.animation;

    }


}