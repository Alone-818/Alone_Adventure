package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.FactionManager;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * 派系加入/退出物品 - 右键选择加入或退出派系
 *
 * 合成配方：
 * - 加入派系物品：中间下界之星，四个角钻石，上下左右合金碎片
 * - 退出派系物品：中间下界之星，四个角合金碎片，上下左右地狱疣
 *
 * 功能：
 * - 加入派系后：玩家无法对该派系生物造成伤害
 * - 加入派系后：同派系士兵在玩家被攻击时主动反击
 * - 退出派系：清除当前派系归属
 *
 * 右键时会列出当前可用的派系供选择加入，
 * 当前已加入的派系会高亮显示。
 */
public class faction_join_item extends Item {

    /** 加入模式（true）还是退出模式（false） */
    private final boolean joinMode;

    public faction_join_item(boolean joinMode) {
        super(new Properties().stacksTo(1));
        this.joinMode = joinMode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(
                    player.getItemInHand(hand),
                    true
            );
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack stack = player.getItemInHand(hand);

        if (joinMode) {
            // 加入派系模式 - 直接加入第一个可用的派系
            Faction current = RaidManager.getPlayerFaction(player.getUUID());
            boolean joined = false;

            for (Faction faction : ModFactions.all()) {
                // 跳过已加入的派系
                if (faction == current) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable(
                                    "faction.alone_adventure.cannot_join_already_member",
                                    faction.getDisplayName()
                            ).withStyle(ChatFormatting.RED)
                    );
                    joined = true; // 标记为已处理
                    break;
                }

                // 加入此派系（包括恶魔/亡灵）- joinFaction 会检查敌意
                if (RaidManager.joinFaction(serverPlayer, faction)) {
                    joined = true;
                    break; // 一次只加入一个派系
                }
            }

            if (!joined) {
                if (current == null) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable(
                                    "faction.alone_adventure.no_joinable"
                            ).withStyle(ChatFormatting.RED)
                    );
                }
            }

        } else {
            // 退出派系模式
            RaidManager.leaveFaction(serverPlayer);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        if (joinMode) {
            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.faction_join.tooltip.join"
                    ).withStyle(ChatFormatting.GOLD)
            );
        } else {
            tooltip.add(
                    Component.translatable(
                            "item.alone_adventure.faction_join.tooltip.leave"
                    ).withStyle(ChatFormatting.RED)
            );
        }

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.faction_join.tooltip.effect"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
