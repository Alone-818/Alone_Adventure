package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alone_adventure.MODID);

    // ===== 武器标签 =====
    public static final RegistryObject<CreativeModeTab> WEAPONS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_weapons",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.POWERSWORD.get()))
                            .title(Component.translatable("tab.alone_adventure.weapons"))
                            .displayItems((itemDisplayParameters, output) -> {
                                // 武器：动力剑
                                output.accept(new ItemStack(ModItems.POWERSWORD.get()));
                                // 武器：招架之盾
                                output.accept(new ItemStack(ModItems.PARRYSHIELD.get()));
                                // 武器：链锯剑
                                output.accept(new ItemStack(ModItems.CHAINSAW_SWORD.get()));
                                // 武器：痛击之锤
                                output.accept(new ItemStack(ModItems.PAINSTRIKE_HAMMER.get()));
                                // 武器：墨制刀刃
                                output.accept(new ItemStack(ModItems.INK_BLADE.get()));
                                // 武器：投掷电击器
                                output.accept(new ItemStack(ModItems.SHOCK_DEVICE.get()));
                                // 战术道具：连队团旗
                                output.accept(new ItemStack(ModItems.REGIMENT_BANNER.get()));
                                // 武器：星辉大剑
                                output.accept(new ItemStack(ModItems.STARLIGHT_GREATSWORD.get()));
                                //武器：死神镰刀
                                output.accept(new ItemStack(ModItems.REAPER_SCYTHE.get()));
                                //武器：机器爪刃
                                output.accept(new ItemStack(ModItems.MACHINE_CLAW.get()));
                                //武器：火腿大棒
                                output.accept(new ItemStack(ModItems.HAM_CLUB.get()));
                                // 枪械：步枪 / 霰弹枪 / 手枪
                                output.accept(new ItemStack(ModItems.RIFLE.get()));
                                output.accept(new ItemStack(ModItems.SHOTGUN.get()));
                                output.accept(new ItemStack(ModItems.PISTOL.get()));
                                //装备：女武神头盔
                                output.accept(new ItemStack(ModItems.VALKYRIE_HELMET.get()));
                            })

                            .build());

    // ===== 饰品标签 =====
    public static final RegistryObject<CreativeModeTab> CURIOS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_curios",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.CRYSTALLINE_HEART.get()))
                            .title(Component.translatable("tab.alone_adventure.curios"))
                            .displayItems((itemDisplayParameters, output) -> {
                                // 饰品
                                output.accept(new ItemStack(ModItems.BLEEDINGSHIELD.get()));
                                output.accept(new ItemStack(ModItems.CRYSTALLINE_HEART.get()));
                                output.accept(new ItemStack(ModItems.NIGHT_CONTRACT.get()));
                                output.accept(new ItemStack(ModItems.SURVIVAL_WHIMPER.get()));
                                output.accept(new ItemStack(ModItems.ADAPTIVE_FLESH.get()));
                                output.accept(new ItemStack(ModItems.DRAGON_POWER.get()));
                                output.accept(new ItemStack(ModItems.IMPERIAL_EAGLE.get()));
                                output.accept(new ItemStack(ModItems.BROKEN_MASK.get()));
                                output.accept(new ItemStack(ModItems.BINDING_BANDAGE.get()));
                                output.accept(new ItemStack(ModItems.SEALED_THRONE.get()));
                                output.accept(new ItemStack(ModItems.NECROMANCER_LEDGER.get()));
                                output.accept(new ItemStack(ModItems.CHARGING_CORE.get()));
                                output.accept(new ItemStack(ModItems.HUNTER_SERUM.get()));
                                // 任务物品（模板实例）：完成全部任务后可作为合成材料
                                output.accept(new ItemStack(ModItems.QUEST_CONTRACT.get()));
                            })
                            .build());

    // ===== 道具标签 =====
    public static final RegistryObject<CreativeModeTab> ITEMS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_items",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.INJECTION_SYRINGE.get()))
                            .title(Component.translatable("tab.alone_adventure.items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept((new ItemStack(ModItems.INJECTION_EMPTY.get())));
                                output.accept((new ItemStack(ModItems.INJECTION_SYRINGE.get())));
                                output.accept((new ItemStack(ModItems.MONSTER_ASH.get())));
                                output.accept((new ItemStack(ModItems.BLOOD_SHARD.get())));
                                output.accept((new ItemStack(ModItems.FIRST_AID_KIT.get())));
                                // 食物：水手菠菜
                                output.accept((new ItemStack(ModItems.SAILOR_SPINACH.get())));
                                // 诅咒道具：诡异八音盒
                                output.accept((new ItemStack(ModItems.EERIE_MUSIC_BOX.get())));
                                // 弹药：长子弹 / 霰弹 / 短子弹 / 弩箭弹药
                                output.accept((new ItemStack(ModItems.LONG_BULLET.get())));
                                output.accept((new ItemStack(ModItems.SHOTGUN_SHELL.get())));
                                output.accept((new ItemStack(ModItems.SHORT_BULLET.get())));
                                output.accept((new ItemStack(ModItems.CROSSBOW_BOLT.get())));
                            })
                            .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}