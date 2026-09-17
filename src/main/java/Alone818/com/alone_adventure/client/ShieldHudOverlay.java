package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.binding_bandage;
import Alone818.com.alone_adventure.Curios.crystalline_heart;
import Alone818.com.alone_adventure.Curios.sealed_throne;
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
 * 护盾 HUD 渲染器 —— 在生命值左侧显示水晶心（crystalline_heart）与紧缚绷带（binding_bandage）护盾。
 *
 * 布局（参考 alone_journey 的 ShieldHudOverlay）：
 *   两个护盾从右往左排布：绷带护盾 → 水晶心护盾 → ♥♥♥♥♥…
 *   [绷带图标][绷带数值] [水晶心图标][水晶心数值] ♥♥♥♥♥…
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
    private static final int COLOR_STAR = 0xFFD9A6FF;   // 王座星辉紫

    // ── 封印王座护盾闪烁状态 ──
    /** 上一帧王座护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double throneLastShield = -1.0D;
    /** 王座护盾闪烁结束的游戏刻 */
    private static long throneFlashUntilTick = Long.MIN_VALUE;

    // ── 水晶心护盾闪烁状态 ──
    /** 上一帧水晶心护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double crystalLastShield = -1.0D;
    /** 水晶心护盾闪烁结束的游戏刻 */
    private static long crystalFlashUntilTick = Long.MIN_VALUE;

    // ── 绷带护盾闪烁状态 ──
    /** 上一帧绷带护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double bandageLastShield = -1.0D;
    /** 绷带护盾闪烁结束的游戏刻 */
    private static long bandageFlashUntilTick = Long.MIN_VALUE;

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
        long gameTime = mc.level.getGameTime();

        // 血量行 y；红心区域最左为 screenWidth/2 - 91，两个护盾从该位置向左排布
        int y = screenHeight - 39;
        // rightEdge 表示下一个护盾图标的右边缘（从右往左逐个追加）
        int rightEdge = screenWidth / 2 - 91;

        ICuriosHelper helper = CuriosApi.getCuriosHelper();

        // ── 0. 封印王座护盾（整数，主动技能消耗星辉获得，星辉归零时熄灭）──
        Optional<SlotResult> throneOpt = helper.findFirstCurio(player, ModItems.SEALED_THRONE.get());
        if (throneOpt.isPresent()) {
            ItemStack throneStack = throneOpt.get().stack();
            CompoundTag throneTag = throneStack.getTag();
            double throneShield = 0;
            if (throneTag != null && throneTag.contains(sealed_throne.NB_TAG_SHIELD)) {
                throneShield = Math.max(0, throneTag.getDouble(sealed_throne.NB_TAG_SHIELD));
            }
            if (throneShield > 0) {
                if (throneLastShield >= 0 && throneShield < throneLastShield) {
                    throneFlashUntilTick = gameTime + 10L;
                }
                throneLastShield = throneShield;

                boolean flashing = gameTime < throneFlashUntilTick;
                String text = String.valueOf((int) Math.ceil(throneShield));
                int textWidth = font.width(text);

                int iconX = rightEdge - ICON_SIZE;
                int textX = iconX - textWidth - 1;
                rightEdge = textX - 3;

                int u = flashing ? U_FLASH : U_FULL;
                gui.blit(SHIELD_ICONS, iconX, y, u, 0F, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

                int color = flashing ? COLOR_FLASH : COLOR_STAR;
                gui.drawString(font, text, textX, y + 1, color);
            } else {
                throneLastShield = -1.0D; // 护盾熄灭后重置闪烁检测
            }
        }

        // ── 1. 紧缚绷带护盾（整数 0~3，无护甲加成）──
        Optional<SlotResult> bandageOpt = helper.findFirstCurio(player, ModItems.BINDING_BANDAGE.get());
        if (bandageOpt.isPresent()) {
            ItemStack bandageStack = bandageOpt.get().stack();
            CompoundTag bandageTag = bandageStack.getTag();
            double bandageShield = 0;
            if (bandageTag != null && bandageTag.contains(binding_bandage.NB_TAG_SHIELD)) {
                bandageShield = Mth.clamp(bandageTag.getDouble(binding_bandage.NB_TAG_SHIELD), 0, binding_bandage.SHIELD_MAX);
            }

            // 掉盾瞬间触发 10 tick 闪烁（首帧 lastShield < 0 不闪）
            if (bandageLastShield >= 0 && bandageShield < bandageLastShield) {
                bandageFlashUntilTick = gameTime + 10L;
            }
            bandageLastShield = bandageShield;

            boolean flashing = gameTime < bandageFlashUntilTick;
            boolean empty = bandageShield <= 0;

            String text = String.valueOf((int) Math.ceil(bandageShield));
            int textWidth = font.width(text);

            int iconX = rightEdge - ICON_SIZE;
            int textX = iconX - textWidth - 1;
            rightEdge = textX - 3; // 为下一个护盾留出间距

            int u = empty ? U_EMPTY : (flashing ? U_FLASH : U_FULL);
            gui.blit(SHIELD_ICONS, iconX, y, u, 0F, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

            int color = empty ? COLOR_EMPTY : (flashing ? COLOR_FLASH : COLOR_SHIELD);
            gui.drawString(font, text, textX, y + 1, color);
        }

        // ── 2. 水晶心护盾（浮点数，受护甲加成）──
        Optional<SlotResult> crystalOpt = helper.findFirstCurio(player, ModItems.CRYSTALLINE_HEART.get());
        if (crystalOpt.isPresent()) {
            ItemStack crystalStack = crystalOpt.get().stack();
            CompoundTag crystalTag = crystalStack.getTag();
            if (crystalTag != null && crystalTag.contains(crystalline_heart.NB_TAG_SHIELD)) {
                // 最大护盾 = 基础上限（服务端写入） + 护甲加成（与 ShieldEvent 同公式，实时重算）
                double maxShield = crystalTag.getDouble(crystalline_heart.NB_TAG_MAX_SHIELD)
                        + player.getArmorValue() / (double) ARMOR_TO_SHIELD;
                if (maxShield > 0) {
                    double shield = Mth.clamp(crystalTag.getDouble(crystalline_heart.NB_TAG_SHIELD), 0, maxShield);

                    // 掉盾瞬间触发 10 tick 闪烁
                    if (crystalLastShield >= 0 && shield < crystalLastShield) {
                        crystalFlashUntilTick = gameTime + 10L;
                    }
                    crystalLastShield = shield;

                    boolean flashing = gameTime < crystalFlashUntilTick;
                    boolean empty = shield <= 0;

                    String text = String.valueOf((int) Math.ceil(shield));
                    int textWidth = font.width(text);

                    int iconX = rightEdge - ICON_SIZE;
                    int textX = iconX - textWidth - 1;

                    int u = empty ? U_EMPTY : (flashing ? U_FLASH : U_FULL);
                    gui.blit(SHIELD_ICONS, iconX, y, u, 0F, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);

                    int color = empty ? COLOR_EMPTY : (flashing ? COLOR_FLASH : COLOR_SHIELD);
                    gui.drawString(font, text, textX, y + 1, color);
                }
            }
        }
    }
}
