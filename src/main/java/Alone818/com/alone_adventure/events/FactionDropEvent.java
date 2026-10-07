package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.entity.faction.FactionMobEntity;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.IFactionMob;
import Alone818.com.alone_adventure.faction.MobTier;
import Alone818.com.alone_adventure.faction.PromotionTier;
import Alone818.com.alone_adventure.init.ModItems;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 派系掉落物 - 掉落结算
 *
 * 被玩家击杀的派系生物按其等级（MobTier）
 * 概率掉落对应派系、对应等级的掉落物：
 *
 * - 1 级生物：50% 掉落 1 个 I 阶掉落物
 * - 2 级生物：65% 掉落 1 个 II 阶掉落物
 * - 3 级生物：80% 掉落 1 个 III 阶掉落物
 *
 * 规则：
 *
 * - 不受抢夺（Looting）附魔加成影响
 * - 掉落与仇恨结算互相独立：
 *   突袭中冻结的只是仇恨，
 *   突袭里的击杀照常掉落掉落物
 *
 * 物品与 8 合 1 配方见 ModItems 派系掉落物区块
 * 与 data/alone_adventure/recipes。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class FactionDropEvent {

    /** 各等级的掉落概率（索引 = 等级 - 1），可按手感调整 */
    public static final float[] DROP_CHANCE =
            {0.5F, 0.65F, 0.8F};

    /** 掉落数量（始终为 1） */
    public static final int DROP_COUNT = 1;

    /**
     * 掉落档位看军衔（整体），不看军衔内小等级：
     *
     * 士兵 / 弓手 / 法师   -> I 阶
     * 中士 / 弩手 / 大法师 -> II 阶
     * 将军 / 连弩手 / 魔导师 -> III 阶
     *
     * 非本模组派系实体（仅分类注册）
     * 退回 FactionManager 的静态分类档位。
     */
    private static MobTier dropTierFor(LivingEntity entity) {
        if (entity instanceof FactionMobEntity mob) {
            return MobTier.values()[mob.getRank().ordinal()];
        }
        return FactionManager.getTier(entity);
    }

    @SubscribeEvent
    public static void onLivingDrops(
            LivingDropsEvent event
    ) {

        LivingEntity victim =
                event.getEntity();

        if (victim.level().isClientSide()) {
            return;
        }

        // 只有被玩家近期攻击致死的生物才构成"击杀"
        if (!event.isRecentlyHit()) {
            return;
        }

        if (!(event.getSource()
                .getEntity() instanceof Player)) {
            return;
        }

        // 只处理派系生物
        Faction faction =
                FactionManager.getFaction(victim);

        if (faction == null) {
            return;
        }

        MobTier tier =
                dropTierFor(victim);

        if (tier == null) {
            return;
        }

        // 对应派系 x 等级的掉落物
        Item drop =
                ModItems.getFactionDrop(
                        faction,
                        tier
                );

        if (drop == null) {
            return;
        }

        // 按生物等级的概率掉落
        float chance =
                DROP_CHANCE[tier.getLevel() - 1];

        if (victim.getRandom()
                .nextFloat() >= chance) {
            return;
        }

        event.getDrops().add(
                new ItemEntity(
                        victim.level(),
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        new ItemStack(
                                drop,
                                DROP_COUNT
                        )
                )
        );
    }
}
