package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Effects.VulnerabilityEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Alone_adventure.MODID);

    // 易伤：每次受到伤害额外 +等级×25%，伤害结算后等级 -1
    public static final RegistryObject<MobEffect> VULNERABILITY =
            EFFECTS.register("vulnerability", VulnerabilityEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}