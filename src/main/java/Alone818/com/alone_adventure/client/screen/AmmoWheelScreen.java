package Alone818.com.alone_adventure.client.screen;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.network.GunSelectAmmoPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 枪械弹药轮盘 HUD 控制器。
 *
 * 注意：这个类虽然继承 Screen，但不会通过 Minecraft#setScreen() 打开。
 * ClientGunHandler 会直接调用 tick() / renderWheel()。
 *
 * 轮盘显示规则：
 * 1. 显示枪械支持的全部弹药类型。
 * 2. 是否存在于背包只影响数量显示，不影响弹药是否显示。
 * 3. 背包没有该弹药时，数量显示 0，图标仍然显示。
 * 4. 鼠标只有进入有效圆环后才会改变 selectedIndex。
 * 5. 鼠标位于中心死区或圆环外时，selectedIndex = -1。
 */
public class AmmoWheelScreen extends Screen {

    private final ItemStack gunStack;
    private final GunItem gun;
    private final int ammoCount;

    /** -1 表示当前鼠标没有指向有效圆环。 */
    private int selectedIndex = -1;

    // =========================================================
    // 轮盘尺寸
    // =========================================================

    /** 中心死区半径。 */
    private static final double INNER_RADIUS = 18.0D;

    /** 有效检测圆环外半径。 */
    private static final double OUTER_RADIUS = 56.0D;

    /** 弹药图标距离中心的距离。 */
    private static final double ICON_RADIUS = 42.0D;

    /** 轮环显示半径。 */
    private static final double RING_RADIUS = 52.0D;

