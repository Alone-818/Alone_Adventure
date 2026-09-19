package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.*;
import Alone818.com.alone_adventure.Items.*;
import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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

    // 破帽：+7护甲/+20%生命/+3韧性；攻击20%概率施加易伤

    // 破损面具：+7护甲/+20%生命/+3韧性；攻击20%概率施加易伤，易伤被消耗时获得力量并回复固定生命
    public static final RegistryObject<Item> BROKEN_MASK =
            ITEMS.register("broken_mask", broken_mask::new);

    // 紧缚绷带：50%概率攻击后自我施加耐力，攻击虚弱目标附加等级×2伤害；主动技能获得护盾
    public static final RegistryObject<Item> BINDING_BANDAGE =
            ITEMS.register("binding_bandage", binding_bandage::new);

    // 亡灵秘典：契约饰品；减少20%最大生命值，减少95%攻击伤害，周期性获得黄心；
    // 攻击时施加灾厄效果（时长×等级取决于伤害），灾厄≥生命值时致命一击
    public static final RegistryObject<Item> NECROMANCER_LEDGER =
            ITEMS.register("necromancer_ledger", necromancer_ledger::new);

    // 封印王座：契约栏位；星辉随时间回复（上限12），按星辉提供伤害/护甲被动；
    // 受击扣红心时损失星辉；主动技能消耗全部星辉换取护盾与黄心
    public static final RegistryObject<Item> SEALED_THRONE =
            ITEMS.register("sealed_throne", sealed_throne::new);

    // 充能核心：多形态契约饰品；雷霆/冰冻/黑暗循环切换（R 键，冷却 5 秒）；
    // 当前形态随时间充能层数（上限与每层时间随形态不同），切换时释放并清零：
    // 雷霆→力量（层数+1）级 15 秒；冰冻→等同层数黄心；黑暗→下次攻击 +层数×8 伤害
    public static final RegistryObject<Item> CHARGING_CORE =
            ITEMS.register("charging_core", charging_core::new);

    // ===== 装备物品 =====
    public static final RegistryObject<Item> PARRYSHIELD =
            ITEMS.register("parryshield", parryshield::new);

    public static final RegistryObject<Item> POWERSWORD =
            ITEMS.register("powersword", powersword::new);

    public static final RegistryObject<Item> CHAINSAW_SWORD =
            ITEMS.register("chainsawsword", chainsawsword::new);

    public static final RegistryObject<Item> PAINSTRIKE_HAMMER =
            ITEMS.register("painstrike_hammer", painstrike_hammer::new);

    // 墨制刀刃：远程投掷武器，命中造成3点伤害并叠加虚弱（最高5级）；耐久随时间自动回复，不会损坏
    public static final RegistryObject<Item> INK_BLADE =
            ITEMS.register("ink_blade", ink_blade::new);

    // 星辉大剑：高 CD 重武器；持于手上每 3 秒聚 1 层星辉储能（上限 20），
    // 命中时每层 +5% 伤害并转化为同层数的伤害吸收（20 秒），随后储能清零
    public static final RegistryObject<Item> STARLIGHT_GREATSWORD =
            ITEMS.register("starlight_greatsword", starlight_greatsword::new);

    // ===== 巨大镰刀 =====
    public static final RegistryObject<Item> REAPER_SCYTHE =
            ITEMS.register("reaper_scythe", reaper_scythe::new);

    // 机器爪刃：高攻速连击武器（远古科技机器人部件）；连续命中实体每次 +1 伤害，
    // 2 秒内未命中（含挥空）则加成清零
    public static final RegistryObject<Item> MACHINE_CLAW =
            ITEMS.register("machine_claw", machine_claw::new);

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