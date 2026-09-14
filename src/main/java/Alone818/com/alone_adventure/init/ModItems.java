package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.*;
import Alone818.com.alone_adventure.Items.*;
import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
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

    public static final RegistryObject<Item> DRAGON_POWER =
            ITEMS.register("dragon_power", dragon_power::new);

    // 帝国天鹰：负面效果反转、免疫火焰岩浆伤害、主动技能（按键触发）
    public static final RegistryObject<Item> IMPERIAL_EAGLE =
            ITEMS.register("imperial_eagle", imperial_eagle::new);

    // 破损面具：+7护甲/+20%生命/+3韧性；攻击20%概率施加易伤，易伤被消耗时获得力量并回复固定生命
    public static final RegistryObject<Item> BROKEN_MASK =
            ITEMS.register("broken_mask", broken_mask::new);

    // 紧缚绷带：10%概率攻击后自我施加耐力，主动技能获得护盾
    public static final RegistryObject<Item> BINDING_BANDAGE =
            ITEMS.register("binding_bandage", binding_bandage::new);

    // ===== 装备物品 =====
    public static final RegistryObject<Item> PARRYSHIELD =
            ITEMS.register("parryshield", parryshield::new);

    public static final RegistryObject<Item> POWERSWORD =
            ITEMS.register("powersword", powersword::new);

    public static final RegistryObject<Item> CHAINSAW_SWORD =
            ITEMS.register("chainsawsword", chainsawsword::new);

    public static final RegistryObject<Item> PAINSTRIKE_HAMMER =
            ITEMS.register("painstrike_hammer", painstrike_hammer::new);

    // 墨制刀刃：远程投掷武器，命中施加虚弱；耐久随时间自动回复，不会损坏
    public static final RegistryObject<Item> INK_BLADE =
            ITEMS.register("ink_blade", ink_blade::new);

    // ===== 道具物品 =====
    public static final RegistryObject<Item> INJECTION_EMPTY =
            ITEMS.register("injection_empty",
                    () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> INJECTION_SYRINGE =
            ITEMS.register("injection_syringe",
                    () -> new injection_template(new Item.Properties().stacksTo(1)));

    // 金甜菜根：8 金粒围绕甜菜根合成；食用给予耐力，也是耐力药水的酿造原料
    public static final RegistryObject<Item> GOLDEN_BEETROOT =
            ITEMS.register("golden_beetroot",
                    () -> new golden_beetroot(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(6).saturationMod(1.2F)
                                    // 直接吃也给予耐力 I 60 秒（1200 tick）
                                    .effect(() -> new MobEffectInstance(ModEffects.ENDURANCE.get(), 1200), 1.0F)
                                    .alwaysEat()
                                    .build())));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
