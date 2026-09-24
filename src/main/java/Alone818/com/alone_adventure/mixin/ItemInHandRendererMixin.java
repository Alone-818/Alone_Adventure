package Alone818.com.alone_adventure.mixin;

import Alone818.com.alone_adventure.Items.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 防止 GunItem 因 NBT 更新触发 vanilla 重新装备动画。
 *
 * 关键：
 *
 * 这里只处理 ItemInHandRenderer 的 equip progress。
 *
 * GeckoLib 的：
 *
 *     reload
 *     fire
 *     aim
 *     run
 *     idle
 *
 * 完全由 GunItemAnimation 控制。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

    /**
     * 在 vanilla tick 最后处理。
     *
     * 不取消 vanilla tick。
     *
     * 普通物品继续正常工作。
     */
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void aloneAdventure$fixGunEquip(
            CallbackInfo ci
    ) {

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        ItemStack main =
                mc.player.getMainHandItem();

        ItemStack off =
                mc.player.getOffhandItem();

        // =====================================================
        // 主手枪械
        // =====================================================

        if (main.getItem() instanceof GunItem) {

            /*
             * 当前 renderer 永远使用玩家当前枪械 Stack。
             *
             * NBT 改变也不会留下旧 Stack。
             */
            this.mainHandItem = main;

            /*
             * 禁止 vanilla equip 动画。
             */
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
        }

        // =====================================================
        // 副手枪械
        // =====================================================

        if (off.getItem() instanceof GunItem) {

            this.offHandItem = off;

            this.offHandHeight = 1.0F;
            this.oOffHandHeight = 1.0F;
        }
    }
}