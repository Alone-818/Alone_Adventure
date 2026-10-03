package Alone818.com.alone_adventure.armor;


import net.minecraft.resources.ResourceLocation;



public class ArmorTextureLayer {



    /**
     * 图层贴图
     */
    public final ResourceLocation texture;



    /**
     * 图层类型
     */
    public final LayerType type;



    /**
     * 默认颜色
     */
    public final int color;



    /**
     * 是否覆盖基础材质
     */
    public final boolean overlay;



    public ArmorTextureLayer(
            ResourceLocation texture,
            LayerType type,
            int color
    ){

        this(
                texture,
                type,
                color,
                true
        );

    }





    public ArmorTextureLayer(
            ResourceLocation texture,
            LayerType type,
            int color,
            boolean overlay
    ){

        this.texture = texture;

        this.type = type;

        this.color = color;

        this.overlay = overlay;

    }


}