package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;

import Alone818.com.alone_adventure.Curios.towerCurios.necromancer_ledger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Alone_adventure.MODID
            );

    // =========================================================
    // 武器标签
    // =========================================================

    public static final RegistryObject<CreativeModeTab> WEAPONS_TAB =
            CREATIVE_MODE_TABS.register(
                    "alone_adventure_weapons",
                    () -> CreativeModeTab.builder()

                            .icon(() ->
                                    new ItemStack(
                                            ModItems.POWERSWORD.get()
                                    )
                            )

                            .title(
                                    Component.translatable(
                                            "tab.alone_adventure.weapons"
                                    )
                            )

                            .displayItems(
                                    (itemDisplayParameters, output) -> {

                                        // 武器：动力剑
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.POWERSWORD.get()
                                                )
                                        );

                                        // 武器：突击盾
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.ASSAULT_SHIELD.get()
                                                )
                                        );

                                        // 武器：招架之盾
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.PARRYSHIELD.get()
                                                )
                                        );

                                        // 武器：链锯剑
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CHAINSAW_SWORD.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.DECAPITATION_AXE.get()
                                                )
                                        );
                                        // 武器：痛击之锤
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.PAINSTRIKE_HAMMER.get()
                                                )
                                        );

                                        // 武器：墨制刀刃
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.INK_BLADE.get()
                                                )
                                        );

                                        // 武器：投掷电击器
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHOCK_DEVICE.get()
                                                )
                                        );

                                        // 战术道具：连队团旗
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.REGIMENT_BANNER.get()
                                                )
                                        );

                                        // 武器：星辉大剑
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.STARLIGHT_GREATSWORD.get()
                                                )
                                        );

                                        // 武器：死神镰刀
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.REAPER_SCYTHE.get()
                                                )
                                        );

                                        // 武器：机器爪刃
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.MACHINE_CLAW.get()
                                                )
                                        );

                                        // 武器：火腿大棒
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.HAM_CLUB.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BANRUO_SOUP.get()
                                                )
                                        );

                                        // 枪械
                                        // 步枪已取消注册
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.DOUBLE_BARREL_SHOTGUN.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.PISTOL.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.HEAVY_REVOLVER.get()
                                                )
                                        );

                                        // 装备：女武神头盔
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.VALKYRIE_HELMET.get()
                                                )
                                        );
                                    }
                            )

                            .build()
            );

    // =========================================================
    // 饰品标签
    // =========================================================

    public static final RegistryObject<CreativeModeTab> CURIOS_TAB =
            CREATIVE_MODE_TABS.register(
                    "alone_adventure_curios",
                    () -> CreativeModeTab.builder()

                            .icon(() ->
                                    new ItemStack(
                                            ModItems.CRYSTALLINE_HEART.get()
                                    )
                            )

                            .title(
                                    Component.translatable(
                                            "tab.alone_adventure.curios"
                                    )
                            )

                            .displayItems(
                                    (itemDisplayParameters, output) -> {

                                        // 饰品
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BLEEDINGSHIELD.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CRYSTALLINE_HEART.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.NIGHT_CONTRACT.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SINGLE_ACTION_RAPID_FIRE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SURVIVAL_WHIMPER.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.ADAPTIVE_FLESH.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.DRAGON_POWER.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.IMPERIAL_EAGLE.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BROKEN_MASK.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BINDING_BANDAGE.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SEALED_THRONE.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.NECROMANCER_LEDGER.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CHARGING_CORE.get()
                                                )
                                        );

                                        output.accept(
                                                new ItemStack(
                                                        ModItems.HUNTER_SERUM.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.MAGAZINE_PRESSURE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.ONE_WITH_GUN.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.THIRST_FOR_BLOOD.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CURSE_OF_CORPSE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.WEAPON_MASTER.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHADOW_STEP.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BALD_EAGLE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.GRAND_OPENING.get()
                                                )
                                        );

                                        // ========== 任务契约类物品 (放在饰品最后) ==========
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.RESURRECTION_CONTRACT.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BATTLE_CONTRACT.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.TOWER_CONTRACT.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.IMPERIAL_CONTRACT.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.EVIL_CONTRACT.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.HARVEST_CONTRACT.get()
                                                )
                                        );
                                    }
                            )

                            .build()
            );

    // =========================================================
    // 道具标签
    // =========================================================

    public static final RegistryObject<CreativeModeTab> ITEMS_TAB =
            CREATIVE_MODE_TABS.register(
                    "alone_adventure_items",
                    () -> CreativeModeTab.builder()

                            .icon(() ->
                                    new ItemStack(
                                            ModItems.INJECTION_SYRINGE.get()
                                    )
                            )

                            .title(
                                    Component.translatable(
                                            "tab.alone_adventure.items"
                                    )
                            )

                            .displayItems(
                                    (itemDisplayParameters, output) -> {

                                        // 空针剂
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.INJECTION_EMPTY.get()
                                                )
                                        );

                                        // 药水针剂
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.INJECTION_SYRINGE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.GOLDEN_BEETROOT.get()
                                                )
                                        );
                                        // 怪物残灰
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.MONSTER_ASH.get()
                                                )
                                        );

                                        // 血脉碎片
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.BLOOD_SHARD.get()
                                                )
                                        );
                                        // 弹药箱
                                        output.accept(new ItemStack(ModItems.AMMO_BOX.get()));

                                        // 仇恨之书
                                        output.accept(new ItemStack(ModItems.HATRED_BOOK.get()));

                                        // 挑衅号角
                                        output.accept(new ItemStack(ModItems.PROVOCATION_HORN.get()));

                                        // 通用仇恨符 / 安息符
                                        output.accept(new ItemStack(ModItems.UNIVERSAL_TALISMAN_UP.get()));
                                        output.accept(new ItemStack(ModItems.UNIVERSAL_TALISMAN_DOWN.get()));

                                        // =================================================
                                        // 枪械改装件
                                        // =================================================
                                        output.accept(
                                                new ItemStack(
                                                        ModModificationItems.SINGLE_SHOT_AMPLIFIER.get()
                                                )
                                        );
                                        // 重型枪管
                                        output.accept(
                                                new ItemStack(
                                                        ModModificationItems.HEAVY_BARREL.get()
                                                )
                                        );

                                        // 追踪改件
                                        output.accept(
                                                new ItemStack(
                                                        ModModificationItems.TRACKING_UPGRADE.get()
                                                )
                                        );

                                        // 急救包
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.FIRST_AID_KIT.get()
                                                )
                                        );

                                        // 水手菠菜
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SAILOR_SPINACH.get()
                                                )
                                        );

                                        // 诡异八音盒
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.EERIE_MUSIC_BOX.get()
                                                )
                                        );

                                        // 枪械构件
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.GUN_PART.get()
                                                )
                                        );

                                        // 枪械盒
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.GUN_BOX.get()
                                                )
                                        );
                                        // ========== 派系令牌与退出物品（物品栏）==========
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.FACTION_JOIN_TOKEN.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.FACTION_LEAVE.get()
                                                )
                                        );
                                        // =================================================
                                        // 基础弹药
                                        // =================================================

                                        // 长子弹
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.LONG_BULLET.get()
                                                )
                                        );

                                        // 霰弹
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHOTGUN_SHELL.get()
                                                )
                                        );

                                        // 短子弹
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET.get()
                                                )
                                        );

                                        // 弩箭弹药
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CROSSBOW_BOLT.get()
                                                )
                                        );

                                        // =================================================
                                        // 新增特殊短子弹
                                        // =================================================

                                        // 短子弹-达姆
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET_DUM.get()
                                                )
                                        );

                                        // 短子弹-爆炸
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET_EXPLOSIVE.get()
                                                )
                                        );

                                        // 短子弹-硬币
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET_COIN.get()
                                                )
                                        );

                                        // 短子弹-中毒
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET_POISON.get()
                                                )
                                        );

                                        // 短子弹-穿甲
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHORT_BULLET_ARMOR_PIERCING.get()
                                                )
                                        );

                                        // =================================================
                                        // 长子弹特种弹药
                                        // =================================================

                                        // 长子弹-达姆
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.LONG_BULLET_DUM.get()
                                                )
                                        );

                                        // 长子弹-爆炸
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.LONG_BULLET_EXPLOSIVE.get()
                                                )
                                        );

                                        // 长子弹-中毒
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.LONG_BULLET_POISON.get()
                                                )
                                        );

                                        // 长子弹-穿甲
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.LONG_BULLET_ARMOR_PIERCING.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHOTGUN_SHELL_POISON.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHOTGUN_SHELL_DUM.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.SHOTGUN_SHELL_EXPLOSIVE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CROSSBOW_BOLT_EXPLOSIVE.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CROSSBOW_BOLT_DUM.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CROSSBOW_BOLT_ARMOR_PIERCING.get()
                                                )
                                        );
                                        output.accept(
                                                new ItemStack(
                                                        ModItems.CROSSBOW_BOLT_POISON.get()
                                                )
                                        );

                                        // 派系掉落物（每派系 x 3 等级，I / II / III）
                                        for (RegistryObject<Item> drop
                                                : ModItems.FACTION_DROPS) {

                                            output.accept(
                                                    new ItemStack(
                                                            drop.get()
                                                    )
                                            );
                                        }

                                        // 仇恨符（派系 x 等级 x 增/减）
                                        for (RegistryObject<Item> talisman
                                                : ModItems.FACTION_TALISMANS) {

                                            output.accept(
                                                    new ItemStack(
                                                            talisman.get()
                                                    )
                                            );
                                        }

                                        // 派系生物生成蛋（全部派系 x 职业 x 军衔）
                                        for (RegistryObject<Item> egg
                                                : ModItems.FACTION_SPAWN_EGGS) {

                                            output.accept(
                                                    new ItemStack(
                                                            egg.get()
                                                    )
                                            );
                                        }




                                    }
                            )

                            .build()
            );

    // =========================================================
    // 注册 Creative Mode Tabs
    // =========================================================

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}