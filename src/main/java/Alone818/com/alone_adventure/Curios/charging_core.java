package Alone818.com.alone_adventure.Curios;

import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 充能核心 - 多形态契约饰品
 *
 * 形态机制（充能 → 切换 → 释放循环）：
 * - 三种形态循环：雷霆 → 冰冻 → 黑暗 → 雷霆，主动技能（默认 R）切换，冷却 5 秒
 * - 只有当前形态在充能；切换时按"离开的形态"释放全部充能并清零，
 *   新形态从 0 层重新充能（各形态上限与每层充能时间不同）
 *
 * 雷霆形态：上限 6 层，每层充能 10 秒
 * - 常态：攻击额外造成 层数×5% 伤害（见 ChargingCoreEvent）
 * - 释放：获得力量（层数+1）等级 15 秒
 *
 * 冰冻形态：上限 10 层，每层充能 15 秒
 * - 常态：+层数 点护甲、+层数/2 点护甲韧性（动态修饰符）
 * - 释放：获得等同层数的黄心（1:1 叠加到现有吸收值上）
 *
 * 黑暗形态：上限 5 层，每层充能 30 秒
 * - 常态：无效果
 * - 释放：下一次攻击额外造成 层数×8 点伤害（与当前形态无关，命中后消耗）
 */
public class charging_core extends Item implements ICurioItem {

    // ===== 主动技能 =====
    /** 形态切换冷却：5 秒 = 100 tick */
    public static int SKILL_COOLDOWN_TICKS = 100;
    /** 雷霆释放的力量持续：15 秒 = 300 tick */
    public static int THUNDER_BURST_TICKS = 300;
    /** 黑暗释放：每层转化成的下次攻击附加伤害 */
    public static int DARK_DAMAGE_PER_LAYER = 8;

    // ===== 常态数值（默认值，可由 Config 覆盖）=====
    /** 雷霆常态：每层攻击伤害加成比例（5%） */
    public static float THUNDER_DAMAGE_PER_LAYER = 0.05F;
    /** 冰冻常态：每层护甲加成（点） */
    public static double FROST_ARMOR_PER_LAYER = 1.0;
    /** 冰冻常态：每层护甲韧性加成（点，默认 0.5 即每 2 层 1 点韧性） */
    public static double FROST_TOUGHNESS_PER_LAYER = 0.5;

    // ===== NBT 键 =====
    public static final String NB_TAG_FORM = "CoreForm";                 // 当前形态 id
    public static final String NB_TAG_LAYER = "CoreLayer";               // 当前形态层数
    public static final String NB_TAG_NEXT_LAYER = "CoreNextLayer";     // 下一层充能时刻
    public static final String NB_TAG_DAMAGE_BONUS = "CoreDamageBonus"; // 黑暗释放的下次攻击附加伤害
    /** NBT 键：技能下次可用时的世界时刻（gameTime） */
    public static final String TAG_SKILL_READY_AT = "CoreSkillReadyAt";

    // 冰冻形态动态属性修饰符（固定 UUID，随层数增删）
    public static final UUID FROST_ARMOR_UUID = UUID.fromString("c3d4e5f6-a7b8-4901-cdef-234567890abc");
    public static final UUID FROST_TOUGHNESS_UUID = UUID.fromString("d4e5f6a7-b8c9-4012-def0-345678901bcd");

    public charging_core() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    // ===== 形态 =====

    public enum Form {
        /** 雷霆形态：上限 6 层，每层充能 10 秒（200 tick） */
        THUNDER(0, 6, 200, "item.alone_adventure.charging_core.form.thunder", ChatFormatting.YELLOW),
        /** 冰冻形态：上限 10 层，每层充能 15 秒（300 tick） */
        FROST(1, 10, 300, "item.alone_adventure.charging_core.form.frost", ChatFormatting.AQUA),
        /** 黑暗形态：上限 5 层，每层充能 30 秒（600 tick） */
        DARK(2, 5, 600, "item.alone_adventure.charging_core.form.dark", ChatFormatting.DARK_PURPLE);

        public final int id;
        /** 形态层数上限 / 每层充能间隔（默认值，可由 Config 覆盖） */
        public int maxLayers;
        public int layerTicks;
        public final String langKey;
        public final ChatFormatting color;

        Form(int id, int maxLayers, int layerTicks, String langKey, ChatFormatting color) {
            this.id = id;
            this.maxLayers = maxLayers;
            this.layerTicks = layerTicks;
            this.langKey = langKey;
            this.color = color;
        }

