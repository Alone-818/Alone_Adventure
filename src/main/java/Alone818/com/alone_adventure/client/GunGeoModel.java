package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public class GunGeoModel extends GeoModel<GunItem> {

    public static final String ANIM_DIR = "animations/gun/";

    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    private final String itemId;

    private final ResourceLocation model;
    private final ResourceLocation raisedModel;
    private final ResourceLocation texture;
    private final ResourceLocation animations;

    private static final Map<String, ResourceLocation> PATH_CACHE =
            new HashMap<>();

    private boolean dualWield;

    public GunGeoModel(String itemId) {

        this.itemId = itemId;

        this.model = new ResourceLocation(
                Alone_adventure.MODID,
                "geo/gun/" + itemId + ".geo.json"
        );

        this.raisedModel = new ResourceLocation(
                Alone_adventure.MODID,
                "geo/gun/" + itemId + "_raised.geo.json"
        );

        this.texture = new ResourceLocation(
                Alone_adventure.MODID,
                "textures/gun/" + itemId + ".png"
        );

        this.animations = animPath(
                itemId + ".animation.json"
        );
    }

    // =========================================================
    // 双持
    // =========================================================

    public void setDualWield(boolean dual) {
        this.dualWield = dual;
    }

    // =========================================================
    // Animation Resource Path
    // =========================================================

    private static ResourceLocation animPath(String fileName) {

        return PATH_CACHE.computeIfAbsent(
                fileName,
                name -> new ResourceLocation(
                        Alone_adventure.MODID,
                        ANIM_DIR + name
                )
        );
    }

    // =========================================================
    // Baked Animation
    // =========================================================

    private static BakedAnimations baked(String fileName) {

        return GeckoLibCache
                .getBakedAnimations()
                .get(animPath(fileName));
    }

    // =========================================================
    // Model
    // =========================================================

    @Override
    public ResourceLocation getModelResource(GunItem gun) {

        if (dualWield
                && GeckoLibCache
                .getBakedModels()
                .containsKey(raisedModel)) {

            return raisedModel;
        }

        return model;
    }

    // =========================================================
    // Texture
    // =========================================================

    @Override
    public ResourceLocation getTextureResource(GunItem gun) {
        return texture;
    }

    // =========================================================
    // Animation Resource
    // =========================================================

    @Override
    public ResourceLocation getAnimationResource(GunItem gun) {
        return animations;
    }

    // =========================================================
    // GeckoLib Animation Lookup
    // =========================================================

    @Override
    public Animation getAnimation(GunItem gun, String name) {

        return resolveAnimation(itemId, name);
    }

    // =========================================================
    // Resolve Animation
    // =========================================================

    public static Animation resolveAnimation(
            String itemId,
            String behavior
    ) {

        if (behavior == null || behavior.isEmpty()) {
            return null;
        }

        String actualBehavior =
                extractBehavior(behavior);

        /*
         * =========================================================
         * 单一 animation.json
         *
         * animations/gun/<itemId>.animation.json
         *
         * 里面：
         *
         * "reload"
         * "fire"
         * "aim"
         * "run"
         * "idle"
         * =========================================================
         */

        BakedAnimations single =
                baked(itemId + ".animation.json");

        Animation animation =
                pick(single, actualBehavior);

        if (animation != null) {
            return animation;
        }

        /*
         * =========================================================
         * 如果使用分开的动画文件：
         *
         * <itemId>_fire.animation.json
         * <itemId>_reload.animation.json
         * etc.
         * =========================================================
         */

        BakedAnimations separate =
                baked(itemId + "_" + actualBehavior + ".animation.json");

        animation =
                pick(separate, actualBehavior);

        if (animation != null) {
            return animation;
        }

        return null;
    }

    // =========================================================
    // Extract Behavior
    // =========================================================

    private static String extractBehavior(String name) {

        if (name == null || name.isEmpty()) {
            return "";
        }

        String value = name;

        /*
         * animation.pistol.fire
         */
        if (value.startsWith("animation.")) {
            value = value.substring(
                    "animation.".length()
            );
        }

        /*
         * pistol.fire
         */
        int dot = value.lastIndexOf('.');

        if (dot >= 0 && dot < value.length() - 1) {

            String suffix =
                    value.substring(dot + 1);

            if (isBehavior(suffix)) {
                return suffix;
            }
        }

        /*
         * pistol_fire
         */
        int underscore =
                value.lastIndexOf('_');

        if (underscore >= 0
                && underscore < value.length() - 1) {

            String suffix =
                    value.substring(underscore + 1);

            if (isBehavior(suffix)) {
                return suffix;
            }
        }

        /*
         * fire
         */
        if (isBehavior(value)) {
            return value;
        }

        return value;
    }

    // =========================================================
    // Behavior Check
    // =========================================================

    private static boolean isBehavior(String value) {

        return FIRE.equals(value)
                || RELOAD.equals(value)
                || AIM.equals(value)
                || RUN.equals(value)
                || IDLE.equals(value);
    }

    // =========================================================
    // Pick Animation
    // =========================================================

    private static Animation pick(
            BakedAnimations bakedAnimations,
            String behavior
    ) {

        if (bakedAnimations == null) {
            return null;
        }

        /*
         * 首先直接寻找：
         *
         * fire
         * reload
         * aim
         * run
         * idle
         */

        Animation animation =
                bakedAnimations.getAnimation(behavior);

        if (animation != null) {
            return animation;
        }

        /*
         * 如果单独文件里面只有一个动画，
         * 直接使用这个唯一动画。
         */

        Map<String, Animation> all =
                bakedAnimations.animations();

        if (all.size() == 1) {

            return all.values()
                    .iterator()
                    .next();
        }

        return null;
    }
}