package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.adaptive_flesh;
import Alone818.com.alone_adventure.Curios.bleedingshield;
import Alone818.com.alone_adventure.Curios.crystalline_heart;
import Alone818.com.alone_adventure.Curios.night_contract;
import Alone818.com.alone_adventure.Curios.survival_whimper;
import Alone818.com.alone_adventure.Items.parryshield;
import Alone818.com.alone_adventure.Items.powersword;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Alone_adventure.MODID);

    // ===== 饰品 =====
    public static final RegistryObject<Item> ADAPTIVE_FLESH =
            ITEMS.register("adaptive_flesh", adaptive_flesh::new);

    public static final RegistryObject<Item> BLEEDINGSHIELD =
            ITEMS.register("bleedingshield", bleedingshield::new);

    public static final RegistryObject<Item> CRYSTALLINE_HEART =
            ITEMS.register("crystalline_heart", crystalline_heart::new);

    public static final RegistryObject<Item> NIGHT_CONTRACT =
            ITEMS.register("night_contract", night_contract::new);

    public static final RegistryObject<Item> SURVIVAL_WHIMPER =
            ITEMS.register("survival_whimper", survival_whimper::new);

    // ===== 装备物品 =====
    public static final RegistryObject<Item> PARRYSHIELD =
            ITEMS.register("parryshield", parryshield::new);

    public static final RegistryObject<Item> POWERSWORD =
            ITEMS.register("powersword", powersword::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }}
