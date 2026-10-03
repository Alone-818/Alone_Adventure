package Alone818.com.alone_adventure.armor;

import net.minecraft.resources.ResourceLocation;

import static Alone818.com.alone_adventure.Alone_adventure.MODID;

public class GeoArmorRegistry {

    public static void register() {

        registerTestArmor();

    }

    private static void registerTestArmor() {

        GeoArmorConfig test =
                registerArmor(
                        "test",
                        "geo/test.geo.json",
                        "textures/armor/test/test_base.png",
                        "animations/test.animation.json",
                        true,
                        0xffffff
                );

        // =========================================================
        // 染色层
        // =========================================================

        test.addLayer(
                new ArmorTextureLayer(
                        new ResourceLocation(
                                MODID,
                                "textures/armor/test/test_dye.png"
                        ),
                        LayerType.DYE,
                        0xffffff
                )
        );

        // =========================================================
        // Pattern 1
        // =========================================================

        test.addPattern(
                new PatternPreset(
                        "1",
                        new ResourceLocation(
                                MODID,
                                "textures/armor/test_patterns/1.png"
                        )
                )
        );

        // =========================================================
        // Pattern 2
        // =========================================================

        test.addPattern(
                new PatternPreset(
                        "2",
                        new ResourceLocation(
                                MODID,
                                "textures/armor/test_patterns/2.png"
                        )
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
    ) {

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