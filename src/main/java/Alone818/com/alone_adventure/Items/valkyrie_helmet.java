package Alone818.com.alone_adventure.Items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 女武神头盔 - 皮甲级头部装备
 *
 * 皮革材质头盔，防御力比皮革头盔高 {@value #EXTRA_DEFENSE}（共 3 点）；
 * 佩戴时造成伤害 +{@value #DAMAGE_BONUS}（ATTACK_DAMAGE 乘算修饰符，
 * 头盔装备面板"佩戴时"属性中显示）。
 * 合成：空/空/空 + 木棍/空/木棍 + 圆石/皮革帽子/圆石
 * （见 data/alone_adventure/recipes/valkyrie_helmet.json）。
 */
public class valkyrie_helmet extends ArmorItem {

    /** 相比皮革头盔的额外防御（默认值，可由 Config 覆盖） */
    public static int EXTRA_DEFENSE = 2;
    /** 佩戴时的伤害加成（0.10 = +10%） */
    public static double DAMAGE_BONUS = 0.10;

    /** 伤害加成修饰符固定 UUID（幂等，装备栏 GUI 可显示） */
    public static final UUID DAMAGE_BONUS_UUID = UUID.fromString("8f0a1b2c-3d4e-4f5a-9a8b-7c6d5e4f3a10");

    public valkyrie_helmet() {
        super(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET,
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** 皮革头盔防御力 +2 */
    @Override
    public int getDefense() {
        return super.getDefense() + EXTRA_DEFENSE;
    }

    /** 佩戴（头部）时造成伤害 +10% */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getDefaultAttributeModifiers(slot);
        if (slot == EquipmentSlot.HEAD) {
            Multimap<Attribute, AttributeModifier> result = HashMultimap.create();
            result.putAll(modifiers);
            result.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    DAMAGE_BONUS_UUID, "Valkyrie Helmet Damage Bonus",
                    DAMAGE_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
            return result;
        }
        return modifiers;
    }

    // ── 提示文本 ──

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.valkyrie_helmet.tooltip.desc")
                .withStyle(ChatFormatting.AQUA));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.alone_adventure.valkyrie_helmet.tooltip.effect_desc")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
