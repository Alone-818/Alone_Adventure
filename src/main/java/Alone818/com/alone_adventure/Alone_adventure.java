package Alone818.com.alone_adventure;

import Alone818.com.alone_adventure.crafting.EnduranceBrewingRecipe;
import Alone818.com.alone_adventure.crafting.ModRecipes;
import Alone818.com.alone_adventure.crafting.PotionUpgradeRecipe;
import Alone818.com.alone_adventure.init.ModBlocks;
import Alone818.com.alone_adventure.init.ModCreativeModeTabs;
import Alone818.com.alone_adventure.init.ModEntities;
import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import Alone818.com.alone_adventure.init.ModPotions;
import Alone818.com.alone_adventure.network.EagleSkillPacket;
import Alone818.com.alone_adventure.network.ReviveEffectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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

    // Define mod id in a common place for everything to reference
    public static final String MODID = "alone_adventure";

    // 网络通道（自定义包）
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1",
            s -> true,
            s -> true);

    // 包装 ID（网络包注册器）
    private static int packetId = 0;

    public static void registerPackets() {
        NETWORK.registerMessage(packetId++, ReviveEffectPacket.class,
                ReviveEffectPacket::encode, ReviveEffectPacket::decode, ReviveEffectPacket::handle);
        NETWORK.registerMessage(packetId++, EagleSkillPacket.class,
                EagleSkillPacket::encode, EagleSkillPacket::decode, EagleSkillPacket::handle);
    }

    // Directly reference a slf4j logger

    public Alone_adventure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModPotions.register(modEventBus);

        // 饰品数值配置（alone_adventure-common.toml，由 Config 在加载/重载时回填到各饰品静态字段）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);




        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        registerPackets();
        // 酿造配方：金甜菜根 → 耐力药水；萤石 → 强效；红石 → 延长
        event.enqueueWork(() -> {
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(new EnduranceBrewingRecipe());
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(
                    new PotionUpgradeRecipe(
                            ModPotions.ENDURANCE.get(),
                            net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.GLOWSTONE_DUST),
                            ModPotions.ENDURANCE_STRONG.get()));
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(
                    new PotionUpgradeRecipe(
                            ModPotions.ENDURANCE.get(),
                            net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.REDSTONE),
                            ModPotions.ENDURANCE_LONG.get()));
        });
    }

}