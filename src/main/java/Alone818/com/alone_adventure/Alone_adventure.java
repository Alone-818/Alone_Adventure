package Alone818.com.alone_adventure;

import Alone818.com.alone_adventure.init.ModBlocks;
import Alone818.com.alone_adventure.init.ModCreativeModeTabs;
import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Alone_adventure.MODID)
public class Alone_adventure {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "alone_adventure";
    // Cooldown item identifier for powersword overclock
    public static final ResourceLocation POWERSWORD_OVERCLOCK_ITEM = new ResourceLocation(MODID, "powersword");
    // Directly reference a slf4j logger

    public Alone_adventure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEffects.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);




        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}

}