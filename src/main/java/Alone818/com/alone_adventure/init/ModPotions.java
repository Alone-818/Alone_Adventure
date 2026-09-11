package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 耐力药水注册
 *
 * 金甜菜根（8 个金粒围绕一个甜菜根合成）作为酿造原料，
 * 酿造台：水瓶 + 金甜菜根 → 耐力药水。
 * 另支持原版升级路线：萤石 → 延长版，红石 → 强效版。
 */
public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Alone_adventure.MODID);

    /** 耐力药水 —— 耐力 I，持续 4 分钟（4800 tick） */
    public static final RegistryObject<Potion> ENDURANCE =
            POTIONS.register("endurance",
                    () -> new Potion("endurance",
                            new MobEffectInstance(ModEffects.ENDURANCE.get(), 4800)));

    /** 强效耐力药水（萤石升级）—— 耐力 II，持续 2 分钟（2400 tick） */
    public static final RegistryObject<Potion> ENDURANCE_STRONG =
            POTIONS.register("endurance_strong",
                    () -> new Potion("endurance_strong",
                            new MobEffectInstance(ModEffects.ENDURANCE.get(), 2400, 1)));

    /** 延长版耐力药水（红石升级）—— 耐力 I，持续 8 分钟（9600 tick） */
    public static final RegistryObject<Potion> ENDURANCE_LONG =
            POTIONS.register("endurance_long",
                    () -> new Potion("endurance_long",
                            new MobEffectInstance(ModEffects.ENDURANCE.get(), 9600)));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
