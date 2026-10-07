package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Curios.towerCurios.binding_bandage;
import Alone818.com.alone_adventure.Curios.towerCurios.broken_mask;
import Alone818.com.alone_adventure.Curios.towerCurios.charging_core;
import Alone818.com.alone_adventure.Curios.miscCurios.crystalline_heart;
import Alone818.com.alone_adventure.Curios.towerCurios.sealed_throne;
import Alone818.com.alone_adventure.Curios.trait.shadow_step;
import Alone818.com.alone_adventure.Curios.miscCurios.hunter_serum;
import Alone818.com.alone_adventure.Curios.ImperialCurios.imperial_eagle;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 护盾 HUD 渲染器 —— 在生命值左侧显示水晶心（crystalline_heart）与紧缚绷带（binding_bandage）护盾。
 *
 * 同时支持右侧显示饰品技能 CD（暗影跃迁等）。
 *
 * 布局（参考 alone_journey 的 ShieldHudOverlay）：
 *   两个护盾从右往左排布：绷带护盾 → 水晶心护盾 → ♥♥♥♥♥…
 *   [绷带图标][绷带数值] [水晶心图标][水晶心数值] ♥♥♥♥♥…
 *
 * 右侧技能 CD 显示：
 *   在屏幕右侧生命值右侧显示佩戴的技能饰品 CD。
 *   格式：[图标] [剩余秒数]，当 CD 满时不显示。
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

    /** 侧边栏 CD 显示开关（客户端本地状态，F6 键切换，见 ClientInputHandler） */
    public static boolean SHOW_CURIOS_CD = false;

    /** 侧边栏：最多显示的饰品 CD 条目数 */
    private static final int SIDEBAR_MAX_ENTRIES = 8;

    /** 护盾图标图集 */
    private static final ResourceLocation SHIELD_ICONS =
            new ResourceLocation(Alone_adventure.MODID, "textures/gui/shield_icons.png");

    /** 技能 CD 图标图集 */
    private static final ResourceLocation TRAIT_ICONS =
            new ResourceLocation(Alone_adventure.MODID, "textures/gui/trait_icons.png");

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

    // ── 护盾闪烁状态 ──
    /** 上一帧王座护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double throneLastShield = -1.0D;
    /** 王座护盾闪烁结束的游戏刻 */
    private static long throneFlashUntilTick = Long.MIN_VALUE;

    /** 上一帧水晶心护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double crystalLastShield = -1.0D;
    /** 水晶心护盾闪烁结束的游戏刻 */
    private static long crystalFlashUntilTick = Long.MIN_VALUE;

    /** 上一帧绷带护盾值，用于检测掉盾触发闪烁；-1 表示尚未渲染过 */
    private static double bandageLastShield = -1.0D;
    /** 绷带护盾闪烁结束的游戏刻 */
    private static long bandageFlashUntilTick = Long.MIN_VALUE;

    // ── 技能 CD 常量 ──
    /** CD 图标大小（宽高） */
    private static final int TRAIT_CD_ICON_SIZE = 16;
    /** CD 图标 X 偏移（从右侧生命值右侧往左） */
    private static final int TRAIT_CD_X_OFFSET = 10;
    /** 图集尺寸 */
    private static final int TRAIT_TEXTURE_WIDTH = 128;
    private static final int TRAIT_TEXTURE_HEIGHT = 16;

    /**
     * 技能 CD 注册信息
     */
    public static class SkillCDInfo {
        final RegistryObject<? extends Item> item;
        final int iconU;
        final java.util.function.BiFunction<ItemStack, Player, Long> cooldownGetter;

        public SkillCDInfo(RegistryObject<? extends Item> item, int iconU,
                          java.util.function.BiFunction<ItemStack, Player, Long> cooldownGetter) {
            this.item = item;
            this.iconU = iconU;
            this.cooldownGetter = cooldownGetter;
        }
    }

    /** 所有具有 CD 的饰品列表（按显示顺序） */
    private static final List<SkillCDInfo> SKILL_CD_ITEMS = new ArrayList<>();

    static {
        // 注册暗影跃迁 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.SHADOW_STEP,
                0, // 图标在图集第 0 列
                (stack, player) -> shadow_step.getSkillCooldownRemaining(stack, player.level())
        ));

        // 注册帝国天鹰 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.IMPERIAL_EAGLE,
                16, // 图标在图集第 1 列（16px 偏移）
                (stack, player) -> imperial_eagle.getSkillCooldownRemaining(stack, player.level())
        ));

        // 注册猎人血清 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.HUNTER_SERUM,
                32, // 图标在图集第 2 列（32px 偏移）
                (stack, player) -> hunter_serum.getSkillCooldownRemaining(stack, player.level())
        ));

        // 注册紧缚绷带 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.BINDING_BANDAGE,
                48, // 图标在图集第 3 列（48px 偏移）
                (stack, player) -> binding_bandage.getSkillCooldownRemaining(stack, player.level())
        ));

        // 注册充能核心 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.CHARGING_CORE,
                64, // 图标在图集第 4 列（64px 偏移）
                (stack, player) -> charging_core.getSkillCooldownRemaining(stack, player.level())
        ));

        // 注册破损面具 CD 显示
        SKILL_CD_ITEMS.add(new SkillCDInfo(
                ModItems.BROKEN_MASK,
                80, // 图标在图集第 5 列（80px 偏移）
                (stack, player) -> broken_mask.getSkillCooldownRemaining(stack, player.level())
        ));
    }


    @SubscribeEvent
    public static void onRenderHealth(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        long gameTime = mc.level.getGameTime();

        ICuriosHelper helper = CuriosApi.getCuriosHelper();

        // 技能 CD 显示：只看 F6 开关，不限制模式
        // 挂载到 HOTBAR，因为创造模式下原版不渲染 PLAYER_HEALTH，但 HOTBAR 所有模式都渲染
        if (SHOW_CURIOS_CD && event.getOverlay().id() == VanillaGuiOverlay.HOTBAR.id()) {
            // 在屏幕最右侧边缘显示佩戴的技能饰品 CD（向右移10像素）
            int baseX = screenWidth - 40;
            int baseY = 120;
            int entryHeight = 18;

            int entryIndex = 0;
            for (SkillCDInfo info : SKILL_CD_ITEMS) {
                if (entryIndex >= SIDEBAR_MAX_ENTRIES) break;

                Optional<SlotResult> opt = helper.findFirstCurio(player, info.item.get());
                if (opt.isPresent()) {
                    ItemStack stack = opt.get().stack();
                    long remainingTicks = info.cooldownGetter.apply(stack, player);

                    int entryX = baseX;
                    int entryY = baseY + entryIndex * entryHeight;

                    gui.renderItem(stack, entryX, entryY);

                    if (remainingTicks > 0) {
                        int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
                        String cdText = remainingSeconds + "s";
                        gui.drawString(font, cdText, entryX + 17, entryY + 6, 0xFFFFFFFF);
                    }

                    entryIndex++;
                }
            }
        }

        // 护盾显示：生存/冒险显示，创造/旁观隐藏
        // 不再依赖 PLAYER_HEALTH overlay，改为直接渲染到屏幕固定位置
        // 这样即使其他模组替换生命渲染，护盾依然会显示
        if (mc.gameMode == null) {
            return;
        }
        boolean isCreative = mc.gameMode.getPlayerMode() == GameType.CREATIVE;

        // 创造模式：不渲染护盾
        if (isCreative) {
            return;
        }

        // 旁观模式不显示护盾
        GameType gameType = mc.gameMode.getPlayerMode();
        if (gameType == GameType.SPECTATOR) {
            return;
        }

        // 移除 overlay id 检查，直接在 HOTBAR 渲染时绘制护盾到固定位置
        // 这样无论原版生命渲染是否被替换，护盾都会显示
        if (event.getOverlay().id() != VanillaGuiOverlay.HOTBAR.id()) {
            return;
        }

        // 血量行 y；红心区域最左为 screenWidth/2 - 91，两个护盾从该位置向左排布
        int y = screenHeight - 39;
        int rightEdge = screenWidth / 2 - 91;


        // 封印王座护盾
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

        // 紧缚绷带护盾
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

        // 水晶心护盾
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
