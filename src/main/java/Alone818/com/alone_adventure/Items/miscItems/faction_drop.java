package Alone818.com.alone_adventure.Items.miscItems;

import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.MobTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 派系凋落物 - 击杀派系生物获得的战利品材料
 *
 * 每个派系一种凋落物，各 3 个等级（I / II / III），
 * 对应派系生物的等级（{@link MobTier}）：
 *
 * - 帝国：帝国军衔 I / II / III
 * - 亡灵：亡灵精魄 I / II / III
 * - 恶魔：恶魔鲜血 I / II / III
 * - 部落：部落兽骨 I / II / III
 *
 * 合成关系（8 合 1）：
 * 8 个低阶凋落物环形摆放 -> 1 个高阶凋落物
 * （配方见 data/alone_adventure/recipes）。
 *
 * 12 个物品共用本类，派系与等级在注册时传入
 * （见 ModItems 的派系凋落物区块）。
 */
public class faction_drop extends Item {

    /** 归属派系（tooltip 显示用） */
    private final Faction faction;

    /** 等级（对应掉落生物的等级） */
    private final MobTier tier;

    public faction_drop(
            Faction faction,
            MobTier tier
    ) {

        super(new Properties());

        this.faction = faction;
        this.tier = tier;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        // 来源：哪个派系的哪一级生物
        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.faction_drop.tooltip.desc",
                        faction.getDisplayName(),
                        tier.getDisplayName()
                ).withStyle(ChatFormatting.GRAY)
        );

        // 8 合 1 提示
        tooltip.add(
                Component.translatable(
                        "item.alone_adventure.faction_drop.tooltip.combine"
                ).withStyle(ChatFormatting.AQUA)
        );
    }
}
