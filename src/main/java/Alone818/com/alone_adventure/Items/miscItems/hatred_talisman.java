package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.MobTier;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 仇恨符 - 人工干预派系仇恨值的道具
 *
 * 每个派系各 3 个等级（I / II / III），
 * 等级由制作材料决定（对应派系对应等级的掉落物，
 * 见 data/alone_adventure/recipes）。
 *
 * 每个派系 x 等级有增仇 / 减仇两种：
 *
 * - 增仇恨符：让该派系更恨你，推进巡逻与突袭
 * - 减仇恨符：平息该派系的敌意，脱离敌对与突袭
 *
 * 仇恨增减幅度（绝对值）：
 *
 * - I 级：50（从 0 起步时刚好够到敌对线 200 的门槛量级）
 * - II 级：150
 * - III 级：350（一次可从突袭临界 400 打回中立）
 *
 * 约束（全部走 RaidManager 的既定规则）：

 * - 仇恨值夹在 0 ~ 上限（1000）之间
 * - 该玩家有突袭进行中时仇恨冻结，
 *   符仍然消耗但仇恨不变，并给出提示
 * - 增到敌对线（200）以上会让中立派系
 *   把玩家当敌人，见 hostility
 *
 * 24 个物品共用本类，
 * 派系、等级、方向在注册时传入
 * （见 ModItems 的仇恨符区块）。
 */
public class hatred_talisman extends Item {

    /** 归属派系 */
    private final Faction faction;

    /** 等级（对应制作材料的掉落物等级） */
    private final MobTier tier;

    /** 增仇（true）还是减仇（false） */
    private final boolean increase;

    public hatred_talisman(
            Faction faction,
            MobTier tier,
            boolean increase
    ) {

        super(
                new Properties()
                        .stacksTo(16)
        );

        this.faction = faction;
        this.tier = tier;
        this.increase = increase;
    }

    /**
     * 本级符的仇恨增减绝对值。
     */
    private int amount() {

        return switch (tier) {

            case TIER_1 -> 50;
            case TIER_2 -> 150;
            default -> 350;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack =
                player.getItemInHand(hand);

        if (level.isClientSide) {

            return InteractionResultHolder.sidedSuccess(
                    stack,
                    true
            );
        }

        ServerPlayer serverPlayer =
                (ServerPlayer) player;

        // 突袭进行中：仇恨冻结，符不生效
        if (RaidManager.getActiveRaidFaction(
                serverPlayer.getUUID()
        ) != null) {

            serverPlayer.sendSystemMessage(
                    Component.translatable(
                            "talisman.alone_adventure.frozen"
                    )
            );

            return consume(
                    serverPlayer,
                    stack
            );
        }

        int amount =
                increase
                        ? amount()
                        : -amount();

        RaidManager.addHatred(
                serverPlayer.getUUID(),
                faction,
                amount
        );

        int hatred =
                RaidManager.getHatred(
                        serverPlayer.getUUID(),
                        faction
                );

        serverPlayer.sendSystemMessage(
                Component.translatable(
                        increase
                                ? "talisman.alone_adventure.raised"
                                : "talisman.alone_adventure.lowered",
                        faction.getDisplayName(),
                        hatred,
                        RaidManager.HATRED_CAP
                )
        );

        level.playSound(
                null,
                serverPlayer.blockPosition(),
                increase
                        ? SoundEvents.RAID_HORN.value()
                        : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                increase ? 0.8F : 1.0F,
                increase ? 0.7F : 1.4F
        );

        return consume(
                serverPlayer,
                stack
        );
    }

    /**
     * 消耗一个（创造模式不消耗）。
     */
    private static InteractionResultHolder<ItemStack> consume(
            ServerPlayer player,
            ItemStack stack
    ) {

        if (!player.getAbilities()
                .instabuild) {

            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                false
        );
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_talisman.tooltip.desc",
                        faction.getDisplayName(),
                        tier.getDisplayName()
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        increase
                                ? "item.alone_adventure.hatred_talisman.tooltip.up"
                                : "item.alone_adventure.hatred_talisman.tooltip.down",
                        amount()
                ).withStyle(
                        increase
                                ? ChatFormatting.RED
                                : ChatFormatting.AQUA
                )
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_talisman.tooltip.use"
                ).withStyle(ChatFormatting.YELLOW)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.hatred_talisman.tooltip.freeze"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
