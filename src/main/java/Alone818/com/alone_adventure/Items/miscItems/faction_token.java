package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.client.screen.FactionTokenScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
 * 派系令牌 - 右键打开 4 选 1 派系选择轮盘
 *
 * 功能：
 * - 右键打开派系选择界面（帝国/亡灵/恶魔/部落）
 * - 点击选择加入对应派系
 * - 使用后消耗（创造模式不消耗）
 * - 只能使用一次，不可重复使用
 *
 * 显示：
 * - 描述文字
 * - 使用说明
 */
public class faction_token extends Item {

    public faction_token() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端打开 UI
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new FactionTokenScreen(stack));
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // 服务端不消耗，等待 UI 确认后由网络包处理
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.faction_token.tooltip.desc"
                ).withStyle(net.minecraft.ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.faction_token.tooltip.use"
                ).withStyle(net.minecraft.ChatFormatting.YELLOW)
        );
    }
}
