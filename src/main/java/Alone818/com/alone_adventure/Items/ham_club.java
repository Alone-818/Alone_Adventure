package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 火腿大棒 - 可食用武器
 *
 * 基础伤害 8、攻击速度 0.8、耐久 32。
 * 每次攻击回复 {@value #FOOD_PER_ATTACK} 点饱食度（饥饿值）；
 * 玩家攻击时耐久耗尽后不消失，而是同一槽位原地变为骨头
 * （见 {@link #hurtEnemy}；合成配方见 data/alone_adventure/recipes/ham_club.json）。
 */
public class ham_club extends SwordItem {

    // 基础战斗属性：伤害 8、攻速 0.8、耐久 32（注册期固定，不参与配置）
    public static final int ATTACK_DAMAGE = 8;
    public static final float ATTACK_SPEED = 0.8F;
    public static final int MAX_DURABILITY = 32;

    /** 每次攻击回复的饱食度（点，默认值，可由 Config 覆盖） */
    public static int FOOD_PER_ATTACK = 3;

    public ham_club() {
        // WOOD 层级本身加成 0，参数直接给 8；攻速 0.8 = 基值 4.0 + 修饰符 -3.2
        super(Tiers.WOOD, ATTACK_DAMAGE, ATTACK_SPEED - 4.0F,
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(MAX_DURABILITY));
    }

    /**
     * 攻击命中：
     * 1. 玩家回复 {@value #FOOD_PER_ATTACK} 点饱食度；
     * 2. 玩家攻击手动结算耐久——耗尽后同槽位替换为骨头
     *    （hurtAndBreak 的损毁路径不触发 PlayerDestroyItemEvent，故自管）；
     *    非玩家攻击者走原版耐久损耗。
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide) return true;

        if (attacker instanceof Player player) {
            // 1. 边打边吃：回复饱食度
            player.getFoodData().eat(FOOD_PER_ATTACK, 0.0F);

            // 2. 手动扣耐久：耗尽后原地变为骨头（同一背包槽位）
            int newDamage = stack.getDamageValue() + 1;
            if (newDamage >= stack.getMaxDamage()) {
                int slot = player.getInventory().findSlotMatchingItem(stack);
                if (slot >= 0) {
                    player.getInventory().setItem(slot, new ItemStack(Items.BONE));
                }
                player.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            } else {
                stack.setDamageValue(newDamage);
            }
            return true;
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.ham_club.tooltip.desc")
                .withStyle(ChatFormatting.GOLD));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.ham_club.tooltip.food_desc")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
