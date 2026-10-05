package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.ImperialCurios.imperial_eagle;
import Alone818.com.alone_adventure.Curios.gun.GrandOpening;
import Alone818.com.alone_adventure.Curios.gun.MagazinePressure;
import Alone818.com.alone_adventure.Curios.gun.OneWithGun;
import Alone818.com.alone_adventure.Curios.gun.SingleActionRapidFire;
import Alone818.com.alone_adventure.Curios.miscCurios.*;
import Alone818.com.alone_adventure.Curios.towerCurios.*;
import Alone818.com.alone_adventure.Items.ImperialItems.chainsawsword;
import Alone818.com.alone_adventure.Items.ImperialItems.decapitation_axe;
import Alone818.com.alone_adventure.Items.ImperialItems.powersword;
import Alone818.com.alone_adventure.Items.ImperialItems.regiment_banner;
import Alone818.com.alone_adventure.Items.armor.GeoArmorItem;
import Alone818.com.alone_adventure.Items.bullet.*;
import Alone818.com.alone_adventure.Items.contract.*;
// evil_contract 已导入
import Alone818.com.alone_adventure.Items.bullet.crossbow_bolt;
import Alone818.com.alone_adventure.Items.bullet.long_bullet;
import Alone818.com.alone_adventure.Items.bullet.long_bullet_dum;
import Alone818.com.alone_adventure.Items.bullet.long_bullet_explosive;
import Alone818.com.alone_adventure.Items.bullet.long_bullet_poison;
import Alone818.com.alone_adventure.Items.gun.*;
import Alone818.com.alone_adventure.Items.bullet.shotgun_shell;
import Alone818.com.alone_adventure.Items.hunterItems.*;
import Alone818.com.alone_adventure.Items.miscItems.*;
import Alone818.com.alone_adventure.Items.towerItems.*;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.MobTier;
import Alone818.com.alone_adventure.faction.ModFactions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    // 高亮范围内生物/掉落物（发光）与容器（白色线框），仅本人可见
    public static final RegistryObject<Item> HUNTER_SERUM =
            ITEMS.register("hunter_serum", hunter_serum::new);
    public static final RegistryObject<Item> SINGLE_ACTION_RAPID_FIRE =
            ITEMS.register(
                    "single_action_rapid_fire",
                    () -> new SingleActionRapidFire());
    public static final RegistryObject<Item> MAGAZINE_PRESSURE =
            ITEMS.register(
                    "magazine_pressure",
                    MagazinePressure::new

            );

    // 人枪合一：枪械基础倍率削弱到 70%，每次开枪命中 +10%（上限 150%），空枪 -20%（最低回落到 70%）
    public static final RegistryObject<Item> ONE_WITH_GUN =
            ITEMS.register(
                    "one_with_gun",
                    OneWithGun::new
            );

    // 盛大开场：命中 100% ~ 95% 血量生物时额外造成 1200% 伤害并引发爆炸（TNT 范围，不破坏方块），
    // 随后 20 秒内伤害基础值 -35%
    public static final RegistryObject<Item> GRAND_OPENING =
            ITEMS.register(
                    "grand_opening",
                    GrandOpening::new
            );

    // 枪械构件：用于枪械升级的合成材料
    public static final RegistryObject<Item> GUN_PART =
            ITEMS.register("gun_part", gun_part::new);

    // 枪械盒：8 个枪械构件 + 末影之眼合成
    public static final RegistryObject<Item> GUN_BOX =
            ITEMS.register("gun_box", gun_box::new);

    // ===== 装备物品 =====
    public static final RegistryObject<Item> PARRYSHIELD =
            ITEMS.register("parryshield", parryshield::new);

    public static final RegistryObject<Item> POWERSWORD =
            ITEMS.register("powersword", powersword::new);

    public static final RegistryObject<Item> CHAINSAW_SWORD =
            ITEMS.register("chainsawsword", chainsawsword::new);
    // 斩首斧：10伤害/0.2攻速；击杀生物必掉对应头颅
    public static final RegistryObject<Item> DECAPITATION_AXE =
            ITEMS.register(
                    "decapitation_axe",
                    decapitation_axe::new
            );
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
    public static final RegistryObject<Item> ASSAULT_SHIELD =
            ITEMS.register("assaultshield", assaultshield::new);
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
                    () -> new injection_empty(new Item.Properties().stacksTo(16)));

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
// 通用子弹实体渲染物品
// 仅用于 BulletProjectile 的统一外观，不作为实际弹药使用
    public static final RegistryObject<Item> BULLET =
            ITEMS.register("bullet",
                    () -> new Item(new Item.Properties()));
    // 弹药：长子弹（步枪用，高威力远射程）
    public static final RegistryObject<Item> LONG_BULLET =
            ITEMS.register("long_bullet", long_bullet::new);

    // 弹药：霰弹（霰弹枪用，多弹丸大散布）
    public static final RegistryObject<Item> SHOTGUN_SHELL =
            ITEMS.register("shotgun_shell", shotgun_shell::new);

    // 弹药：短子弹（手枪用，低威力快射速）
    public static final RegistryObject<Item> SHORT_BULLET =
            ITEMS.register("short_bullet", short_bullet::new);
    // 短子弹-达姆：命中施加 Laceration VIII，持续 4 秒
    public static final RegistryObject<Item> SHORT_BULLET_DUM =
            ITEMS.register(
                    "short_bullet_dum",
                    short_bullet_dum::new
            );

    // 短子弹-爆炸：命中实体/方块产生爆炸，爆炸不伤害生物
    public static final RegistryObject<Item> SHORT_BULLET_EXPLOSIVE =
            ITEMS.register(
                    "short_bullet_explosive",
                    short_bullet_explosive::new
            );

    // 短子弹-硬币：每次额外 +6 弹丸，散布 ×2，射程 ×0.05，伤害 ×0.32
    public static final RegistryObject<Item> SHORT_BULLET_COIN =
            ITEMS.register(
                    "short_bullet_coin",
                    short_bullet_coin::new
            );

    // 短子弹-中毒：伤害 ×0.3，命中施加猛毒 III，可叠加（每中一枪 +2 级，上限 VIII，与绷带共用）
    public static final RegistryObject<Item> SHORT_BULLET_POISON =
            ITEMS.register(
                    "short_bullet_poison",
                    short_bullet_poison::new
            );

    // 短子弹-穿甲：额外造成 目标护甲值 ×0.5 的无视护甲伤害
    public static final RegistryObject<Item> SHORT_BULLET_ARMOR_PIERCING =
            ITEMS.register(
                    "short_bullet_armor_piercing",
                    short_bullet_armor_piercing::new
            );

    // 长子弹-达姆：命中施加 Laceration X，持续 6 秒
    public static final RegistryObject<Item> LONG_BULLET_DUM =
            ITEMS.register(
                    "long_bullet_dum",
                    long_bullet_dum::new
            );

    // 长子弹-爆炸：命中实体/方块产生爆炸，爆炸不伤害生物
    public static final RegistryObject<Item> LONG_BULLET_EXPLOSIVE =
            ITEMS.register(
                    "long_bullet_explosive",
                    long_bullet_explosive::new
            );

    // 长子弹-中毒：伤害 ×0.3，命中 Poison III 10 秒
    public static final RegistryObject<Item> LONG_BULLET_POISON =
            ITEMS.register(
                    "long_bullet_poison",
                    long_bullet_poison::new
            );

    // 长子弹-穿甲：额外造成 目标护甲值 ×0.5 的无视护甲伤害
    public static final RegistryObject<Item> LONG_BULLET_ARMOR_PIERCING =
            ITEMS.register(
                    "long_bullet_armor_piercing",
                    shotgun_shell_armor_piercing::new
            );
    public static final RegistryObject<Item> SHOTGUN_SHELL_DUM =
            ITEMS.register(
                    "shotgun_shell_dum",
                    shotgun_shell_dum::new
            );
    public static final RegistryObject<Item> SHOTGUN_SHELL_EXPLOSIVE =
            ITEMS.register(
                    "shotgun_shell_explosive",
                    shotgun_shell_explosive::new
            );
    public static final RegistryObject<Item> SHOTGUN_SHELL_POISON =
            ITEMS.register(
                    "shotgun_shell_poison",
                    shotgun_shell_poison::new
            );

    // 弹药：弩箭弹药（步枪可切换装填的箭形弹药）
    public static final RegistryObject<Item> CROSSBOW_BOLT =
            ITEMS.register("crossbow_bolt", crossbow_bolt::new);
    public static final RegistryObject<Item> CROSSBOW_BOLT_EXPLOSIVE =
            ITEMS.register("crossbow_bolt_explosive", crossbow_bolt_explosive::new);
    public static final RegistryObject<Item> CROSSBOW_BOLT_POISON =
            ITEMS.register("crossbow_bolt_poison", crossbow_bolt_poison::new);
    public static final RegistryObject<Item> CROSSBOW_BOLT_ARMOR_PIERCING =
            ITEMS.register("crossbow_bolt_armor_piercing", crossbow_bolt_armor_piercing::new);
    public static final RegistryObject<Item> CROSSBOW_BOLT_DUM =
            ITEMS.register("crossbow_bolt_dum", crossbow_bolt_dum::new);

    // 步枪：双手长枪模板（长子弹/弩箭弹药，G 键切换弹种）
    //public static final RegistryObject<Item> RIFLE =
    //        ITEMS.register("rifle", rifle::new);

    // 霰弹枪：单手面杀伤模板（霰弹，6 弹丸大散布）
    public static final RegistryObject<Item> DOUBLE_BARREL_SHOTGUN =
            ITEMS.register("double_barrel_shotgun", double_barrel_shotgun::new);

    // 手枪：单手速射模板（短子弹，半自动，可双持）
    public static final RegistryObject<Item> PISTOL =
            ITEMS.register("pistol", pistol::new);

    // 重型左轮：双手逐发装填（短子弹，6发弹夹，高后坐力小扩散）
    public static final RegistryObject<Item> HEAVY_REVOLVER =
            ITEMS.register("heavy_revolver", heavy_revolver::new);


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
    public static final RegistryObject<Item> BANRUO_SOUP =
            ITEMS.register(
                    "banruo_soup", banruo_soup::new

            );
    public static final RegistryObject<Item> AMMO_BOX =
            ITEMS.register(
                    "ammo_box",
                    () -> new AmmoBoxItem(
                            new Item.Properties()
                    )
            );

    // 仇恨之书：Shift+右键切换查看派系，右键查看该派系仇恨值与突袭可能性
    public static final RegistryObject<Item> HATRED_BOOK =
            ITEMS.register(
                    "hatred_book",
                    hatred_book::new
            );

    // 挑衅号角：右键吹响，无视冷却立刻对仇恨达标的派系发起突袭
    public static final RegistryObject<Item> PROVOCATION_HORN =
            ITEMS.register(
                    "provocation_horn",
                    provocation_horn::new
            );
    // ===== 任务契约 =====
    // 旧版任务契约：击杀僵尸 x10 + 收集金甜菜根 x3
   /* public static final RegistryObject<Item> QUEST_CONTRACT =
            ITEMS.register("quest_contract",
                    () -> new QuestItem(new Item.Properties()
                                    .stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                            QuestItem.KillTask.of("kill_zombie", EntityType.ZOMBIE, 10),
                            QuestItem.CollectTask.of("collect_beetroot", GOLDEN_BEETROOT.get(), 3)));
*/
    // 复生契约：以黄金供物换取复活之约
    // 任务：提交金块 x4 + 提交金苹果 x16 + 提交不死图腾 x2
    public static final RegistryObject<Item> RESURRECTION_CONTRACT =
            ITEMS.register("resurrection_contract", resurrection_contract::new);

    public static final RegistryObject<Item> TOWER_CONTRACT =
            ITEMS.register("tower_contract", tower_contract::new);

    public static final RegistryObject<Item> BATTLE_CONTRACT =
            ITEMS.register("battle_contract", battle_contract::new);

    // 帝国契约：击杀僵尸猪人 x20 + 到达沙漠群系 + 提交下界合金碎片 x4
    public static final RegistryObject<Item> IMPERIAL_CONTRACT =
            ITEMS.register("imperial_contract", imperial_contract::new);

    // 邪恶契约：击杀村民 x3 + 提交金块 x1 + 钻石块 x1 + 骨块 x8 + 腐肉 x32
    public static final RegistryObject<Item> EVIL_CONTRACT =
            ITEMS.register("evil_contract", evil_contract::new);

    // =========================================================
    // 派系掉落物
    //
    // 每派系一种材料 x 3 个等级（I / II / III），
    // 等级对应派系生物的 MobTier。
    // 8 个低阶掉落物合成 1 个高阶
    // （配方 JSON 在 data/alone_adventure/recipes）。
    // =========================================================

    // 帝国军衔 I / II / III
    public static final RegistryObject<Item> EMPIRE_INSIGNIA_I =
            ITEMS.register(
                    "empire_insignia_i",
                    () -> new faction_drop(
                            ModFactions.EMPIRE,
                            MobTier.TIER_1
                    )
            );

    public static final RegistryObject<Item> EMPIRE_INSIGNIA_II =
            ITEMS.register(
                    "empire_insignia_ii",
                    () -> new faction_drop(
                            ModFactions.EMPIRE,
                            MobTier.TIER_2
                    )
            );

    public static final RegistryObject<Item> EMPIRE_INSIGNIA_III =
            ITEMS.register(
                    "empire_insignia_iii",
                    () -> new faction_drop(
                            ModFactions.EMPIRE,
                            MobTier.TIER_3
                    )
            );

    // 亡灵精魄 I / II / III
    public static final RegistryObject<Item> UNDEAD_SOUL_I =
            ITEMS.register(
                    "undead_soul_i",
                    () -> new faction_drop(
                            ModFactions.UNDEAD,
                            MobTier.TIER_1
                    )
            );

    public static final RegistryObject<Item> UNDEAD_SOUL_II =
            ITEMS.register(
                    "undead_soul_ii",
                    () -> new faction_drop(
                            ModFactions.UNDEAD,
                            MobTier.TIER_2
                    )
            );

    public static final RegistryObject<Item> UNDEAD_SOUL_III =
            ITEMS.register(
                    "undead_soul_iii",
                    () -> new faction_drop(
                            ModFactions.UNDEAD,
                            MobTier.TIER_3
                    )
            );

    // 恶魔鲜血 I / II / III
    public static final RegistryObject<Item> DEMON_BLOOD_I =
            ITEMS.register(
                    "demon_blood_i",
                    () -> new faction_drop(
                            ModFactions.DEMON,
                            MobTier.TIER_1
                    )
            );

    public static final RegistryObject<Item> DEMON_BLOOD_II =
            ITEMS.register(
                    "demon_blood_ii",
                    () -> new faction_drop(
                            ModFactions.DEMON,
                            MobTier.TIER_2
                    )
            );

    public static final RegistryObject<Item> DEMON_BLOOD_III =
            ITEMS.register(
                    "demon_blood_iii",
                    () -> new faction_drop(
                            ModFactions.DEMON,
                            MobTier.TIER_3
                    )
            );

    // 部落兽骨 I / II / III
    public static final RegistryObject<Item> TRIBE_BONE_I =
            ITEMS.register(
                    "tribe_bone_i",
                    () -> new faction_drop(
                            ModFactions.TRIBE,
                            MobTier.TIER_1
                    )
            );

    public static final RegistryObject<Item> TRIBE_BONE_II =
            ITEMS.register(
                    "tribe_bone_ii",
                    () -> new faction_drop(
                            ModFactions.TRIBE,
                            MobTier.TIER_2
                    )
            );

    public static final RegistryObject<Item> TRIBE_BONE_III =
            ITEMS.register(
                    "tribe_bone_iii",
                    () -> new faction_drop(
                            ModFactions.TRIBE,
                            MobTier.TIER_3
                    )
            );

    /** 全部派系掉落物（派系 x 等级，注册顺序），创造模式物品栏批量展开用 */
    public static final List<RegistryObject<Item>> FACTION_DROPS =
            List.of(
                    EMPIRE_INSIGNIA_I,
                    EMPIRE_INSIGNIA_II,
                    EMPIRE_INSIGNIA_III,
                    UNDEAD_SOUL_I,
                    UNDEAD_SOUL_II,
                    UNDEAD_SOUL_III,
                    DEMON_BLOOD_I,
                    DEMON_BLOOD_II,
                    DEMON_BLOOD_III,
                    TRIBE_BONE_I,
                    TRIBE_BONE_II,
                    TRIBE_BONE_III
            );

    // =========================================================
    // 仇恨符
    //
    // 每派系 x 3 等级 x 增/减 = 24 个物品。
    // 用对应派系对应等级的掉落物制作
    // （配方 JSON 在 data/alone_adventure/recipes）：
    // I 级符 = 同派系 I 级掉落物
    // II 级符 = 同派系 II 级掉落物
    // III 级符 = 同派系 III 级掉落物
    // 增 / 减各占掉落物网格对角
    // （其余格放掉落物，区分增减两版）。
    // =========================================================

    // 帝国仇恨符 I / II / III
    public static final RegistryObject<Item> EMPIRE_TALISMAN_UP_I =
            ITEMS.register(
                    "empire_talisman_up_i",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_1,
                            true
                    )
            );

    public static final RegistryObject<Item> EMPIRE_TALISMAN_UP_II =
            ITEMS.register(
                    "empire_talisman_up_ii",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_2,
                            true
                    )
            );

    public static final RegistryObject<Item> EMPIRE_TALISMAN_UP_III =
            ITEMS.register(
                    "empire_talisman_up_iii",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_3,
                            true
                    )
            );

    public static final RegistryObject<Item> EMPIRE_TALISMAN_DOWN_I =
            ITEMS.register(
                    "empire_talisman_down_i",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_1,
                            false
                    )
            );

    public static final RegistryObject<Item> EMPIRE_TALISMAN_DOWN_II =
            ITEMS.register(
                    "empire_talisman_down_ii",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_2,
                            false
                    )
            );

    public static final RegistryObject<Item> EMPIRE_TALISMAN_DOWN_III =
            ITEMS.register(
                    "empire_talisman_down_iii",
                    () -> new hatred_talisman(
                            ModFactions.EMPIRE,
                            MobTier.TIER_3,
                            false
                    )
            );

    // 亡灵仇恨符 I / II / III
    public static final RegistryObject<Item> UNDEAD_TALISMAN_UP_I =
            ITEMS.register(
                    "undead_talisman_up_i",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_1,
                            true
                    )
            );

    public static final RegistryObject<Item> UNDEAD_TALISMAN_UP_II =
            ITEMS.register(
                    "undead_talisman_up_ii",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_2,
                            true
                    )
            );

    public static final RegistryObject<Item> UNDEAD_TALISMAN_UP_III =
            ITEMS.register(
                    "undead_talisman_up_iii",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_3,
                            true
                    )
            );

    public static final RegistryObject<Item> UNDEAD_TALISMAN_DOWN_I =
            ITEMS.register(
                    "undead_talisman_down_i",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_1,
                            false
                    )
            );

    public static final RegistryObject<Item> UNDEAD_TALISMAN_DOWN_II =
            ITEMS.register(
                    "undead_talisman_down_ii",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_2,
                            false
                    )
            );

    public static final RegistryObject<Item> UNDEAD_TALISMAN_DOWN_III =
            ITEMS.register(
                    "undead_talisman_down_iii",
                    () -> new hatred_talisman(
                            ModFactions.UNDEAD,
                            MobTier.TIER_3,
                            false
                    )
            );

    // 恶魔仇恨符 I / II / III
    public static final RegistryObject<Item> DEMON_TALISMAN_UP_I =
            ITEMS.register(
                    "demon_talisman_up_i",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_1,
                            true
                    )
            );

    public static final RegistryObject<Item> DEMON_TALISMAN_UP_II =
            ITEMS.register(
                    "demon_talisman_up_ii",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_2,
                            true
                    )
            );

    public static final RegistryObject<Item> DEMON_TALISMAN_UP_III =
            ITEMS.register(
                    "demon_talisman_up_iii",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_3,
                            true
                    )
            );

    public static final RegistryObject<Item> DEMON_TALISMAN_DOWN_I =
            ITEMS.register(
                    "demon_talisman_down_i",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_1,
                            false
                    )
            );

    public static final RegistryObject<Item> DEMON_TALISMAN_DOWN_II =
            ITEMS.register(
                    "demon_talisman_down_ii",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_2,
                            false
                    )
            );

    public static final RegistryObject<Item> DEMON_TALISMAN_DOWN_III =
            ITEMS.register(
                    "demon_talisman_down_iii",
                    () -> new hatred_talisman(
                            ModFactions.DEMON,
                            MobTier.TIER_3,
                            false
                    )
            );

    // 部落仇恨符 I / II / III
    public static final RegistryObject<Item> TRIBE_TALISMAN_UP_I =
            ITEMS.register(
                    "tribe_talisman_up_i",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_1,
                            true
                    )
            );

    public static final RegistryObject<Item> TRIBE_TALISMAN_UP_II =
            ITEMS.register(
                    "tribe_talisman_up_ii",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_2,
                            true
                    )
            );

    public static final RegistryObject<Item> TRIBE_TALISMAN_UP_III =
            ITEMS.register(
                    "tribe_talisman_up_iii",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_3,
                            true
                    )
            );

    public static final RegistryObject<Item> TRIBE_TALISMAN_DOWN_I =
            ITEMS.register(
                    "tribe_talisman_down_i",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_1,
                            false
                    )
            );

    public static final RegistryObject<Item> TRIBE_TALISMAN_DOWN_II =
            ITEMS.register(
                    "tribe_talisman_down_ii",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_2,
                            false
                    )
            );

    public static final RegistryObject<Item> TRIBE_TALISMAN_DOWN_III =
            ITEMS.register(
                    "tribe_talisman_down_iii",
                    () -> new hatred_talisman(
                            ModFactions.TRIBE,
                            MobTier.TIER_3,
                            false
                    )
            );

    /** 全部仇恨符（派系 x 等级 x 增/减），创造模式物品栏批量展开用 */
    public static final List<RegistryObject<Item>> FACTION_TALISMANS =
            List.of(
                    EMPIRE_TALISMAN_UP_I,
                    EMPIRE_TALISMAN_UP_II,
                    EMPIRE_TALISMAN_UP_III,
                    EMPIRE_TALISMAN_DOWN_I,
                    EMPIRE_TALISMAN_DOWN_II,
                    EMPIRE_TALISMAN_DOWN_III,
                    UNDEAD_TALISMAN_UP_I,
                    UNDEAD_TALISMAN_UP_II,
                    UNDEAD_TALISMAN_UP_III,
                    UNDEAD_TALISMAN_DOWN_I,
                    UNDEAD_TALISMAN_DOWN_II,
                    UNDEAD_TALISMAN_DOWN_III,
                    DEMON_TALISMAN_UP_I,
                    DEMON_TALISMAN_UP_II,
                    DEMON_TALISMAN_UP_III,
                    DEMON_TALISMAN_DOWN_I,
                    DEMON_TALISMAN_DOWN_II,
                    DEMON_TALISMAN_DOWN_III,
                    TRIBE_TALISMAN_UP_I,
                    TRIBE_TALISMAN_UP_II,
                    TRIBE_TALISMAN_UP_III,
                    TRIBE_TALISMAN_DOWN_I,
                    TRIBE_TALISMAN_DOWN_II,
                    TRIBE_TALISMAN_DOWN_III
            );

    /**
     * 查询派系 x 等级对应的掉落物
     * （派系无对应掉落物时返回 null）。
     *
     * 依赖 FACTION_DROPS 的顺序：
     * 派系为外层（与 ModFactions.all() 一致），
     * 等级为内层（1 ~ 3 级）。
     * 以后新增派系时记得同步补一组掉落物。
     */
    @Nullable
    public static Item getFactionDrop(
            Faction faction,
            MobTier tier
    ) {

        Faction[] all = ModFactions.all();

        for (int f = 0; f < all.length; f++) {

            if (all[f] == faction) {

                int index =
                        f * 3 + tier.getLevel() - 1;

                // 派系没配齐 3 个等级的掉落物
                if (index >= FACTION_DROPS.size()) {
                    return null;
                }

                return FACTION_DROPS
                        .get(index)
                        .get();
            }
        }

        return null;
    }

    // =========================================================
    // 派系生物生成蛋
    //
    // 全部派系 x 职业 x 军衔批量注册
    // （见 ModFactionEntities），
    // 主色 = 派系色，副色 = 职业军衔色。
    // =========================================================

    /** 全部派系生物生成蛋（注册顺序），创造模式物品栏批量展开用 */
    public static final List<RegistryObject<Item>> FACTION_SPAWN_EGGS =
            registerFactionSpawnEggs();

    private static List<RegistryObject<Item>> registerFactionSpawnEggs() {

        List<RegistryObject<Item>> eggs =
                new ArrayList<>();

        for (ModFactionEntities.FactionMobEntry entry
                : ModFactionEntities.allEntries()) {

            eggs.add(
                    ITEMS.register(
                            entry.getEntityId()
                                    + "_spawn_egg",
                            () -> new ForgeSpawnEggItem(
                                    () -> entry.getType().get(),
                                    entry.getFaction().getColor(),
                                    entry.getMobClass()
                                            .getEggColor(entry.getTier()),
                                    new Item.Properties()
                            )
                    )
            );
        }

        return eggs;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}