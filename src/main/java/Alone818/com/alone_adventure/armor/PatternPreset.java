package Alone818.com.alone_adventure.armor;

import net.minecraft.resources.ResourceLocation;

public class PatternPreset {

    public final String id;

    public final ResourceLocation texture;

    public final int color;

    public PatternPreset(
            String id,
            ResourceLocation texture
    ) {
        this(
                id,
                texture,
                0xffffff
        );
    }

    public PatternPreset(
            String id,
            ResourceLocation texture,
            int color
    ) {
        this.id = id;
        this.texture = texture;
        this.color = color;
    }
}