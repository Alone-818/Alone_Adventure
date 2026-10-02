package Alone818.com.alone_adventure.armor;


import net.minecraft.resources.ResourceLocation;


public class GeoArmorConfig {


    public final String id;

    public final ResourceLocation model;

    public final ResourceLocation texture;

    public final ResourceLocation animation;


    public final boolean dyeable;

    public final int defaultColor;



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


}