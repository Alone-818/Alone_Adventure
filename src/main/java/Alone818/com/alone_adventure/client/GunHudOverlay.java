package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * 枪械弹药 HUD —— 在物品栏右侧与屏幕右边界之间、靠右边界 1/3 处
 * 显示每把持枪的弹药状态：[主/副][子弹类型图标]当前弹药/弹夹容量。
 *
 * 位置：物品栏右缘 = screenWidth/2 + 91，锚点 = 屏幕右边界向左 gap/3
 * （gap = 屏幕宽 - 物品栏右缘），图标以锚点为中心向两侧排布。
 * 单持枪（主或副）只显示一行、不标注；主副手双持时上下两行，
 * 主手与物品栏同行，副手在其上方，并标注 主/副。
 * 弹药数据读自枪械 NBT（持枪物品栏变化由原版容器同步到客户端）；
 * 装填中数字显示为黄色，弹尽显示为红色。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GunHudOverlay {

    /** 文字颜色 */
    private static final int COLOR_LABEL = 0xFFD0D0D0;   // 主/副标签灰
    private static final int COLOR_AMMO = 0xFFFFFFFF;    // 弹药白
    private static final int COLOR_EMPTY = 0xFFFF5555;  // 弹尽红
    private static final int COLOR_RELOADING = 0xFFFFFF55; // 装填黄

    /** 图标边长 */
    private static final int ICON_SIZE = 16;
    /** 双持时两行的行距 */
    private static final int ROW_GAP = 14;

    private GunHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiOverlayEvent.Post event) {
        // 只挂在原版物品栏渲染完成之后
        if (event.getOverlay().id() != VanillaGuiOverlay.HOTBAR.id()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;
        // 创造/旁观不显示弹药
        GameType gameType = mc.gameMode.getPlayerMode();
        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainGun = main.getItem() instanceof GunItem;
        // 双手枪在副手不发挥作用，不显示其弹药行
        boolean offGun = off.getItem() instanceof GunItem offGunItem
                && GunItem.isUsableInHand(offGunItem, InteractionHand.OFF_HAND);
        if (!mainGun && !offGun) return;
        boolean dual = mainGun && offGun;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 锚点：物品栏右缘与屏幕右边界之间、靠右边界 1/3 处
        int hotbarRight = screenWidth / 2 + 91;
        int gap = screenWidth - hotbarRight;
        int anchorX = screenWidth - gap / 3;

        // 主手与物品栏同行，副手在其上方
        int mainY = screenHeight - 20;
        int offY = mainY - ROW_GAP;

        if (mainGun) {
            renderGunRow(gui, font, main, dual ? "主" : null, anchorX, mainY);
        }
        if (offGun) {
            renderGunRow(gui, font, off, dual ? "副" : null, anchorX, offY);
        }
    }

    /**
     * 渲染一行弹药状态：[标签][子弹图标]当前弹药/弹夹容量。
     * 图标以锚点为中心，标签排在图标左侧、数量文本排在图标右侧。
     */
    private static void renderGunRow(GuiGraphics gui, Font font, ItemStack gunStack,
                                     @Nullable String label, int anchorX, int y) {
        GunItem gun = (GunItem) gunStack.getItem();
        GunStats stats = gun.getStats();
        CompoundTag tag = gunStack.getTag();
        int ammo = tag != null ? tag.getInt(GunItem.TAG_AMMO) : 0;
        boolean reloading = tag != null && tag.contains(GunItem.TAG_RELOAD_START);

        int iconX = anchorX - ICON_SIZE / 2;

        // 标签（主/副）：排在图标左侧
        if (label != null) {
            gui.drawString(font, label, iconX - font.width(label) - 2, y + 4, COLOR_LABEL);
        }

        // 子弹类型图标：实际生效的弹药（弹夹已装填的弹种，随持枪 NBT 同步）
        gui.renderItem(new ItemStack(gun.getEffectiveAmmoItem(gunStack)), iconX, y);

        // 当前弹药/弹夹容量：排在图标右侧
        String count = ammo + "/" + stats.magazineSize();
        int color = reloading ? COLOR_RELOADING : (ammo <= 0 ? COLOR_EMPTY : COLOR_AMMO);
        gui.drawString(font, count, iconX + ICON_SIZE + 2, y + 4, color);
    }
}
