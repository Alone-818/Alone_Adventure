package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.hunter_serum;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 猎人血清 - 视野期间穿甲结算
 *
 * 猎人视野激活期间，佩戴者的攻击无视目标 40% 的护甲与 20% 的护甲韧性
 * （比例见 hunter_serum.ARMOR_PEN_RATIO / TOUGHNESS_PEN_RATIO，可由 Config 覆盖）。
 *
 * 实现方式（LivingHurtEvent，护甲结算前）：按原版公式
 * {@link CombatRules#getDamageAfterAbsorb} 分别计算正常护甲减免
 * 与"护甲/韧性按比例下调后"的减免，把事件伤害按两者比值放大——
 * 原版管线完成护甲结算后，最终伤害恰等于无视比例后应受到的伤害。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class HunterSerumEvent {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        LivingEntity target = event.getEntity();
        if (target == player) return;
        if (!hunter_serum.isVisionActive(player)) return;

        float raw = event.getAmount();
        if (raw <= 0) return;

        float armor = target.getArmorValue();
        AttributeInstance toughnessAttr = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        float toughness = toughnessAttr != null ? (float) toughnessAttr.getValue() : 0.0F;
        if (armor <= 0 && toughness <= 0) return;

        // 正常结算 vs 按比例无视护甲/韧性后的结算
        float normal = CombatRules.getDamageAfterAbsorb(raw, armor, toughness);
        float penetrated = CombatRules.getDamageAfterAbsorb(raw,
                armor * (1.0F - hunter_serum.ARMOR_PEN_RATIO),
                toughness * (1.0F - hunter_serum.TOUGHNESS_PEN_RATIO));
        if (normal <= 0) return;

        event.setAmount(raw * (penetrated / normal));
    }
}
