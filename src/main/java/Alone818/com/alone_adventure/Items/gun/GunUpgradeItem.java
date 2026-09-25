package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 通用枪械改装件。
 *
 * 改装件分为两类：
 * 1. 属性改装：通过 statModifier 修改枪械实际 GunStats。
 * 2. 行为改装：不修改 GunStats，而是在子弹生成时写入行为标记。
 *
 * 具体改装件只需要注册一个实例即可。
 */
public class GunUpgradeItem extends Item {

    private final UnaryOperator<GunStats> statModifier;
    private final boolean tracking;

    public GunUpgradeItem(
            Properties properties,
            UnaryOperator<GunStats> statModifier,
            boolean tracking
    ) {
        super(properties.stacksTo(1));
        this.statModifier = statModifier;
        this.tracking = tracking;
    }

    /** 应用这个改装件的属性效果。 */
    public GunStats modifyStats(GunStats base) {
        return statModifier.apply(base);
    }

    /** 是否给发射出的子弹启用追踪行为。 */
    public boolean enablesTracking() {
        return tracking;
    }

    // =========================================================
    // Tooltip
    // =========================================================

    /**
     * 改装件的提示文本采用“注册名自动映射”的方式。
     *
     * 例如：
     * heavy_barrel -> tooltip.alone_adventure.heavy_barrel
     * tracking_upgrade -> tooltip.alone_adventure.tracking_upgrade
     *
     * 不再在这里判断具体改装件名称。
     * 以后新增改装件，只需要在 lang 文件增加对应翻译即可。
     */
    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        String prefix = "tooltip.alone_adventure." + path;

        // 第一行：通用标题
        tooltip.add(
                Component.translatable(
                        "tooltip.alone_adventure.gun_modification"
                ).withStyle(ChatFormatting.LIGHT_PURPLE)
        );

        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.alone_adventure.press_shift"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY,
                            ChatFormatting.ITALIC
                    )
            );
            return;
        }

        /*
         * 批量读取改装件说明。
         *
         * 当前 lang 文件支持：
         *   <id>                 主说明
         *   <id>.desc            说明补充
         *   <id>.description     说明补充
         *   <id>.damage          伤害变化
         *   <id>.fire_rate       射击间隔变化
         *   <id>.effect          行为效果
         *   <id>.type            改装类型
         *
         * 以后新增改装件时无需修改 Java，只需添加相应 lang 键即可。
         */
        addIfExists(tooltip, prefix, "", ChatFormatting.GRAY);
        addIfExists(tooltip, prefix, ".desc", ChatFormatting.GRAY);
        addIfExists(tooltip, prefix, ".description", ChatFormatting.GRAY);
        addIfExists(tooltip, prefix, ".damage", ChatFormatting.RED);
        addIfExists(tooltip, prefix, ".fire_rate", ChatFormatting.YELLOW);
        addIfExists(tooltip, prefix, ".effect", ChatFormatting.AQUA);
        addIfExists(tooltip, prefix, ".type", ChatFormatting.GOLD);

        // 通用安装提示
        tooltip.add(
                Component.translatable(
                        "tooltip.alone_adventure.install"
                ).withStyle(ChatFormatting.GREEN)
        );
    }

    /** 只有翻译存在时才显示这一行，避免出现未翻译的 key。 */
    private static void addIfExists(
            List<Component> tooltip,
            String prefix,
            String suffix,
            ChatFormatting formatting
    ) {
        String key = prefix + suffix;

        if (net.minecraft.locale.Language.getInstance().has(key)) {
            tooltip.add(
                    Component.translatable(key)
                            .withStyle(formatting)
            );
        }
    }

}
