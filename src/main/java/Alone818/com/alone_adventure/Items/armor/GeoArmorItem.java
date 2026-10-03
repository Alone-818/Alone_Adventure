package Alone818.com.alone_adventure.Items.armor;

import Alone818.com.alone_adventure.armor.GeoArmorConfig;
import Alone818.com.alone_adventure.armor.GeoArmorManager;
import Alone818.com.alone_adventure.client.armor.DynamicGeoArmorRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GeoArmorItem
        extends ArmorItem
        implements GeoItem, DyeableLeatherItem {

    private final String geoId;

    private final ArmorType armorType;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    /*
     * 当前正在渲染的 ItemStack。
     *
     * GeoRenderLayer 的 render() 本身不会直接把 ItemStack
     * 作为参数传进来，所以这里暂时保存当前渲染 Stack。
     */
    private ItemStack currentStack = ItemStack.EMPTY;


    public GeoArmorItem(
            ArmorMaterial material,
            Type type,
            String geoId,
            ArmorType armorType
    ) {
        super(material, type, new Properties());

        this.geoId = geoId;
        this.armorType = armorType;
    }


    /*
     * 兼容旧的三参数注册方式
     */
    public GeoArmorItem(
            ArmorMaterial material,
            Type type,
            String geoId
    ) {
        this(material, type, geoId, null);
    }


    public String getGeoId() {
        return geoId;
    }


    public ArmorType getArmorType() {
        return armorType;
    }


    public GeoArmorConfig getConfig() {
        return GeoArmorManager.get(geoId);
    }


    /*
     * 保存当前正在渲染的 ItemStack
     */
    public void setCurrentStack(ItemStack stack) {

        if (stack == null) {
            this.currentStack = ItemStack.EMPTY;
            return;
        }

        this.currentStack = stack;
    }


    /*
     * Layer Renderer 使用
     */
    public ItemStack getCurrentStack() {
        return currentStack;
    }


    /*
     * 染色
     */
    @Override
    public int getColor(ItemStack stack) {

        GeoArmorConfig config = getConfig();

        if (config == null) {
            return 0xffffff;
        }

        /*
         * 不允许染色
         */
        if (!config.dyeable) {
            return config.defaultColor;
        }

        /*
         * 使用原版 DyeableLeatherItem 染色逻辑
         */
        return DyeableLeatherItem.super.getColor(stack);
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        /*
         * 暂时没有动画 Controller。
         *
         * 后面如果需要动画，可以在这里添加。
         */
    }


    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {

        consumer.accept(new IClientItemExtensions() {

            private DynamicGeoArmorRenderer renderer;


            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity,
                    ItemStack stack,
                    EquipmentSlot slot,
                    HumanoidModel<?> original
            ) {

                if (renderer == null) {
                    renderer = new DynamicGeoArmorRenderer();
                }

                /*
                 * 保存当前正在渲染的装备。
                 *
                 * ArmorTextureLayerRenderer 会从这里读取染色信息。
                 */
                setCurrentStack(stack);


                renderer.prepForRender(
                        entity,
                        stack,
                        slot,
                        original
                );

                return renderer;
            }
        });
    }
}