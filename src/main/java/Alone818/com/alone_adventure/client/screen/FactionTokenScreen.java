package Alone818.com.alone_adventure.client.screen;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.faction.Faction;
import Alone818.com.alone_adventure.faction.ModFactions;
import Alone818.com.alone_adventure.faction.RaidManager;
import Alone818.com.alone_adventure.network.FactionJoinPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;

/**
 * 派系令牌选择界面 - 轮盘式设计
 *
 * 显示 4 个派系图标在圆环上，点击选择加入。
 * 参考 AmmoWheelScreen 的轮盘设计。
 */
public class FactionTokenScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ItemStack tokenStack;

    /** 圆环半径 */
    private static final double RING_RADIUS = 56.0D;

    /** 图标距离中心距离 */
    private static final double ICON_RADIUS = 42.0D;

    /** 中心死区半径 */
    private static final double INNER_RADIUS = 18.0D;

    /** 按钮尺寸 */
    private static final int BUTTON_SIZE = 24;

    /** 当前选中的派系索引 */
    private int selectedIndex = -1;

    /** 派系图标材料 */
    private static final ItemStack[] FACTION_ICONS = {
            new ItemStack(Items.YELLOW_BANNER),   // 帝国
            new ItemStack(Items.BLUE_BANNER),     // 亡灵
            new ItemStack(Items.RED_BANNER),      // 恶魔
            new ItemStack(Items.GREEN_BANNER)     // 部落
    };

    public FactionTokenScreen(ItemStack tokenStack) {
        super(Component.translatable("item.alone_adventure.faction_token.tooltip.desc"));
        this.tokenStack = tokenStack;
    }

    @Override
    public void tick() {
        updateSelection();
    }

    /**
     * 根据鼠标位置更新选中的派系。
     */
    private void updateSelection() {
        Minecraft mc = Minecraft.getInstance();

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        double centerX = width / 2.0D;
        double centerY = height / 2.0D;

        double mouseX =
                mc.mouseHandler.xpos()
                        * width
                        / mc.getWindow().getScreenWidth();

        double mouseY =
                mc.mouseHandler.ypos()
                        * height
                        / mc.getWindow().getScreenHeight();

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        double distanceSquared = dx * dx + dy * dy;

        // 中心死区
        if (distanceSquared < INNER_RADIUS * INNER_RADIUS) {
            selectedIndex = -1;
            return;
        }

        // 超出外圈
        if (distanceSquared > RING_RADIUS * RING_RADIUS) {
            selectedIndex = -1;
            return;
        }

        // 计算扇区
        int factionCount = ModFactions.all().length;
        double section = Math.PI * 2.0D / factionCount;

        // 0 号派系位于正上方，顺时针排列
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;

        // 将角度规范化到 [0, 2π)
        angle = angle % (Math.PI * 2.0D);
        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }

        selectedIndex =
                (int) Math.floor((angle + section / 2.0D) / section)
                        % factionCount;
    }

    /**
     * 确认选择并加入派系
     */
    private void confirmJoin() {
        if (selectedIndex < 0
                || selectedIndex >= ModFactions.all().length) {
            return;
        }

        Faction faction = ModFactions.all()[selectedIndex];

        Alone_adventure.NETWORK.sendToServer(
                new FactionJoinPacket(faction.getId())
        );

        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void init() {
        super.init();

        // 取消按钮 - 关闭界面，不加入
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 取消按钮
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("✕"),
                        btn -> Minecraft.getInstance().setScreen(null))
                        .pos(centerX - 12, centerY - 12)
                        .size(24, 24)
                        .build()
        );

        // 确认按钮（当选中时可用）
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("✓"),
                        btn -> confirmJoin())
                        .pos(centerX - 12, centerY + (int)RING_RADIUS + 10)
                        .size(24, 24)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 半透明暗背景
        int bgSize = 140;
        gui.fill(
                centerX - bgSize / 2,
                centerY - bgSize / 2,
                centerX + bgSize / 2,
                centerY + bgSize / 2,
                0x90000000
        );

        // 绘制圆环
        drawRing(gui, centerX, centerY, RING_RADIUS, 0x90FFFFFF);

        // 绘制派系图标
        int factionCount = ModFactions.all().length;
        double angleStep = Math.PI * 2.0D / factionCount;

        for (int i = 0; i < factionCount; i++) {
            double angle = -Math.PI / 2.0D + i * angleStep;

            int x = centerX + (int) Math.round(Math.cos(angle) * ICON_RADIUS);
            int y = centerY + (int) Math.round(Math.sin(angle) * ICON_RADIUS);

            boolean selected = i == selectedIndex;

            // 选中时绘制高亮背景
            if (selected) {
                int highlightSize = 28;
                gui.fill(
                        x - highlightSize / 2,
                        y - highlightSize / 2,
                        x + highlightSize / 2,
                        y + highlightSize / 2,
                        0x66FFFFFF
                );
                gui.renderItem(
                        FACTION_ICONS[i],
                        x - 8,
                        y - 8
                );
            } else {
                // 未选中绘制半透明
                gui.renderItem(
                        FACTION_ICONS[i],
                        x - 8,
                        y - 8,
                        1,
                        0
                );
            }

            // 绘制派系名称
            gui.drawCenteredString(
                    this.font,
                    ModFactions.all()[i].getDisplayName(),
                    x,
                    y + 14,
                    selected
                            ? 0xFFFFFF
                            : 0xAAAAAA
            );
        }

        // 绘制标题
        gui.drawCenteredString(
                this.font,
                Component.translatable("item.alone_adventure.faction_token.tooltip.desc")
                        .withStyle(ChatFormatting.GOLD),
                centerX,
                centerY - bgSize / 2 + 15,
                0xFFFFFF
        );

        // 绘制当前选择的派系名称
        if (selectedIndex >= 0
                && selectedIndex < factionCount) {
            Faction selected = ModFactions.all()[selectedIndex];
            gui.drawCenteredString(
                    this.font,
                    Component.literal("Selected: ")
                            .append(selected.getDisplayName()),
                    centerX,
                    centerY + bgSize / 2 - 15,
                    selected.getColor()
            );
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    /**
     * 绘制圆环
     */
    private void drawRing(
            GuiGraphics gui,
            int centerX,
            int centerY,
            double radius,
            int color
    ) {
        final int points = 64;

        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;

            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);

            gui.fill(x, y, x + 1, y + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedIndex >= 0
                && selectedIndex < ModFactions.all().length) {
            confirmJoin();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
