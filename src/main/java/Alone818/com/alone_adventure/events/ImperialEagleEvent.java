package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 帝国天鹰 — 常态免疫
 *
 * 佩戴时免疫火焰与岩浆伤害（含火烧、岩浆块、火焰弹等带 IS_FIRE 标签的伤害）。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ImperialEagleEvent {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!isFireRelated(event.getSource())) return;

        // 只有玩家主动佩戴天鹰时生效（用 Curios 查找）
        if (CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.IMPERIAL_EAGLE.get()).isEmpty())
            return;

        event.setCanceled(true);
    }

    /** 伤害是否属于火焰/岩浆类 */
    private static boolean isFireRelated(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.LAVA);
    }
}
