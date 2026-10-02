package Alone818.com.alone_adventure.client.armor;


import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.armor.GeoArmorConfig;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;


import software.bernie.geckolib.renderer.GeoArmorRenderer;



public class DynamicGeoArmorRenderer
        extends GeoArmorRenderer<GeoArmorItem>{



    public DynamicGeoArmorRenderer(){

        super(
                new DynamicGeoArmorModel()
        );

    }




    @Override
    public ResourceLocation getTextureLocation(
            GeoArmorItem animatable
    ){


        GeoArmorConfig config =
                animatable.getConfig();


        if(config == null){

            return new ResourceLocation(
                    "alone_adventure",
                    "textures/armor/missing.png"
            );

        }


        return config.texture;

    }




    @Override
    public RenderType getRenderType(
            GeoArmorItem animatable,
            ResourceLocation texture,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            float partialTick
    ){


        return RenderType.armorCutoutNoCull(texture);

    }



}