package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
 * 投掷电击器 - 投掷型范围电击道具
 *
 * 右键掷出一台放电装置（{@link ShockDeviceProjectile}），出膛参数与原版雪球
 * 完全一致（初速 {@value #THROW_SPEED}、离散 1.0、下坠 0.03）。
 * 掷出后装置立即开始放电：每 {@value ShockDeviceProjectile#SHOCK_INTERVAL_TICKS} tick
 * 对周围 {@value ShockDeviceProjectile#SHOCK_RADIUS} 格内所有生物造成
 * {@value ShockDeviceProjectile#SHOCK_DAMAGE} 点伤害（不伤投掷者）；
 * 命中实体或方块后固定原地继续放电，持续 {@value ShockDeviceProjectile#SHOCK_DURATION_TICKS} tick
 * （5 秒）后烧毁消散。投掷冷却 {@value #COOLDOWN_TICKS} tick，物品可堆叠 16 个。
 */
public class shock_device extends Item {

    // 以下数值为默认值，可由 Config（alone_adventure-common.toml）覆盖
    /** 投掷冷却：0.5 秒（10 tick），防止连续投掷叠加电场 */
    public static int COOLDOWN_TICKS = 10;
    /** 投掷初速（与原版雪球一致） */
    public static float THROW_SPEED = 1.5F;

    public shock_device() {
        super(new Properties().stacksTo(16));
    }

    // ===== 右键投掷 =====

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中（0.5 秒）直接拦截
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            ShockDeviceProjectile device = new ShockDeviceProjectile(
                    ModEntities.SHOCK_DEVICE.get(), player, level, stack);
            // 与原版雪球完全一致的出膛参数（初速 1.5、离散 1.0）
            device.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, THROW_SPEED, 1.0F);
            level.addFreshEntity(device);
            level.playSound(null, player, SoundEvents.SNOWBALL_THROW,
                    SoundSource.NEUTRAL, 0.5F,
                    0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            // 消耗一枚（创造模式不消耗）
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.shock_device.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.alone_adventure.shock_device.tooltip.throw_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.shock_device.tooltip.shock_desc")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure.shock_device.tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
