package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Effects.EnduranceEffect;
import Alone818.com.alone_adventure.Effects.LacerationEffect;
import Alone818.com.alone_adventure.Effects.VulnerabilityEffect;
import Alone818.com.alone_adventure.Effects.CalamityEffect;
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

    // 撕裂：到期结算伤害并把等级减半
    public static final RegistryObject<MobEffect> LACERATION =
            EFFECTS.register("laceration", LacerationEffect::new);

    // 耐力：移动速度、跳跃高度、台阶高度提升，饱食度不消耗
    public static final RegistryObject<MobEffect> ENDURANCE =
            EFFECTS.register("endurance", EnduranceEffect::new);

    // 灾厄：亡灵秘典专属负面效果，等级 = 攻击伤害 × 20（技能后 × 50）
    public static final RegistryObject<MobEffect> CALAMITY =
            EFFECTS.register("calamity", CalamityEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}