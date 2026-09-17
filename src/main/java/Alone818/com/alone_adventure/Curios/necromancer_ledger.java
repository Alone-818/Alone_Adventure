package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.init.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 亡灵秘典 - 契约栏位饰品
 *
 * 特性：
 * - 减少 20% 最大生命值上限
 * - 减少 95% 攻击伤害
 * - 每 60 秒获得 10 点黄心（不可堆叠）
 * - 攻击时施加灾厄等级 1，时长 = 攻击伤害 × 0.8 秒，多次攻击叠加时长
 * - R 技能：周围生物灾厄时长变为 1/5，等级 +3；下次攻击时长 × 2
 * - 灾厄等级 × 灾厄时长 >= 目标生命值时，下次攻击造成 32676 直接伤害
 */
public class necromancer_ledger extends Item implements ICurioItem {

    // ===== 属性修正 =====
    /** 最大生命值减少比例：20% */
    public static final float MAX_HEALTH_REDUCTION = 0.20F;
    /** 攻击伤害减少比例：95% */
    public static final float ATTACK_DAMAGE_REDUCTION = 0.95F;

    // ===== 黄心回复 =====
    /** 每 60 秒获得的黄心点数 */
    public static final double ABSORPTION_AMOUNT = 10.0;
    /** 黄心回复间隔（tick）：60 秒 = 1200 tick */
    public static final int ABSORPTION_INTERVAL_TICKS = 1200;

    // ===== 灾厄 =====
    /** 攻击时基础灾厄时长倍率 */
    public static final int DURATION_BASE_MULTIPLIER = 20;
    /** 攻击时施加的灾厄等级 */
    public static final int CALAMITY_BASE_LEVEL = 1;
    /** 灾厄等级 × 灾厄时长 >= 目标生命值时的直接伤害 */
    public static final float LETHAL_DAMAGE = 32676.0F;

    // ===== 技能 =====
    /** R 技能持续时间（tick）：30 秒 = 600 tick */
    public static final int SKILL_DURATION_TICKS = 600;
    /** R 技能影响半径 */
    public static final double SKILL_RADIUS = 8.0;
    /** R 技能：周围生物灾厄等级额外 +3 */
    public static final int SKILL_LEVEL_BONUS = 3;
    /** R 技能：周围生物灾厄时长变为 1/5 */
    public static final float SKILL_DURATION_RATIO = 0.20F;

    // NBT 键
    public static final String NB_TAG_SKILL_ACTIVE = "NecromancerSkillActive"; // 技能是否活跃
    public static final String NB_TAG_NEXT_ABSORPTION = "NecromancerNextAbs"; // 下一次黄心回复时刻
    public static final String NB_TAG_SKILL_STACKED = "NecromancerSkillStacked"; // 技能叠加标记

    public necromancer_ledger() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    /** 检查玩家是否佩戴亡灵秘典 */
    private static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ForgeRegistries.ITEMS
                        .getValue(new ResourceLocation("alone_adventure", "necromancer_ledger")))
                .isPresent();
    }

    private static ItemStack getStack(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ForgeRegistries.ITEMS
                        .getValue(new ResourceLocation("alone_adventure", "necromancer_ledger")))
                .map(s -> s.stack())
                .orElse(ItemStack.EMPTY);
    }

    public static boolean activateSkill(Player player) {
        if (!isWearing(player)) return false;

        ItemStack stack = getStack(player);
        if (stack.isEmpty()) return false;

        // 检查 8 格范围内是否有生物带有灾厄效果
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(necromancer_ledger.SKILL_RADIUS),
                e -> e != player && e.getEffect(ModEffects.CALAMITY.get()) != null
        );

        if (entities.isEmpty()) return false;

        for (LivingEntity entity : entities) {
            MobEffectInstance effect = entity.getEffect(ModEffects.CALAMITY.get());
            if (effect != null) {
                int newLevel = effect.getAmplifier() + 1 + necromancer_ledger.SKILL_LEVEL_BONUS;
                int newDuration = (int) (effect.getDuration() * necromancer_ledger.SKILL_DURATION_RATIO);
                entity.addEffect(new MobEffectInstance(ModEffects.CALAMITY.get(), newDuration, newLevel - 1, false, true, true));
            }
        }

        // 标记技能活跃并设置下次攻击叠加灾厄时长至 50 倍
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(necromancer_ledger.NB_TAG_SKILL_STACKED, true);
        tag.putBoolean(necromancer_ledger.NB_TAG_SKILL_ACTIVE, true);
        tag.putLong(necromancer_ledger.NB_TAG_NEXT_ABSORPTION,
                player.level().getGameTime() + necromancer_ledger.SKILL_DURATION_TICKS);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(null, -1, stack));

        return true;
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.necromancer_ledger.tooltip.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.necromancer_ledger.tooltip.absorption")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("item.alone_adventure.necromancer_ledger.tooltip.calamity")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.necromancer_ledger.tooltip.skill")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.alone_adventure.necromancer_ledger.tooltip.lethal")
                    .withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
