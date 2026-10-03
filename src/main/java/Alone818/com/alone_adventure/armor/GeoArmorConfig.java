package Alone818.com.alone_adventure.armor;


import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;



public class GeoArmorConfig {



    public final String id;


    public final ResourceLocation model;


    // 默认纹理（不染色）
    public final ResourceLocation texture;


    public final ResourceLocation animation;



    // 是否允许染色
    public final boolean dyeable;


    // 无染色默认颜色
    public final int defaultColor;



    // 纹理层
    public final List<ArmorTextureLayer> layers =
            new ArrayList<>();



    public GeoArmorConfig(
            String id,
            ResourceLocation model,
            ResourceLocation texture,
            ResourceLocation animation,
            boolean dyeable,
            int defaultColor
    ){

        this.id = id;

        this.model = model;

        this.texture = texture;

        this.animation = animation;

        this.dyeable = dyeable;

        this.defaultColor = defaultColor;

    }




    public GeoArmorConfig addLayer(
            ArmorTextureLayer layer
    ){

        layers.add(layer);

        return this;

    }



    public List<ArmorTextureLayer> getLayers(){

        return layers;

    }


}