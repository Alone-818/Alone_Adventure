package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 急救包 - 应急治疗道具
 *
 * 右键使用：立即回复最大生命值的 60%，随后进入 120 秒冷却
 * （使用原版物品冷却系统，物品栏显示白色遮罩，同末影珍珠）。
 * 耐久 3 点，每使用一次消耗 1 点，耗尽后损坏；不可堆叠、无法附魔。
 * 生命值全满或冷却中时无法使用（防止浪费）。
 */
public class first_aid_kit extends Item {

    public static final float HEAL_RATIO = 0.6F;        // 回复最大生命值的 60%
    public static final int COOLDOWN_TICKS = 20 * 120;  // 冷却 120 秒
    public static final int MAX_USES = 3;               // 耐久 3 点

    public first_aid_kit() {
        super(new Properties().stacksTo(1).durability(MAX_USES));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack); // 冷却中
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return InteractionResultHolder.fail(stack); // 满血时不浪费耐久
        }
        if (!level.isClientSide) {
            player.heal(player.getMaxHealth() * HEAL_RATIO);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ── 无法附魔：附魔台与附魔书均不可用 ──

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.first_aid_kit.tooltip.desc")
                .withStyle(ChatFormatting.RED));
    }
}
