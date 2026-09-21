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
 *   <li>动画（两种形式，按顺序解析）：
 *       <ol>
 *         <li><b>单文件（推荐）</b>：{@code animations/gun/<物品id>.animation.json}
 *             —— 一个 Blockbench 动画项目的整包导出，全部行为动画共存于一个文件，
 *             内名直接用行为名（fire / reload / aim / run / idle），
 *             Blockbench 导出后改个文件名即用，无需编辑内名</li>
 *         <li><b>逐行为文件（兼容旧约定）</b>：
 *             {@code animations/gun/<物品id>_<行为>.animation.json}
 *             —— 每个行为一个文件（rifle / shotgun 现状）</li>
 *       </ol></li>
 * </ul>
 *
 * <b>内名宽容匹配</b>（{@link #pick}）：同一文件内依次尝试
 * 精确名 {@code <物品id>_<行为>} → 纯行为名 {@code <行为>}
 * → 文件内唯一动画（逐行为文件无论内名叫什么都采用）。
 *
 * <b>节省内存的设计</b>：不重复解析任何资源——动画直接引用 GeckoLib 全局缓存
 * （{@code animations/} 目录下所有 json 由 GeckoLib 统一烤入，随资源重载自动刷新）；
 * 本类仅缓存少量 {@link ResourceLocation}（避免逐帧构造）。
 */
public class GunGeoModel extends GeoModel<GunItem> {

    /** 动画目录（资源路径前缀，GeckoLib 扫描 animations/ 目录） */
    public static final String ANIM_DIR = "animations/gun/";

    /** 行为名（英文，即动画内名/文件名后缀），与 GunItemAnimation 常量一致 */
    public static final String FIRE = "fire";
    public static final String RELOAD = "reload";
    public static final String AIM = "aim";
    public static final String RUN = "run";
    public static final String IDLE = "idle";

    /** 本模型对应的物品 id（动画解析与路径拼接用） */
    private final String itemId;
    /** 基础几何模型 */
    private final ResourceLocation model;
    /** 双持举枪变体模型（存在才用） */
    private final ResourceLocation raisedModel;
    /** 贴图 */
    private final ResourceLocation texture;
    /** 单文件动画路径（解析走 {@link #getAnimation} 覆写，此处仅满足抽象方法） */
    private final ResourceLocation animations;

    /** 渲染单线程，缓存极小（每枪 1~6 条），免逐帧构造 ResourceLocation */
    private static final Map<String, ResourceLocation> PATH_CACHE = new HashMap<>();

    /** 本次渲染是否处于双持举枪姿态（渲染器 renderByItem 时写入） */
    private boolean dualWield;

    public GunGeoModel(String itemId) {
        this.itemId = itemId;
        this.model = new ResourceLocation(Alone_adventure.MODID, "geo/gun/" + itemId + ".geo.json");
        this.raisedModel = new ResourceLocation(Alone_adventure.MODID, "geo/gun/" + itemId + "_raised.geo.json");
        this.texture = new ResourceLocation(Alone_adventure.MODID, "textures/gun/" + itemId + ".png");
        this.animations = animPath(itemId + ".animation.json");
    }

    /** 双持举枪姿态开关（渲染器每帧 renderByItem 时设置） */
    public void setDualWield(boolean dual) {
        this.dualWield = dual;
    }

    /** 动画文件路径（带缓存）：参数为 animations/gun/ 下的相对文件名 */
    private static ResourceLocation animPath(String fileName) {
        return PATH_CACHE.computeIfAbsent(fileName,
                name -> new ResourceLocation(Alone_adventure.MODID, ANIM_DIR + name));
    }

    /** 取已烤入 GeckoLib 全局的动画文件（缺文件返回 null） */
    private static BakedAnimations baked(String fileName) {
        return GeckoLibCache.getBakedAnimations().get(animPath(fileName));
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
        // 单文件动画路径（实际解析全部经 getAnimation 覆写，此处仅满足抽象方法）
        return animations;
    }

    /**
     * 动画名解析覆写：控制器请求 {@code <物品id>_<行为>}（如 {@code pistol_fire}），
     * 剥出行为名后走 {@link #resolveAnimation} 双形式解析。
     */
    @Override
    public Animation getAnimation(GunItem gun, String name) {
        String prefix = itemId + "_";
        String behavior = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
        return resolveAnimation(itemId, behavior);
    }

    /**
     * 解析行为动画（双形式，单文件优先）：
     * <ol>
     *   <li>单文件 {@code animations/gun/<物品id>.animation.json}（Blockbench 整包导出）</li>
     *   <li>逐行为文件 {@code animations/gun/<物品id>_<行为>.animation.json}（旧约定）</li>
     * </ol>
     * 缺文件返回 null = 该行为未提供（控制器自动降级到下一优先级行为）。
     */
    public static Animation resolveAnimation(String itemId, String behavior) {
        Animation anim = pick(baked(itemId + ".animation.json"),
                itemId + "_" + behavior, behavior);
        if (anim != null) return anim;
        return pick(baked(itemId + "_" + behavior + ".animation.json"),
                itemId + "_" + behavior, behavior);
    }

    /**
     * 文件内挑动画（宽容匹配）：精确名 {@code <物品id>_<行为>} →
     * 纯行为名（Blockbench 默认导出内名）→ 文件内唯一动画。
     */
    private static Animation pick(BakedAnimations bakedAnimations, String fullName, String behavior) {
        if (bakedAnimations == null) return null;

        Animation exact = bakedAnimations.getAnimation(fullName);
        if (exact != null) return exact;

        Animation byBehavior = bakedAnimations.getAnimation(behavior);
        if (byBehavior != null) return byBehavior;

        // 逐行为文件内名随意（反正只有一个动画）：唯一即采用
        Map<String, Animation> all = bakedAnimations.animations();
        return all.size() == 1 ? all.values().iterator().next() : null;
    }
}
