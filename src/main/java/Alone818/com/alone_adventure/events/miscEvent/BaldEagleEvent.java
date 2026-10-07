package Alone818.com.alone_adventure.events.miscEvent;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.miscCurios.bald_eagle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 秃鹫 - 掉落结算
 *
 * 被玩家击杀的生物，若击杀者佩戴了秃鹫饰品，则以概率获得 2 倍掉落物：
 * - 基础概率：30%（可由 Config 覆盖）
 * - 效果：击杀结算后，将该生物的所有掉落物品数量翻倍
 * - 可与抢夺（Looting）附魔叠加（抢夺在 Minecraft 原版掉落计算中先结算）
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class BaldEagleEvent {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 秃鹫翻倍概率 */
    public static float DOUBLING_CHANCE = 0.3F;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // 服务端处理
        if (event.getEntity().level().isClientSide()) return;

        // 只有被玩家近期攻击致死的生物才构成"击杀"
        if (!event.isRecentlyHit()) return;

        // 只处理玩家击杀
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // 检查击杀者是否佩戴秃鹫
        if (!bald_eagle.isWearing(player)) return;

        // 按概率触发翻倍
        if (player.getRandom().nextFloat() >= DOUBLING_CHANCE) return;

        // 将所有掉落物数量翻倍
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            stack.setCount(stack.getCount() * 2);
        }
    }
}
