package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.charging_core;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

/**
 * 充能核心事件处理 —— 玩家造成伤害时的两项加成：
 * - 雷霆形态（佩戴中）：攻击额外造成 层数×5% 伤害
 * - 黑暗形态释放（NBT 记录 CoreDamageBonus）：下一次攻击额外伤害，命中后消耗
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ChargingCoreEvent {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Optional<SlotResult> coreOpt = charging_core.getCurio(player);
        if (coreOpt.isEmpty()) return;

        ItemStack core = coreOpt.get().stack();
        float amount = event.getAmount();

        // 1. 雷霆形态被动：攻击额外造成 层数×THUNDER_DAMAGE_PER_LAYER 伤害
        if (charging_core.getForm(core) == charging_core.Form.THUNDER) {
            int layer = charging_core.getLayer(core);
            if (layer > 0) {
                amount *= 1.0F + layer * charging_core.THUNDER_DAMAGE_PER_LAYER;
            }
        }

        // 2. 黑暗释放的额外伤害：与当前形态无关，下一次攻击命中即消耗
        CompoundTag tag = core.getTag();
        int bonus = tag != null ? tag.getInt(charging_core.NB_TAG_DAMAGE_BONUS) : 0;
        if (bonus > 0) {
            amount += bonus;
            tag.remove(charging_core.NB_TAG_DAMAGE_BONUS);
            core.setTag(tag);
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(coreOpt.get().slotContext().identifier(),
                            coreOpt.get().slotContext().index(), core));
        }

        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }
}
