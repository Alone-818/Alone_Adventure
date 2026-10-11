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
import Alone818.com.alone_adventure.Curios.trait.black_flower;
import Alone818.com.alone_adventure.Curios.trait.victim_humus;
import Alone818.com.alone_adventure.Curios.trait.chronos_reagent;
import Alone818.com.alone_adventure.Curios.towerCurios.confession_apocalypse;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.util.ICuriosHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.CompoundTag;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 护盾 HUD 渲染器 —— 在生命值左侧显示水晶心（crystalline_heart）与紧缚绷带（binding_bandage）护盾。
 *
 * 同时支持右侧显示饰品技能 CD（暗影跃迁等）以及叠层饰品（黑色花朵、受害者腐殖、克罗诺斯试剂）。
 *
 * 布局（参考 alone_journey 的 ShieldHudOverlay）：
 *   两个护盾从右往左排布：绷带护盾 → 水晶心护盾 → ♥♥♥♥♥…
 *   [绷带图标][绷带数值] [水晶心图标][水晶心数值] ♥♥♥♥♥…
 *
 * 右侧技能 CD 显示：
 *   在屏幕右侧生命值右侧显示佩戴的技能饰品 CD。
 *   格式：[图标] [剩余秒数]，当 CD 满时不显示。
 *
 * 右侧叠层饰品显示：
 *   与 CD 饰品共用右侧区域，显示佩戴的叠层饰品及其层数。
 *   格式：[图标] [当前层数/上限]，例如 "3/7"。
 *   叠层饰品包括：黑色花朵 (灵魂 0-7)、受害者腐殖 (黄心 0-20)、克罗诺斯试剂 (力量 0-9)。
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
    public static boolean SHOW_CURIOS_CD = true;

    /** 侧边栏：最多显示的饰品条目数 */
    private static final int SIDEBAR_MAX_ENTRIES = 8;

    // ── 侧边栏配色 ──
    /** CD 文本颜色：白色 */
    private static final int COLOR_CD = 0xFFFFFFFF;
    /** 叠层文本颜色：浅蓝 */
    private static final int COLOR_LAYER = 0xFF8AC9FF;
    /** 叠层满层文本颜色：金色 */
    private static final int COLOR_LAYER_MAX = 0xFFFFAA00;

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

    /**
     * 统一饰品显示注册信息
     *
     * 每个条目可以具有 CD 获取器和/或层数获取器：
     * - CD 获取器：返回技能剩余冷却 tick（0 表示可用），当 >0 时显示 "Xs"
     * - 层数获取器：返回 "current/max" 格式的叠层信息，始终显示
     */
    public static class CurioDisplayInfo {
        final RegistryObject<? extends Item> item;
        final java.util.function.BiFunction<ItemStack, Player, Long> cdGetter;
        final java.util.function.BiFunction<ItemStack, Player, int[]> layerGetter; // [current, max]，null 表示无叠层

        public CurioDisplayInfo(RegistryObject<? extends Item> item,
                               java.util.function.BiFunction<ItemStack, Player, Long> cdGetter) {
            this.item = item;
            this.cdGetter = cdGetter;
            this.layerGetter = null;
        }

        public CurioDisplayInfo(RegistryObject<? extends Item> item,
                               java.util.function.BiFunction<ItemStack, Player, Long> cdGetter,
                               java.util.function.BiFunction<ItemStack, Player, int[]> layerGetter) {
            this.item = item;
            this.cdGetter = cdGetter;
            this.layerGetter = layerGetter;
        }
    }

    /** 所有需要显示的饰品列表（CD 项 + 叠层项，按显示顺序） */
    private static final List<CurioDisplayInfo> CURIO_DISPLAY_ITEMS = new ArrayList<>();

    static {
        // CD 项
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.SHADOW_STEP,
                (stack, player) -> shadow_step.getSkillCooldownRemaining(stack, player.level())
        ));

        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.IMPERIAL_EAGLE,
                (stack, player) -> imperial_eagle.getSkillCooldownRemaining(stack, player.level())
        ));

        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.HUNTER_SERUM,
                (stack, player) -> hunter_serum.getSkillCooldownRemaining(stack, player.level())
        ));

        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.BINDING_BANDAGE,
                (stack, player) -> binding_bandage.getSkillCooldownRemaining(stack, player.level())
        ));

        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.CHARGING_CORE,
                (stack, player) -> charging_core.getSkillCooldownRemaining(stack, player.level())
        ));

        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.BROKEN_MASK,
                (stack, player) -> broken_mask.getSkillCooldownRemaining(stack, player.level())
        ));

        // 叠层项（黑色花朵）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.BLACK_FLOWER,
                (stack, player) -> 0L, // 无 CD
                (stack, player) -> new int[]{ black_flower.getSoulCount(stack), black_flower.MAX_SOUL }
        ));

        // 叠层项（受害者腐殖）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.VICTIM_HUMUS,
                (stack, player) -> 0L, // 无 CD
                (stack, player) -> new int[]{ victim_humus.getAbsorptionLayers(stack), victim_humus.MAX_LAYERS }
        ));

        // 叠层项（克罗诺斯试剂）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.CHRONOS_REAGENT,
                (stack, player) -> 0L, // 无 CD
                (stack, player) -> {
                    MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
                    int level = strength != null ? strength.getAmplifier() + 1 : 0;
                    return new int[]{ level, chronos_reagent.MAX_STRENGTH_LEVEL };
                }
        ));

        // 叠层项（忏悔默示录）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.CONFESSION_APOCALYPSE,
                (stack, player) -> confession_apocalypse.getSkillCooldownRemaining(stack, player.level()),
                (stack, player) -> new int[]{ confession_apocalypse.getSin(stack), confession_apocalypse.MAX_SIN }
        ));

        // 叠层项（充能核心）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.CHARGING_CORE,
                (stack, player) -> charging_core.getSkillCooldownRemaining(stack, player.level()),
                (stack, player) -> new int[]{ charging_core.getLayer(stack), charging_core.Form.fromId(stack.getTag() != null ? stack.getTag().getInt(charging_core.NB_TAG_FORM) : 0).maxLayers }
        ));

        // 叠层项（封印王座）
        CURIO_DISPLAY_ITEMS.add(new CurioDisplayInfo(
                ModItems.SEALED_THRONE,
                (stack, player) -> 0L, // 无技能 CD，星辉回复即天然冷却
                (stack, player) -> new int[]{ sealed_throne.getStars(stack), sealed_throne.MAX_STARS }
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

        // ── 侧边栏饰品显示 ──
        // 显示 F6 开关控制，挂载到 HOTBAR（创造模式下原版不渲染 PLAYER_HEALTH，但 HOTBAR 所有模式都渲染）
        if (SHOW_CURIOS_CD && event.getOverlay().id() == VanillaGuiOverlay.HOTBAR.id()) {
            int baseX = screenWidth - 50;
            int baseY = 120;
            int entryHeight = 18;

            int entryIndex = 0;
            for (CurioDisplayInfo info : CURIO_DISPLAY_ITEMS) {
                if (entryIndex >= SIDEBAR_MAX_ENTRIES) break;

                Optional<SlotResult> opt = helper.findFirstCurio(player, info.item.get());
                if (opt.isPresent()) {
                    ItemStack stack = opt.get().stack();
                    int entryX = baseX;
                    int entryY = baseY + entryIndex * entryHeight;

                    // 渲染物品图标
                    gui.renderItem(stack, entryX, entryY);

                    // 获取 CD 和层数信息
                    long cdTicks = info.cdGetter != null ? info.cdGetter.apply(stack, player) : 0L;
                    boolean isLayered = info.layerGetter != null;
                    boolean hasMax = false;
                    String displayText = "";

                    if (isLayered) {
                        int[] layers = info.layerGetter.apply(stack, player);
                        int current = layers[0];
                        int max = layers[1];
                        displayText = current + "/" + max;
                        hasMax = current >= max;
                    } else if (cdTicks > 0) {
                        int remainingSeconds = (int) Math.ceil(cdTicks / 20.0);
                        displayText = remainingSeconds + "s";
                    }

                    if (!displayText.isEmpty()) {
                        int textX = entryX + 17;
                        int textColor = isLayered ? (hasMax ? COLOR_LAYER_MAX : COLOR_LAYER) : COLOR_CD;
                        gui.drawString(font, displayText, textX, entryY + 6, textColor);
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
