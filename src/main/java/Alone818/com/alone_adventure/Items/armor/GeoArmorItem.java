package Alone818.com.alone_adventure.Items.armor;


import Alone818.com.alone_adventure.armor.GeoArmorConfig;
import Alone818.com.alone_adventure.armor.GeoArmorManager;
import Alone818.com.alone_adventure.client.armor.DynamicGeoArmorRenderer;


import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;


import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;


import net.minecraftforge.client.extensions.common.IClientItemExtensions;


import java.util.function.Consumer;



public class GeoArmorItem
        extends ArmorItem
        implements GeoItem, DyeableLeatherItem {


    private final String geoId;



    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);



    public GeoArmorItem(
            ArmorMaterial material,
            Type type,
            String geoId
    ){

        super(
                material,
                type,
                new Properties()
        );


        this.geoId = geoId;

    }




    public String getGeoId(){

        return geoId;

    }



    public GeoArmorConfig getConfig(){


        GeoArmorConfig config =
                GeoArmorManager.get(geoId);



        return config;

    }




    @Override
    public int getColor(
            ItemStack stack
    ){

        GeoArmorConfig config = getConfig();


        if(config == null)
            return 0xFFFFFF;



        if(!config.dyeable)
            return config.defaultColor;



        return DyeableLeatherItem.super.getColor(stack);

    }




    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache(){

        return cache;

    }




    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ){

    }





    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ){


        consumer.accept(

                new IClientItemExtensions(){


                    private DynamicGeoArmorRenderer renderer;



                    @Override
                    public HumanoidModel<?> getHumanoidArmorModel(
                            LivingEntity entity,
                            ItemStack stack,
                            EquipmentSlot slot,
                            HumanoidModel<?> original
                    ){


                        if(renderer == null){

                            renderer =
                                    new DynamicGeoArmorRenderer();

                        }


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