package Alone818.com.alone_adventure.client.armor;


import net.minecraft.resources.ResourceLocation;


public class GeoArmorEntry {


    private final ResourceLocation model;

    private final ResourceLocation texture;


    private final int defaultColor;


    public GeoArmorEntry(
            ResourceLocation model,
            ResourceLocation texture,
            int color
    ){

        this.model = model;
        this.texture = texture;
        this.defaultColor = color;

    }



    public ResourceLocation getModel(){
        return model;
    }


    public ResourceLocation getTexture(){
        return texture;
    }


    public int getDefaultColor(){
        return defaultColor;
    }

}