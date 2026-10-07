package Alone818.com.alone_adventure;

import Alone818.com.alone_adventure.armor.GeoArmorRegistry;
import Alone818.com.alone_adventure.datagen.ModRecipeProvider;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.init.*;
import Alone818.com.alone_adventure.crafting.ModRecipes;
import Alone818.com.alone_adventure.network.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(Alone_adventure.MODID)
public class Alone_adventure {


    public static final String MODID = "alone_adventure";


    // 网络通道
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1",
            s -> true,
            s -> true
    );


    private static int packetId = 0;


    public static void registerPackets() {

        NETWORK.registerMessage(packetId++,
                ReviveEffectPacket.class,
                ReviveEffectPacket::encode,
                ReviveEffectPacket::decode,
                ReviveEffectPacket::handle);

        NETWORK.registerMessage(packetId++,
                EagleSkillPacket.class,
                EagleSkillPacket::encode,
                EagleSkillPacket::decode,
                EagleSkillPacket::handle);

        NETWORK.registerMessage(packetId++,
                HunterVisionPacket.class,
                HunterVisionPacket::encode,
                HunterVisionPacket::decode,
                HunterVisionPacket::handle);

        NETWORK.registerMessage(packetId++,
                GunFirePacket.class,
                GunFirePacket::encode,
                GunFirePacket::decode,
                GunFirePacket::handle);

        NETWORK.registerMessage(packetId++,
                GunRecoilPacket.class,
                GunRecoilPacket::encode,
                GunRecoilPacket::decode,
                GunRecoilPacket::handle);

        NETWORK.registerMessage(packetId++,
                GunReloadPacket.class,
                GunReloadPacket::encode,
                GunReloadPacket::decode,
                GunReloadPacket::handle);

        NETWORK.registerMessage(packetId++,
                GunSelectAmmoPacket.class,
                GunSelectAmmoPacket::encode,
                GunSelectAmmoPacket::decode,
                GunSelectAmmoPacket::handle);

        NETWORK.registerMessage(packetId++,
                FactionJoinPacket.class,
                FactionJoinPacket::encode,
                FactionJoinPacket::decode,
                FactionJoinPacket::handle);
    }



    public Alone_adventure() {

        IEventBus modEventBus =
                FMLJavaModLoadingContext.get().getModEventBus();


        // 注册内容
        ModItems.register(modEventBus);

        ModBlocks.register(modEventBus);

        ModEffects.register(modEventBus);

        ModEntities.register(modEventBus);

        // 派系生物系列（士兵 / 弓手 / 法师）
        ModFactionEntities.register(modEventBus);
        modEventBus.addListener(ModFactionEntities::registerAttributes);
        modEventBus.addListener(ModFactionEntities::registerSpawnPlacements);

        // 派系系统：注册派系与生物归属
        ModFactions.register();

        ModCreativeModeTabs.register(modEventBus);

        ModRecipes.register(modEventBus);

        ModPotions.register(modEventBus);
        ModGeoArmorItems.ITEMS.register(modEventBus);

        GeoArmorRegistry.register();


        ModModificationItems.ITEMS.register(modEventBus);



        // 配置文件
        ModLoadingContext.get()
                .registerConfig(
                        ModConfig.Type.COMMON,
                        Config.SPEC
                );


        /*
         * CommonSetup阶段注册：
         * - 酿造配方
         * - 网络
         * - 非Registry内容
         */
        modEventBus.addListener(this::commonSetup);



        // 数据生成
        modEventBus.addListener(this::gatherData);



        MinecraftForge.EVENT_BUS.register(this);
    }



    /**
     * Common Setup
     */
    private void commonSetup(final FMLCommonSetupEvent event) {

        // 注册酿造配方
        ModBrewing.register(event);


        // 网络包注册
        registerPackets();
    }



    /**
     * Data Generator
     */
    private void gatherData(GatherDataEvent event) {

        event.getGenerator().addProvider(
                event.includeServer(),
                new ModRecipeProvider(
                        event.getGenerator().getPackOutput()
                )
        );
    }

}