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
import net.minecraft.world.item.UseAnim;
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
     * ItemStack 中保存当前图案 ID 的 NBT Key。
     */
    public static final String PATTERN_TAG =
            "alone_adventure:armor_pattern";

    private final String geoId;

    private final ArmorType armorType;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private ItemStack currentStack =
            ItemStack.EMPTY;

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

    public String getGeoId() {
        return geoId;
    }

    public ArmorType getArmorType() {
        return armorType;
    }

    public GeoArmorConfig getConfig() {
        return GeoArmorManager.get(geoId);
    }

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

    public ItemStack getCurrentStack() {
        return currentStack;
    }

    // =========================================================
    // 染色
    // =========================================================

    @Override
    public int getColor(
            ItemStack stack
    ) {

        GeoArmorConfig config =
                getConfig();

        if (config == null) {
            return 0xFFFFFF;
        }

        if (!config.dyeable) {
            return config.defaultColor;
        }

        return DyeableLeatherItem.super.getColor(stack);
    }

    // =========================================================
    // Pattern
    // =========================================================

    /**
     * 获取当前 Pattern ID。
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
     */
    public void setPattern(
            ItemStack stack,
            String patternId
    ) {

        if (stack == null ||
                stack.isEmpty()) {

            return;
        }

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
         * 只有注册过的 Pattern
         * 才允许写入 ItemStack。
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

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    /**
     * 是否存在 Pattern。
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
     * 右键切换 Pattern。
     *
     * 循环：
     *
     * 无图案
     * ↓
     * 1
     * ↓
     * 2
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

        if (config.patternPresets.isEmpty()) {
            return;
        }

        String currentId =
                getPatternId(stack);

        /*
         * 当前没有 Pattern：
         *
         * 选择第一个。
         *
         * 目前就是：
         * 1
         */
        if (currentId == null) {

            PatternPreset first =
                    config.patternPresets.get(0);

            setPattern(
                    stack,
                    first.id
            );

            return;
        }

        /*
         * 找到当前 Pattern 在列表中的位置。
         */
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

        /*
         * 当前 Pattern 不存在。
         *
         * 直接回到第一个。
         */
        if (currentIndex == -1) {

            PatternPreset first =
                    config.patternPresets.get(0);

            setPattern(
                    stack,
                    first.id
            );

            return;
        }

        /*
         * 如果已经是最后一个 Pattern，
         * 那么下一次回到无图案。
         *
         * 当前：
         *
         * 2
         *
         * ↓
         *
         * 无图案
         */
        if (currentIndex >=
                config.patternPresets.size() - 1) {

            clearPattern(stack);

            return;
        }

        /*
         * 否则进入下一个 Pattern。
         */
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
         * 只在服务端真正修改 NBT。
         *
         * 防止客户端和服务端各自修改一次。
         */
        if (!level.isClientSide) {

            cyclePattern(stack);
        }

        /*
         * 告诉 Minecraft：
         * 这次右键操作已经被物品处理。
         */
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

                    private DynamicGeoArmorRenderer renderer;

                    @Override
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original
                    ) {

                        if (renderer == null) {
                            renderer =
                                    new DynamicGeoArmorRenderer();
                        }

                        setCurrentStack(stack);

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