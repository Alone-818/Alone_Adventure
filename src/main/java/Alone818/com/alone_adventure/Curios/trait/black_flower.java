package Alone818.com.alone_adventure.Curios.trait;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Optional;

/**
 * 黑色花朵 - 特质槽位饰品
 *
 * 描述：
 * - 一朵黑色的花，散发着死亡的芳香
 * - 曾佩戴在某个死神的头上
 *
 * 效果：
 * - 攻击时获得灵魂（内置冷却）
 * - 灵魂层数上限为 7
 * - 失去 (灵魂数量 × 10%) 的生命值
 * - 激活技能：损失 (灵魂点数 × 2) 的生命值，获得力量 (灵魂 - 2) 等级，时长 (灵魂 × 5 秒)
 */
public class black_flower extends Item {

    /** 最大灵魂层数 */
    public static final int MAX_SOUL = 7;
    /** 灵魂消耗比例：10% 生命值 */
    public static final double SOUL_HEALTH_LOSS_PERCENT = 0.10;
    /** 技能生命损失：灵魂点数 × 2 */
    public static final int SKILL_HEALTH_LOSS_PER_SOUL = 2;
    /** 力量等级：灵魂 - 2 */
    public static final int STRENGTH_AMPLIFIER = 0; // Level = amplifier + 1
    /** 力量时长乘数：灵魂 × 5 秒 = 灵魂 × 100 tick */
    public static final int STRENGTH_DURATION_MULTIPLIER = 100;

    /** NBT 键：灵魂层数 */
    public static final String TAG_SOUL_COUNT = "SoulCount";
    /** NBT 键：灵魂获取冷却结束时刻（gameTime） */
    public static final String TAG_SOUL_COOLDOWN_END = "SoulCooldownEnd";

    public black_flower() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.black_flower.tooltip.desc")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.alone_adventure.black_flower.tooltip.soul_desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.alone_adventure.black_flower.tooltip.skill_desc")
                .withStyle(ChatFormatting.YELLOW));
    }

    /**
     * 获取当前灵魂层数
     */
    public static int getSoulCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getInt(TAG_SOUL_COUNT);
    }

    /**
     * R 键触发：消耗灵魂获得力量效果
     * 由 EagleSkillPacket 调用
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.BLACK_FLOWER.get());
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();
        int soulCount = tag.getInt(TAG_SOUL_COUNT);

        // 需要至少 2 层灵魂才能触发
        if (soulCount < 2) {
            return false;
        }

        // 计算技能生命损失：灵魂点数 × 2
        int skillHealthLoss = soulCount * SKILL_HEALTH_LOSS_PER_SOUL;

        // 扣血（直接设置健康值）
        float currentHealth = player.getHealth();
        float newHealth = Math.max(1.0F, currentHealth - skillHealthLoss);
        player.setHealth(newHealth);

        // 计算力量等级和时长
        int strengthLevel = soulCount - 2; // 力量 (灵魂 -2) 等级
        int durationTicks = soulCount * STRENGTH_DURATION_MULTIPLIER; // 灵魂×5 秒 = 灵魂×100 tick

        // 应用力量效果 (DAMAGE_BOOST = 伤害提升)
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                durationTicks,
                strengthLevel,
                false,
                true
        ));

        // 清空灵魂
        tag.putInt(TAG_SOUL_COUNT, 0);
        stack.setTag(tag);

        // 更新 Curios 槽位
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        return true;
    }
}