package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.gun.GunItem;
import Alone818.com.alone_adventure.Items.gun.GunUpgradeItem;
import Alone818.com.alone_adventure.Items.gun.GunUpgradeManager;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 枪械改装台规则：
 *
 * 铁砧：
 * 左槽 = 枪械
 * 右槽 = 改装件
 * 消耗 3 级经验 + 1 个改装件
 * 消耗枪械 1 个改装槽
 *
 * 砂轮：
 * 卸下枪械上的全部改装件，恢复基础属性。
 */
@Mod.EventBusSubscriber(
        modid = Alone_adventure.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GunModificationEvents {

    private GunModificationEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack gun = event.getLeft();
        ItemStack upgradeStack = event.getRight();

        if (!(gun.getItem() instanceof GunItem)) {
            return;
        }

        if (!(upgradeStack.getItem() instanceof GunUpgradeItem upgrade)) {
            return;
        }

        if (!GunUpgradeManager.canInstall(gun, upgrade)) {
            return;
        }

        ItemStack output = gun.copy();

        if (!GunUpgradeManager.install(output, upgrade)) {
            return;
        }

        event.setOutput(output);

        // 固定消耗 3 级经验。
        event.setCost(3);

        // 消耗右侧 1 个改装件。
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    public static void onGrindstonePlace(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();

        ItemStack gun = null;

        if (top.getItem() instanceof GunItem) {
            gun = top;
        } else if (bottom.getItem() instanceof GunItem) {
            gun = bottom;
        }

        if (gun == null || GunUpgradeManager.getInstalledUpgrades(gun).isEmpty()) {
            return;
        }

        ItemStack cleaned = gun.copy();
        GunUpgradeManager.removeAll(cleaned);

        // OnPlaceItem 直接设置砂轮输出，因此拿走输出后
        // 原枪械输入会照常被消耗。
        event.setOutput(cleaned);
    }
}
