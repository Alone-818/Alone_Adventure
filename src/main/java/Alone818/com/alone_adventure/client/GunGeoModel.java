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

/**
 * 枪械 GeckoLib 模型 —— Blockbench（基岩格式）模型/贴图/动画的资源定位与解析。
 *
 * <b>资源约定</b>（全部由 Blockbench 导出生成，替换文件即生效，F3+T 重载）：
 * <ul>
 *   <li>几何模型：{@code assets/alone_adventure/geo/gun/<物品id>.geo.json}
 *       （Blockbench → 导出 → 基岩版模型）；双持举枪变体
 *       {@code <物品id>_raised.geo.json}（存在才启用，缺失自动回退基础模型）</li>
 *   <li>贴图：{@code assets/alone_adventure/textures/gun/<物品id>.png}</li>
 *   <li>动画：{@code assets/alone_adventure/animations/gun/<物品id>_<行为>.animation.json}
 *       （Blockbench → 导出 → 基岩版动画；<b>文件名与文件内动画名都必须是
 *       {@code <物品id>_<行为>}</b>，行为取英文：fire / reload / aim / run / idle）</li>
 * </ul>
 *
 * <b>逐行为文件的实现</b>：GeckoLib 的资源重载会把 {@code animations/} 目录下
 * 所有 json 全部烤入全局缓存（以完整资源路径为键），因此
 * {@link #getAnimation} 覆写为按 {@code <物品id>_<行为>} 直查<b>同名行为文件</b>，
 * 无需把一把枪的全部行为挤进单个动画文件。
 *
 * <b>节省内存的设计</b>：不重复解析任何资源——动画直接引用 GeckoLib 全局缓存
 * （随资源重载自动刷新）；本类仅缓存少量 {@link ResourceLocation}
 * （每物品每行为一个，避免逐帧构造）。
 */
public class GunGeoModel extends GeoModel<GunItem> {

    /** 动画目录（资源路径前缀，GeckoLib 扫描 animations/ 目录） */
    public static final String ANIM_DIR = "animations/gun/";

    /** 基础几何模型 */
    private final ResourceLocation model;
    /** 双持举枪变体模型（存在才用） */
    private final ResourceLocation raisedModel;
    /** 贴图 */
    private final ResourceLocation texture;
    /** 主动画资源（解析走 {@link #getAnimation} 覆写，此处仅满足抽象方法） */
    private final ResourceLocation animations;

    /** 行为名 → 行为文件路径缓存（每物品个位数条目；渲染单线程） */
    private final Map<String, ResourceLocation> animPaths = new HashMap<>();

    /** 本次渲染是否处于双持举枪姿态（渲染器 renderByItem 时写入） */
    private boolean dualWield;

    public GunGeoModel(String itemId) {
        this.model = new ResourceLocation(Alone_adventure.MODID, "geo/gun/" + itemId + ".geo.json");
        this.raisedModel = new ResourceLocation(Alone_adventure.MODID, "geo/gun/" + itemId + "_raised.geo.json");
        this.texture = new ResourceLocation(Alone_adventure.MODID, "textures/gun/" + itemId + ".png");
        this.animations = animPath(itemId + "_" + GunItemAnimation.IDLE);
    }

    /** 双持举枪姿态开关（渲染器每帧 renderByItem 时设置） */
    public void setDualWield(boolean dual) {
        this.dualWield = dual;
    }

    /** 行为文件路径：animations/gun/<物品id>_<行为>.animation.json（带缓存） */
    private ResourceLocation animPath(String animationName) {
        return animPaths.computeIfAbsent(animationName,
                name -> new ResourceLocation(Alone_adventure.MODID, ANIM_DIR + name + ".animation.json"));
    }

    @Override
    public ResourceLocation getModelResource(GunItem gun) {
        // 双持举枪变体：文件存在才切换（GeckoLib 缓存键即完整路径，重载自动跟上）
        if (dualWield && GeckoLibCache.getBakedModels().containsKey(raisedModel)) {
            return raisedModel;
        }
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(GunItem gun) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(GunItem gun) {
        return animations;
    }

    /**
     * 动画名解析覆写：名字即 {@code <物品id>_<行为>}，直查同名行为文件。
     * GeckoLib 的动画解析（AnimationProcessor）统一经此方法取 Animation，
     * 因此控制器里 {@code thenPlay("pistol_fire")} 会命中
     * {@code animations/gun/pistol_fire.animation.json} 中的同名动画。
     */
    @Override
    public Animation getAnimation(GunItem gun, String name) {
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(animPath(name));
        return baked != null ? baked.getAnimation(name) : null;
    }
}
