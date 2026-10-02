package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 药水注册
 *
 * 自定义药水：
 * 1. 耐力药水
 * 2. 抗性药水
 *
 * 后续可在 ModBrewing 添加酿造流程。
 */
public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Alone_adventure.MODID);


    /**
     * 耐力药水 —— 耐力 I，持续 4 分钟（4800 tick）
     */
    public static final RegistryObject<Potion> ENDURANCE =
            POTIONS.register("endurance",
                    () -> new Potion("endurance",
                            new MobEffectInstance(
                                    ModEffects.ENDURANCE.get(),
                                    4800
                            )));


    /**
     * 抗性药水 —— 原版抗性 I，持续 4 分钟（4800 tick）
     *
     * 使用原版效果：
     * MobEffects.DAMAGE_RESISTANCE
     */
    public static final RegistryObject<Potion> RESISTANCE =
            POTIONS.register("resistance",
                    () -> new Potion("resistance",
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    4800
                            )));


    /**
     * 强效耐力药水（萤石升级）
     * 耐力 II，持续 2 分钟（2400 tick）
     */
    public static final RegistryObject<Potion> ENDURANCE_STRONG =
            POTIONS.register("endurance_strong",
                    () -> new Potion("endurance_strong",
                            new MobEffectInstance(
                                    ModEffects.ENDURANCE.get(),
                                    2400,
                                    1
                            )));


    /**
     * 延长版耐力药水（红石升级）
     * 耐力 I，持续 8 分钟（9600 tick）
     */
    public static final RegistryObject<Potion> ENDURANCE_LONG =
            POTIONS.register("endurance_long",
                    () -> new Potion("endurance_long",
                            new MobEffectInstance(
                                    ModEffects.ENDURANCE.get(),
                                    9600
                            )));
    /** 强效抗性药水（萤石升级）——抗性 II，持续 2分钟（2400 tick） */
    public static final RegistryObject<Potion> RESISTANCE_STRONG =
            POTIONS.register("resistance_strong",
                    () -> new Potion("resistance_strong",
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    2400,
                                    1
                            )));


    /** 延长版抗性药水（红石升级）——抗性 I，持续 8分钟（9600 tick） */
    public static final RegistryObject<Potion> RESISTANCE_LONG =
            POTIONS.register("resistance_long",
                    () -> new Potion("resistance_long",
                            new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    9600
                            )));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}