package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.crystalline_heart;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.Optional;

/**
 * 护盾 HUD 渲染器 —— 在生命值左侧显示水晶心（crystalline_heart）护盾。
 *
 * 布局（参考 alone_journey 的 ShieldHudOverlay）：
 *   [护盾图标][护盾数值] ♥♥♥♥♥…
 * 图标绘制在血量行（screenHeight - 39）上、红心起始位置（screenWidth/2 - 91）的左侧。
 *
 * 纹理 textures/gui/shield_icons.png 为 36x9 的横条，含 4 帧 9px 图标：
 *   u=0  空格（未装备/无护盾时的底槽）
 *   u=9  满护盾（蓝色）
 *   u=18 备用
 *   u=27 受击闪烁帧
 *
 * 交互：护盾值下降瞬间图标切换为闪烁帧、文字变白，持续 10 tick；
 * 护盾耗尽时图标与文字均以灰色显示 "0"。
 * 仅在生存/冒险模式下渲染；护盾数据读取 Curios 自动同步到客户端的 NBT。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShieldHudOverlay {

    private ShieldHudOverlay() {}

    /** 护盾图标图集 */
    private static final ResourceLocation SHIELD_ICONS =
            new ResourceLocation(Alone_adventure.MODID, "textures/gui/shield_icons.png");

    private static final int ICON_SIZE = 9;
    /** 与 ShieldEvent 保持一致：每 5 点护甲值提供 1 点护盾上限（HUD 端重算同一公式） */
    private static final int ARMOR_TO_SHIELD = 5;

    private static final int U_EMPTY = 0;
    private static final int U_FULL = 9;
    private static final int U_FLASH = 27;

    private static final int TEXTURE_WIDTH = 36;
    private static final int TEXTURE_HEIGHT = 9;

    private static final int COLOR_SHIELD = 0xFF3BD3EE; // 护盾蓝
    private static final int COLOR_EMPTY = 0xFF8A8A8A;  // 耗尽灰
    private static final int COLOR_FLASH = 0xFFFFFFFF;  // 受击闪烁白

    /** 上一帧护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double lastShield = -1.0D;
    /** 闪烁结束的游戏刻 */
    private static long flashUntilTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onRenderHealth(RenderGuiOverlayEvent.Post event) {
        // 只挂在原版生命槽渲染完成之后，保证跟随血量 HUD 的显示时机
        if (event.getOverlay().id() != VanillaGuiOverlay.PLAYER_HEALTH.id()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (mc.gameMode == null) {
            return;
        }
        // 创造/旁观模式不显示护盾
        GameType gameType = mc.gameMode.getPlayerMode();
        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 读取客户端已同步的水晶心 NBT
        ICuriosHelper helper = CuriosApi.getCuriosHelper();
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isEmpty()) {
            return;
        }
        ItemStack stack = crystalOpt.get().stack();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(crystalline_heart.NB_TAG_SHIELD)) {
            return;
        }

        // 最大护盾 = 基础上限（服务端写入） + 护甲加成（与 ShieldEvent 同公式，实时重算）
        double maxShield = tag.getDouble(crystalline_heart.NB_TAG_MAX_SHIELD)
                + player.getArmorValue() / (double) ARMOR_TO_SHIELD;
        if (maxShield <= 0) {
            return;
        }
        double shield = Mth.clamp(tag.getDouble(crystalline_heart.NB_TAG_SHIELD), 0, maxShield);

        long gameTime = mc.level.getGameTime();

        // 掉盾瞬间触发 10 tick 闪烁（首帧 lastShield < 0 不闪）
        if (lastShield >= 0 && shield < lastShield) {
            flashUntilTick = gameTime + 10L;
        }
        lastShield = shield;

        boolean flashing = gameTime < flashUntilTick;
        boolean empty = shield <= 0;

        String text = String.valueOf((int) Math.ceil(shield));
        int textWidth = font.width(text);

        // 血量行 y；红心区域最左为 screenWidth/2 - 91，图标再往左排布
        int y = screenHeight - 39;
        int iconX = screenWidth / 2 - 91 - textWidth - ICON_SIZE - 3;
        int textX = iconX + ICON_SIZE + 1;

        // 图标帧：空 → 底槽，闪烁 → 闪帧，否则满盾
        int u = empty ? U_EMPTY : (flashing ? U_FLASH : U_FULL);
        gui.blit(SHIELD_ICONS, iconX, y, u, 0F, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 文字颜色随状态：灰 → 白（闪）→ 蓝
        int color = empty ? COLOR_EMPTY : (flashing ? COLOR_FLASH : COLOR_SHIELD);
        gui.drawString(font, text, textX, y + 1, color);
    }
}