        public static Form fromId(int id) {
            return switch (id) {
                case 1 -> FROST;
                case 2 -> DARK;
                default -> THUNDER;
            };
        }

        /** 循环中的下一形态：雷霆→冰冻→黑暗→雷霆 */
        public Form next() {
            return fromId(this.id + 1);
        }
    }

    // ── 数据读写 ──

    public static Form getForm(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? Form.THUNDER : Form.fromId(tag.getInt(NB_TAG_FORM));
    }

    /** 当前形态的充能层数（0 ~ 上限） */
    public static int getLayer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return Math.max(0, Math.min(getForm(stack).maxLayers, tag.getInt(NB_TAG_LAYER)));
    }

    /** 技能剩余冷却 tick（0 表示可用） */
    public static long getSkillCooldownRemaining(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SKILL_READY_AT) || level == null) return 0;
        return Math.max(0, tag.getLong(TAG_SKILL_READY_AT) - level.getGameTime());
    }

    /** 查找玩家佩戴的充能核心 */
    public static Optional<SlotResult> getCurio(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.CHARGING_CORE.get());
    }

    // ── 佩戴 tick：充能与冰冻属性 ──

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();

        // 首次佩戴：默认雷霆形态，从 0 层开始充能
        if (!tag.contains(NB_TAG_FORM)) {
            tag.putInt(NB_TAG_FORM, Form.THUNDER.id);
            tag.putInt(NB_TAG_LAYER, 0);
            tag.putLong(NB_TAG_NEXT_LAYER, now + Form.THUNDER.layerTicks);
        }

        Form form = getForm(stack);
        int layer = getLayer(stack);
        boolean layerChanged = false;

        // 充能：到点 +1 层；到达上限后停止计时（层数只在切换形态释放时清零）
        if (layer < form.maxLayers) {
            if (!tag.contains(NB_TAG_NEXT_LAYER)) {
                tag.putLong(NB_TAG_NEXT_LAYER, now + form.layerTicks);
                layerChanged = true;
            } else if (now >= tag.getLong(NB_TAG_NEXT_LAYER)) {
                layer++;
                tag.putInt(NB_TAG_LAYER, layer);
                tag.putLong(NB_TAG_NEXT_LAYER, now + form.layerTicks);
                layerChanged = true;
            }
        }

        // 写回堆栈并同步到 Curios 客户端（tooltip 常驻显示层数）
        if (layerChanged) {
            stack.setTag(tag);
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(slotContext.identifier(), slotContext.index(), stack));
        }

        // 冰冻形态：护甲/韧性修饰符随层数调整；
        // 每 20 tick 兜底刷新一次，覆盖重登录等 transient 修饰符被清除的情况
        if (form == Form.FROST && (layerChanged || player.tickCount % 20 == 0)) {
            refreshFrostAttributes(player, layer);
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // 佩戴瞬间按当前层数恢复冰冻修饰符，避免等到 20 tick 兜底才生效
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()
                && getForm(stack) == Form.FROST) {
            refreshFrostAttributes(player, getLayer(stack));
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        // 移除冰冻修饰符（形态与层数保留在堆栈 NBT 中，重新佩戴可继续使用）
        if (slotContext.entity() instanceof Player player) {
            removeFrostAttributes(player);
        }
    }

    // ── 冰冻形态动态属性 ──

    /**
     * 按冰冻形态层数重设两个 transient 修饰符：
     * +层数×{@link #FROST_ARMOR_PER_LAYER} 护甲、+层数×{@link #FROST_TOUGHNESS_PER_LAYER} 韧性。
     * 修饰符数值与目标一致时跳过写入，避免每 tick 重复操作属性实例；
     * 离开冰冻形态/卸下饰品时由 {@link #removeFrostAttributes} 负责移除。
     */
    public static void refreshFrostAttributes(Player player, int layer) {
        double wantArmor = layer * FROST_ARMOR_PER_LAYER;
        double wantToughness = layer * FROST_TOUGHNESS_PER_LAYER;

        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null && !modifierMatches(armor, FROST_ARMOR_UUID, wantArmor)) {
            armor.removeModifier(FROST_ARMOR_UUID);
            if (wantArmor > 0) {
                armor.addTransientModifier(new AttributeModifier(FROST_ARMOR_UUID,
                        "Charging Core Frost Armor", wantArmor, AttributeModifier.Operation.ADDITION));
            }
        }

        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null && !modifierMatches(toughness, FROST_TOUGHNESS_UUID, wantToughness)) {
            toughness.removeModifier(FROST_TOUGHNESS_UUID);
            if (wantToughness > 0) {
                toughness.addTransientModifier(new AttributeModifier(FROST_TOUGHNESS_UUID,
                        "Charging Core Frost Toughness", wantToughness, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /** 移除冰冻形态的全部修饰符（切换形态/卸下时调用） */
    public static void removeFrostAttributes(Player player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.removeModifier(FROST_ARMOR_UUID);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) toughness.removeModifier(FROST_TOUGHNESS_UUID);
    }

    private static boolean modifierMatches(AttributeInstance attribute, UUID uuid, double want) {
        AttributeModifier mod = attribute.getModifier(uuid);
        return want <= 0 ? mod == null : mod != null && Math.abs(mod.getAmount() - want) < 1.0E-6;
    }

    // ── 主动技能：切换形态 ──

    /**
     * 服务端激活：切换到下一形态（雷霆→冰冻→黑暗→雷霆），冷却 5 秒。
     * 切换时按"离开的形态"释放充能（层数随即清零，新形态从 0 层重新充能）：
     * 雷霆→力量（层数+1）级 15 秒；冰冻→等同层数的黄心（1:1 叠加）；
     * 黑暗→下一次攻击额外 层数×8 点伤害。
     */
    public static boolean activateSkill(ServerPlayer player) {
        Optional<SlotResult> curioOpt = getCurio(player);
        if (curioOpt.isEmpty()) return false;

        SlotResult slotResult = curioOpt.get();
        ItemStack stack = slotResult.stack();
        CompoundTag tag = stack.getOrCreateTag();
        long now = player.level().getGameTime();
        if (tag.contains(TAG_SKILL_READY_AT) && now < tag.getLong(TAG_SKILL_READY_AT)) return false;

        Form from = getForm(stack);
        Form to = from.next();
        int layer = getLayer(stack);

        // 1. 释放离开形态的充能
        switch (from) {
            case THUNDER -> {
                // n 层 → 力量 amplifier n（显示等级 n+1），持续 15 秒
                if (layer > 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                            THUNDER_BURST_TICKS, layer, false, true));
                }
            }
            case FROST -> {
                // 黄心 = 层数，1:1 叠加到现有吸收值（1 心 = 2 点吸收，不随时间消失）
                if (layer > 0) {
                    player.setAbsorptionAmount(player.getAbsorptionAmount() + layer * 2.0F);
                }
                removeFrostAttributes(player);
            }
            case DARK -> {
                // 下次攻击附加 层数×8 点伤害，可与尚未消耗的释放量叠加
                if (layer > 0) {
                    tag.putInt(NB_TAG_DAMAGE_BONUS,
                            tag.getInt(NB_TAG_DAMAGE_BONUS) + layer * DARK_DAMAGE_PER_LAYER);
                }
            }
        }

        // 2. 层数清零并切入新形态，重新开始充能；记录技能冷却
        tag.putInt(NB_TAG_FORM, to.id);
        tag.putInt(NB_TAG_LAYER, 0);
        tag.putLong(NB_TAG_NEXT_LAYER, now + to.layerTicks);
        tag.putLong(TAG_SKILL_READY_AT, now + SKILL_COOLDOWN_TICKS);
        stack.setTag(tag);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.setEquippedCurio(slotResult.slotContext().identifier(),
                        slotResult.slotContext().index(), stack));

        player.level().playSound(null, player, SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.8F, 1.5F);
        return true;
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        Form form = getForm(stack);
        int layer = getLayer(stack);

        // 当前形态与层数常驻显示（NBT 由 Curios 同步到客户端）
        tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.form",
                        Component.translatable(form.langKey))
                .withStyle(form.color));
        tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.layer",
                        layer, form.maxLayers)
                .withStyle(layer >= form.maxLayers ? ChatFormatting.GOLD : ChatFormatting.GRAY));

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.thunder_desc")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.frost_desc")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.dark_desc")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.skill_desc")
                    .withStyle(ChatFormatting.GOLD));

            // 冷却中实时显示剩余秒数（NBT 由 Curios 同步到客户端）
            long remaining = getSkillCooldownRemaining(stack, level);
            if (remaining > 0) {
                tooltip.add(Component.translatable("item.alone_adventure.charging_core.tooltip.cooldown_left",
                                Math.round(remaining / 20.0F))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
