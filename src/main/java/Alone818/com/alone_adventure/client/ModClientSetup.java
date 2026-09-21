package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.powersword;
import Alone818.com.alone_adventure.init.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端物品动态属性注册器 —— 驱动举盾 / 超频的模型变换。
 *
 * 原理（Forge 1.20.1）：物品模型 JSON 里 overrides 的 predicate 名
 * （"alone_adventure:blocking" / "alone_adventure:overclock"）只是声明，
 * 必须在此用 ItemProperties.register 注册返回 0.0~1.0 的属性函数，
 * 渲染引擎每帧读取该值，才会真在两个模型之间切换。
 *
 * 1. 招架之盾：玩家正在使用（举盾）→ blocking = 1.0 → 切换到 parryshield_blocking.json
 * 2. 动力剑：堆栈 NBT 中超频结束时刻未到 → overclock = 1.0 → 切换到 powersword_light.json
 * 3. 链锯剑：正在使用（右键扫射/超频激活中）→ overclock = 1.0 → 切换到 chainsawsword_overclock.json
 *
 * 枪械（双持举枪等）已改走 GeckoLib 模型管线（GunGeoRenderer / GunGeoModel），
 * 不再使用原版模型 overrides 谓词。
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientSetup {

    private ModClientSetup() {}

    /** 饰品主动技能按键（触发帝国天鹰主动技能） */
    public static final KeyMapping EAGLE_SKILL_KEY = new KeyMapping(
            "key.alone_adventure.curio_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.alone_adventure");

    /** 枪械装填按键（默认 R；持枪时 R 优先装填而非饰品技能，见 ClientInputHandler 的让位守卫） */
    public static final KeyMapping GUN_RELOAD_KEY = new KeyMapping(
            "key.alone_adventure.gun_reload",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.alone_adventure");

    /** 枪械切换弹药按键（默认 G，主手枪支持多种弹药时循环切换） */
    public static final KeyMapping GUN_SWITCH_AMMO_KEY = new KeyMapping(
            "key.alone_adventure.gun_switch_ammo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.alone_adventure");

    /** 注册按键绑定（模组事件总线） */
    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(EAGLE_SKILL_KEY);
        event.register(GUN_RELOAD_KEY);
        event.register(GUN_SWITCH_AMMO_KEY);
    }

    /** 举盾状态属性名，需与 parryshield.json 中 overrides 的键完全一致 */
    public static final ResourceLocation BLOCKING_PROPERTY =
            new ResourceLocation(Alone_adventure.MODID, "blocking");

    /** 超频状态属性名，需与 powersword.json / chainsawsword.json 中 overrides 的键完全一致 */
    public static final ResourceLocation OVERCLOCK_PROPERTY =
            new ResourceLocation(Alone_adventure.MODID, "overclock");


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // ===== 招架之盾：举盾模型变换 =====
            // 玩家举起招架之盾时，模型切换到 parryshield_blocking.json
            ItemProperties.register(ModItems.PARRYSHIELD.get(), BLOCKING_PROPERTY,
                    (stack, level, entity, seed) -> (entity instanceof Player player
                            && player.isUsingItem()
                            && player.getUseItem().is(ModItems.PARRYSHIELD.get())) ? 1.0F : 0.0F);

            // ===== 动力剑：超频模型变换 =====
            // 堆栈 NBT 中记录的超频结束时刻未到时，模型切换到 powersword_light.json
            ItemProperties.register(ModItems.POWERSWORD.get(), OVERCLOCK_PROPERTY,
                    (stack, level, entity, seed) -> level != null
                            && powersword.isOverclocked(stack, level.getGameTime()) ? 1.0F : 0.0F);
            // ===== 链锯剑：扫射（超频）模型变换 =====
            // 正在使用（右键扫射激活中）时 overclock = 1.0，模型切换到 chainsawsword_overclock.json
            //（链锯剑无 NBT 超频标记，超频状态即"使用中"，与招架之盾的举盾判定同款）
            ItemProperties.register(ModItems.CHAINSAW_SWORD.get(), OVERCLOCK_PROPERTY,
                    (stack, level, entity, seed) -> (entity instanceof Player player
                            && player.isUsingItem()
                            && player.getUseItem().is(ModItems.CHAINSAW_SWORD.get())) ? 1.0F : 0.0F);
        });
    }
}