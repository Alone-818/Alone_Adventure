package Alone818.com.alone_adventure.client;


import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.client.armor.DynamicGeoArmorRenderer;
import Alone818.com.alone_adventure.init.ModItems;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber(
        modid = "alone_adventure",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {



    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ){


    }


}