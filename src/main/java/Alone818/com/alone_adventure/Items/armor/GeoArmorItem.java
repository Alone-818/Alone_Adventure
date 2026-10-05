package Alone818.com.alone_adventure.Items.armor;

import Alone818.com.alone_adventure.armor.GeoArmorConfig;
import Alone818.com.alone_adventure.armor.GeoArmorManager;
import Alone818.com.alone_adventure.armor.PatternPreset;
import Alone818.com.alone_adventure.client.armor.DynamicGeoArmorRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GeoArmorItem
        extends ArmorItem
        implements GeoItem, DyeableLeatherItem {

    /**
     * ItemStack 中保存当前 Pattern ID 的 NBT Key。
     */
    public static final String PATTERN_TAG =
            "alone_adventure:armor_pattern";

    /**
     * GeoArmor 注册 ID。
     */
    private final String geoId;

    /**
     * Armor 类型。
     */
    private final ArmorType armorType;

    /**
     * GeckoLib Animatable Cache。
     */
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    /**
     * 当前正在进行穿戴渲染的 ItemStack。
     *
     * 注意：
     *
     * 这里只给 DynamicGeoArmorRenderer 使用。
     *
     * 背包/物品栏不会使用这个变量
     * 来进行 GeoItemRenderer 渲染。
     */
    private ItemStack currentStack =
            ItemStack.EMPTY;

    // =========================================================
    // Constructor
    // =========================================================

    public GeoArmorItem(
            ArmorMaterial material,
            Type type,
            String geoId,
            ArmorType armorType
    ) {
        super(
                material,
                type,
                new Properties()
        );

        this.geoId = geoId;
        this.armorType = armorType;
    }

    public GeoArmorItem(
            ArmorMaterial material,
            Type type,
            String geoId
    ) {
        this(
                material,
                type,
                geoId,
                null
        );
    }

    // =========================================================
    // 基础 GeoArmor
    // =========================================================

    /**
     * 获取 GeoArmor ID。
     */
    public String getGeoId() {
        return geoId;
    }

    /**
     * 获取 ArmorType。
     */
    public ArmorType getArmorType() {
        return armorType;
    }

    /**
     * 根据 geoId 获取 GeoArmorConfig。
     */
    public GeoArmorConfig getConfig() {
        return GeoArmorManager.get(geoId);
    }

    /**
     * 设置当前正在渲染的 ItemStack。
     *
     * 这个方法主要用于：
     *
     * DynamicGeoArmorRenderer
     *
     * 读取当前装备的染色和 Pattern。
     */
    public void setCurrentStack(
            ItemStack stack
    ) {

        if (stack == null) {

            this.currentStack =
                    ItemStack.EMPTY;

            return;
        }

        this.currentStack = stack;
    }

    /**
     * 获取当前正在渲染的 ItemStack。
     */
    public ItemStack getCurrentStack() {
        return currentStack;
    }

    // =========================================================
    // 染色系统
    // =========================================================

    @Override
    public int getColor(
            ItemStack stack
    ) {

        GeoArmorConfig config =
                getConfig();

        /*
         * 没有 GeoArmorConfig 时，
         * 默认使用白色。
         */
        if (config == null) {
            return 0xFFFFFF;
        }

        /*
         * 不可染色：
         * 使用配置中的默认颜色。
         */
        if (!config.dyeable) {
            return config.defaultColor;
        }

        /*
         * 可染色：
         * 使用 Minecraft DyeableLeatherItem
         * 的标准染色系统。
         */
        return DyeableLeatherItem.super.getColor(stack);
    }

    // =========================================================
    // Pattern
    // =========================================================

    /**
     * 获取当前 ItemStack 的 Pattern ID。
     *
     * 没有 Pattern 时返回 null。
     */
    public String getPatternId(
            ItemStack stack
    ) {

        if (stack == null ||
                stack.isEmpty()) {

            return null;
        }

        CompoundTag tag =
                stack.getTag();

        if (tag == null) {
            return null;
        }

        if (!tag.contains(PATTERN_TAG)) {
            return null;
        }

        String patternId =
                tag.getString(PATTERN_TAG);

        if (patternId.isEmpty()) {
            return null;
        }

        return patternId;
    }

    /**
     * 设置 Pattern。
     *
     * 只有 GeoArmorConfig 中已经注册的 Pattern
     * 才允许写入 ItemStack。
     */
    public void setPattern(
            ItemStack stack,
            String patternId
    ) {

        if (stack == null ||
                stack.isEmpty()) {

            return;
        }

        /*
         * 空 ID = 清除 Pattern。
         */
        if (patternId == null ||
                patternId.isEmpty()) {

            clearPattern(stack);

            return;
        }

        GeoArmorConfig config =
                getConfig();

        if (config == null) {
            return;
        }

        /*
         * Pattern 必须已经注册。
         */
        if (!config.hasPattern(patternId)) {
            return;
        }

        CompoundTag tag =
                stack.getOrCreateTag();

        tag.putString(
                PATTERN_TAG,
                patternId
        );
    }

    /**
     * 清除当前 Pattern。
     */
    public void clearPattern(
            ItemStack stack
    ) {

        if (stack == null ||
                stack.isEmpty()) {

            return;
        }

        CompoundTag tag =
                stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(PATTERN_TAG);

        /*
         * 如果没有其他 NBT，
         * 删除整个 Tag。
         */
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    /**
     * 判断 ItemStack 是否存在 Pattern。
     */
    public boolean hasPattern(
            ItemStack stack
    ) {

        return getPatternId(stack) != null;
    }

    /**
     * 获取当前 PatternPreset。
     */
    public PatternPreset getPattern(
            ItemStack stack
    ) {

        String patternId =
                getPatternId(stack);

        if (patternId == null) {
            return null;
        }

        GeoArmorConfig config =
                getConfig();

        if (config == null) {
            return null;
        }

        return config.getPattern(patternId);
    }

    // =========================================================
    // Pattern 测试切换
    // =========================================================

    /**
     * 右键循环切换 Pattern。
     *
     * 循环：
     *
     * 无图案
     * ↓
     * Pattern 1
     * ↓
     * Pattern 2
     * ↓
     * 无图案
     */
    private void cyclePattern(
            ItemStack stack
    ) {

        GeoArmorConfig config =
                getConfig();

        if (config == null) {
            return;
        }

        /*
         * 当前没有注册 Pattern，
         * 不进行任何操作。
         */
        if (config.patternPresets.isEmpty()) {
            return;
        }

        String currentId =
                getPatternId(stack);

        // -----------------------------------------------------
        // 无 Pattern → 第一个 Pattern
        // -----------------------------------------------------

        if (currentId == null) {

            PatternPreset first =
                    config.patternPresets.get(0);

            setPattern(
                    stack,
                    first.id
            );

            return;
        }

        // -----------------------------------------------------
        // 查找当前 Pattern
        // -----------------------------------------------------

        int currentIndex = -1;

        for (int i = 0;
             i < config.patternPresets.size();
             i++) {

            PatternPreset pattern =
                    config.patternPresets.get(i);

            if (pattern.id.equals(currentId)) {

                currentIndex = i;

                break;
            }
        }

        // -----------------------------------------------------
        // 当前 Pattern 不存在
        // → 回到第一个
        // -----------------------------------------------------

        if (currentIndex == -1) {

            PatternPreset first =
                    config.patternPresets.get(0);

            setPattern(
                    stack,
                    first.id
            );

            return;
        }

        // -----------------------------------------------------
        // 最后一个 Pattern
        // → 清除 Pattern
        // -----------------------------------------------------

        if (currentIndex >=
                config.patternPresets.size() - 1) {

            clearPattern(stack);

            return;
        }

        // -----------------------------------------------------
        // 进入下一个 Pattern
        // -----------------------------------------------------

        PatternPreset next =
                config.patternPresets.get(
                        currentIndex + 1
                );

        setPattern(
                stack,
                next.id
        );
    }

    // =========================================================
    // 右键测试
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        /*
         * 只在服务端修改 Pattern NBT。
         */
        if (!level.isClientSide) {

            cyclePattern(stack);
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    // =========================================================
    // GeckoLib
    // =========================================================

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return cache;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {

        /*
         * 当前暂时没有注册动画 Controller。
         *
         * 后续如果需要使用：
         *
         * test.animation.json
         *
         * 可以在这里加入动画 Controller。
         */
    }

    // =========================================================
    // Client Renderer
    // =========================================================

    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {

        consumer.accept(
                new IClientItemExtensions() {

                    /**
                     * GeoArmor 穿戴渲染器。
                     */
                    private DynamicGeoArmorRenderer renderer;

                    @Override
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original
                    ) {

                        /*
                         * 每个 Client Extension
                         * 只创建一个 Renderer。
                         */
                        if (renderer == null) {

                            renderer =
                                    new DynamicGeoArmorRenderer();
                        }

                        /*
                         * 保存当前装备的 ItemStack。
                         *
                         * DynamicGeoArmorRenderer
                         * 会通过这个 ItemStack
                         * 读取：
                         *
                         * - 染色
                         * - Pattern
                         */
                        setCurrentStack(stack);

                        /*
                         * 准备 GeoArmorRenderer。
                         */
                        renderer.prepForRender(
                                entity,
                                stack,
                                slot,
                                original
                        );

                        return renderer;
                    }
                }
        );
    }
}