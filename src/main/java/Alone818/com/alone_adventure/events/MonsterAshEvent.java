package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.hunter_serum;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 怪物残灰 - 掉落结算
 *
 * 被玩家击杀的敌对生物（Enemy）概率掉落怪物残灰：
 * - 基础：2% 概率掉落 1 个
 * - 佩戴猎人血清：5% 概率掉落 1 个
 * - 不受抢夺（Looting）附魔加成影响
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class MonsterAshEvent {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 基础掉落概率（击杀敌对生物） */
    public static double BASE_DROP_CHANCE = 0.02;
    /** 佩戴猎人血清时的掉落概率 */
    public static double SERUM_DROP_CHANCE = 0.05;
    /** 掉落数量（始终为 1） */
    public static int DROP_COUNT = 1;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        // 只有被玩家近期攻击致死的生物才构成"击杀"
        if (!event.isRecentlyHit()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        // 任何敌对生物
        if (!(event.getEntity() instanceof Enemy)) return;

        boolean serum = hunter_serum.isWearing(player);
        double chance = serum ? SERUM_DROP_CHANCE : BASE_DROP_CHANCE;
        if (player.getRandom().nextDouble() >= chance) return;

        int count = DROP_COUNT;

        LivingEntity victim = event.getEntity();
        event.getDrops().add(new ItemEntity(victim.level(),
                victim.getX(), victim.getY(), victim.getZ(),
                new ItemStack(ModItems.MONSTER_ASH.get(), count)));
    }
}
