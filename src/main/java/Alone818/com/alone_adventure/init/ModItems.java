package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.*;
import Alone818.com.alone_adventure.Items.*;
import Alone818.com.alone_adventure.Items.contract.*;
import Alone818.com.alone_adventure.Items.gun.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    // 猎人血清：契约饰品；R 键开启猎人视野——黑暗+黑白视角，
    // 高亮范围内生物/凋落物（发光）与容器（白色线框），仅本人可见
    public static final RegistryObject<Item> HUNTER_SERUM =
            ITEMS.register("hunter_serum", hunter_serum::new);

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

    // 投掷电击器：投掷型范围电击道具，弹道同雪球，掷出后高频低伤电击周围生物
    public static final RegistryObject<Item> SHOCK_DEVICE =
            ITEMS.register("shock_device", shock_device::new);

    // 连队团旗：战术道具；原地插旗 30 秒，范围内玩家获得抗性 II/耐力 I，
    // 首入范围补足 10 点黄心（每旗每人一次）；耐久 8 次，金锭修复，冷却 120 秒
    public static final RegistryObject<Item> REGIMENT_BANNER =
            ITEMS.register("regiment_banner", regiment_banner::new);

    // ===== 巨大镰刀 =====
    public static final RegistryObject<Item> REAPER_SCYTHE =
            ITEMS.register("reaper_scythe", reaper_scythe::new);

    // 机器爪刃：高攻速连击武器（远古科技机器人部件）；连续命中实体每次 +1 伤害，
    // 2 秒内未命中（含挥空）则加成清零
    public static final RegistryObject<Item> MACHINE_CLAW =
            ITEMS.register("machine_claw", machine_claw::new);

    // 火腿大棒：伤害 8/攻速 0.8/耐久 32；每次攻击回复 3 点饱食度，耗尽后变为骨头
    public static final RegistryObject<Item> HAM_CLUB =
            ITEMS.register("ham_club", ham_club::new);

    // 女武神头盔：皮革头盔防御 +2（共 3 点）；佩戴时造成伤害 +10%
    public static final RegistryObject<Item> VALKYRIE_HELMET =
            ITEMS.register("valkyrie_helmet", valkyrie_helmet::new);

    // ===== 道具物品 =====
    public static final RegistryObject<Item> INJECTION_EMPTY =
            ITEMS.register("injection_empty",
                    () -> new Item(new Item.Properties().stacksTo(16)));

    // 药水针剂：效果完全相同的针剂（NBT 一致）可堆叠至 4 个
    public static final RegistryObject<Item> INJECTION_SYRINGE =
            ITEMS.register("injection_syringe",
                    () -> new injection_template(new Item.Properties().stacksTo(4)));

    // 怪物残灰：击杀敌对生物概率掉落的合成材料（猎人血清提升掉率，不受抢夺影响）
    public static final RegistryObject<Item> MONSTER_ASH =
            ITEMS.register("monster_ash", monster_ash::new);

    // 血脉碎片：击杀敌对生物概率掉落的合成材料（猎人血清提升掉率，不受抢夺影响）
    public static final RegistryObject<Item> BLOOD_SHARD =
            ITEMS.register("blood_shard", blood_shard::new);

    // 急救包：使用回复最大生命值 60%，冷却 120 秒；耐久 3 次，不可堆叠、无法附魔
    public static final RegistryObject<Item> FIRST_AID_KIT =
            ITEMS.register("first_aid_kit", first_aid_kit::new);

    // 诡异八音盒：消耗道具；获得 90 点护盾（1:1 抵消伤害），但 60 秒后立刻死亡
    public static final RegistryObject<Item> EERIE_MUSIC_BOX =
            ITEMS.register("eerie_music_box", eerie_music_box::new);

    // ===== 枪械 =====

    // 弹药：长子弹（步枪用，高威力远射程）
    public static final RegistryObject<Item> LONG_BULLET =
            ITEMS.register("long_bullet", long_bullet::new);

    // 弹药：霰弹（霰弹枪用，多弹丸大散布）
    public static final RegistryObject<Item> SHOTGUN_SHELL =
            ITEMS.register("shotgun_shell", shotgun_shell::new);

    // 弹药：短子弹（手枪用，低威力快射速）
    public static final RegistryObject<Item> SHORT_BULLET =
            ITEMS.register("short_bullet", short_bullet::new);

    // 弹药：弩箭弹药（步枪可切换装填的箭形弹药）
    public static final RegistryObject<Item> CROSSBOW_BOLT =
            ITEMS.register("crossbow_bolt", crossbow_bolt::new);

    // 步枪：双手长枪模板（长子弹/弩箭弹药，G 键切换弹种）
    public static final RegistryObject<Item> RIFLE =
            ITEMS.register("rifle", rifle::new);

    // 霰弹枪：单手面杀伤模板（霰弹，6 弹丸大散布）
    public static final RegistryObject<Item> SHOTGUN =
            ITEMS.register("shotgun", shotgun::new);

    // 手枪：单手速射模板（短子弹，半自动，可双持）
    public static final RegistryObject<Item> PISTOL =
            ITEMS.register("pistol", pistol::new);

    // 金甜菜根：8 金粒围绕甜菜根合成；食用给予耐力，也是耐力药水的酿造原料
    public static final RegistryObject<Item> GOLDEN_BEETROOT =
            ITEMS.register("golden_beetroot",
                    () -> new golden_beetroot(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(6).saturationMod(1.2F)
                                    .effect(() -> new MobEffectInstance(ModEffects.ENDURANCE.get(), 1200), 1.0F)
                                    .alwaysEat()
                                    .build())));

    // 水手菠菜：食用回复 4 点饱食度与 8 点饱和度，给予力量 II 与生命恢复 II
    public static final RegistryObject<Item> SAILOR_SPINACH =
            ITEMS.register("sailor_spinach",
                    () -> new sailor_spinach(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationMod(1.0F)
                                    .effect(() -> new MobEffectInstance(
                                            MobEffects.DAMAGE_BOOST,
                                            sailor_spinach.STRENGTH_DURATION_TICKS, 1), 1.0F)
                                    .effect(() -> new MobEffectInstance(
                                            MobEffects.REGENERATION,
                                            sailor_spinach.REGEN_DURATION_TICKS, 1), 1.0F)
                                    .alwaysEat()
                                    .build())));

    // ===== 任务契约 =====
    // 旧版任务契约：击杀僵尸 x10 + 收集金甜菜根 x3
   /* public static final RegistryObject<Item> QUEST_CONTRACT =
            ITEMS.register("quest_contract",
                    () -> new QuestItem(new Item.Properties()
                                    .stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                            QuestItem.KillTask.of("kill_zombie", EntityType.ZOMBIE, 10),
                            QuestItem.CollectTask.of("collect_beetroot", GOLDEN_BEETROOT.get(), 3)));
*/
    // 复生契约：以唤魔者之力换取复活之约
    // 任务：击杀唤魔者 x2 + 提交金苹果 x16 + 提交不死图腾 x2 + 击杀幻魔者 x1
    public static final RegistryObject<Item> RESURRECTION_CONTRACT =
            ITEMS.register("resurrection_contract", resurrection_contract::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}