    public AmmoWheelScreen(ItemStack stack) {
        super(Component.empty());

        if (!(stack.getItem() instanceof GunItem)) {
            throw new IllegalArgumentException("AmmoWheelScreen requires a GunItem ItemStack");
        }

        this.gunStack = stack.copy();
        this.gun = (GunItem) stack.getItem();

        // 这里使用枪械自身支持的全部弹药类型。
        // 不检查背包，所以即使数量为 0，也一定会出现在轮盘中。
        this.ammoCount = Math.max(
                1,
                gun.getStats(this.gunStack).ammoTypeCount()
        );

        int currentIndex = gun.getSelectedAmmoIndex(this.gunStack);

        this.selectedIndex =
                currentIndex >= 0 && currentIndex < ammoCount
                        ? currentIndex
                        : -1;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    public void tick() {
        updateSelection();
    }

    /**
     * 根据鼠标位置选择弹药。
     *
     * 只有：
     *      INNER_RADIUS <= 鼠标距离 <= OUTER_RADIUS
     * 才会选择弹药。
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

        // 中心死区：不选择任何弹药。
        if (distanceSquared < INNER_RADIUS * INNER_RADIUS) {
            selectedIndex = -1;
            return;
        }

        // 外圈之外：不选择任何弹药。
        if (distanceSquared > OUTER_RADIUS * OUTER_RADIUS) {
            selectedIndex = -1;
            return;
        }

        double section = Math.PI * 2.0D / ammoCount;

        // 第 0 种弹药位于正上方，然后顺时针均匀排列。
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;

        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }

        angle %= Math.PI * 2.0D;

        int index =
                (int) Math.floor(
                        (angle + section / 2.0D) / section
                ) % ammoCount;

        selectedIndex = index;
    }

    /**
     * 绘制轮盘 HUD。
     * 不调用 Screen 的 GUI 输入逻辑，因此不会阻断 WASD。
     */
    public void renderWheel(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int centerX = width / 2;
        int centerY = height / 2;

        // =====================================================
        // 小型中心背景
        // =====================================================

        gui.fill(
                centerX - 13,
                centerY - 13,
                centerX + 13,
                centerY + 13,
                0x55000000
        );

        // =====================================================
        // 圆环
        // =====================================================

        drawRing(
                gui,
                centerX,
                centerY,
                RING_RADIUS,
                0x90FFFFFF
        );

        // =====================================================
        // 全部弹药类型
        // =====================================================

        double angleStep =
                Math.PI * 2.0D / ammoCount;

        for (int i = 0; i < ammoCount; i++) {

            double angle =
                    -Math.PI / 2.0D
                            + i * angleStep;

            int x =
                    centerX
                            + (int) Math.round(
                            Math.cos(angle) * ICON_RADIUS
                    );

            int y =
                    centerY
                            + (int) Math.round(
                            Math.sin(angle) * ICON_RADIUS
                    );

            boolean selected =
                    i == selectedIndex;

            // 选中背景
            if (selected) {
                gui.fill(
                        x - 12,
                        y - 12,
                        x + 12,
                        y + 12,
                        0xCCFFFFFF
                );
            }

            // -------------------------------------------------
            // 非常重要：这里永远绘制 ammoType(i)
            // 不检查背包数量，所以没有弹药时仍然显示。
            // -------------------------------------------------

            Item ammoItem =
                    gun.getStats(gunStack)
                            .ammoType(i);

            ItemStack ammoStack =
                    new ItemStack(ammoItem);

            gui.renderItem(
                    ammoStack,
                    x - 8,
                    y - 8
            );

            // -------------------------------------------------
            // 数量：没有就是 0
            // -------------------------------------------------

            int count =
                    countAmmo(ammoItem);

            gui.drawCenteredString(
                    mc.font,
                    String.valueOf(count),
                    x,
                    y + 9,
                    count > 0
                            ? 0xFFFFFF
                            : 0xFF5555
            );
        }

        // =====================================================
        // 中心显示当前选择
        // =====================================================

        if (selectedIndex >= 0
                && selectedIndex < ammoCount) {

            Item selectedAmmo =
                    gun.getStats(gunStack)
                            .ammoType(selectedIndex);

            int selectedCount =
                    countAmmo(selectedAmmo);

            gui.drawCenteredString(
                    mc.font,
                    selectedAmmo.getDescription(),
                    centerX,
                    centerY + 16,
                    0xFFFFFF
            );

            gui.drawCenteredString(
                    mc.font,
                    "x" + selectedCount,
                    centerX,
                    centerY + 27,
                    selectedCount > 0
                            ? 0xFFFFFF
                            : 0xFF5555
            );
        }
    }

    private void drawRing(
            GuiGraphics gui,
            int centerX,
            int centerY,
            double radius,
            int color
    ) {
        final int points = 64;

        for (int i = 0; i < points; i++) {

            double angle =
                    Math.PI * 2.0D * i / points;

            int x =
                    centerX
                            + (int) Math.round(
                            Math.cos(angle) * radius
                    );

            int y =
                    centerY
                            + (int) Math.round(
                            Math.sin(angle) * radius
                    );

            gui.fill(
                    x,
                    y,
                    x + 1,
                    y + 1,
                    color
            );
        }
    }

    /**
     * 统计背包中的弹药数量。
     *
     * 注意：这个函数只负责显示数量，
     * 不参与决定弹药是否显示。
     */
    private int countAmmo(Item ammoItem) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return 0;
        }

        int count = 0;

        for (int i = 0;
             i < mc.player.getInventory().getContainerSize();
             i++) {

            ItemStack stack =
                    mc.player.getInventory().getItem(i);

            if (stack.is(ammoItem)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * G 松开时确认选择。
     *
     * 中心死区 / 圆环外 selectedIndex = -1，
     * 此时不发送切换请求。
     */
    public void confirm() {
        if (selectedIndex < 0
                || selectedIndex >= ammoCount) {
            return;
        }

        Alone_adventure.NETWORK.sendToServer(
                new GunSelectAmmoPacket(selectedIndex)
        );
    }

    /** 兼容旧调用。实际轮盘不会通过 setScreen() 使用。 */
    @Override
    public void render(
            GuiGraphics gui,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderWheel(gui);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
