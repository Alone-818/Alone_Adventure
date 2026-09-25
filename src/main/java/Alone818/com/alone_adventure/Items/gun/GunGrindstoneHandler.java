package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * 枪械改装砂轮处理器。
 *
 * 功能：
 *
 * 1. 改装枪放入砂轮后：
 *      输出 = 移除全部改装后的枪械
 *
 * 2. 玩家拿走输出后：
 *      自动返还枪械上的全部改装件
 *
 * 3. 不需要写死具体改装件。
 *
 * 例如：
 *
 *      重型枪管
 *      追踪改件
 *      消音器
 *      扩容弹匣
 *      高倍镜
 *      穿甲改件
 *
 * 只要属于 GunUpgradeItem，
 * 并且被 GunUpgradeManager 安装，
 * 这里都会自动处理。
 *
 * 4. 如果玩家背包没有空间：
 *      改装件会直接掉落在玩家脚下。
 */
@Mod.EventBusSubscriber(
        modid = "alone_adventure",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GunGrindstoneHandler {

    private GunGrindstoneHandler() {
    }

    /**
     * 砂轮输入改变时生成卸载后的枪械。
     */
    @SubscribeEvent
    public static void onGrindstonePlace(
            GrindstoneEvent.OnPlaceItem event
    ) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();

        ItemStack gun = ItemStack.EMPTY;

        /*
         * 上槽放枪械，下槽为空
         */
        if (!top.isEmpty()
                && top.getItem() instanceof GunItem
                && hasInstalledUpgrades(top)
                && bottom.isEmpty()) {

            gun = top;
        }

        /*
         * 下槽放枪械，上槽为空
         */
        else if (!bottom.isEmpty()
                && bottom.getItem() instanceof GunItem
                && hasInstalledUpgrades(bottom)
                && top.isEmpty()) {

            gun = bottom;
        }

        /*
         * 不符合枪械卸载条件。
         *
         * 交给原版砂轮。
         */
        if (gun.isEmpty()) {
            return;
        }

        /*
         * 复制枪械。
         *
         * 原枪械不能直接修改。
         */
        ItemStack result = gun.copy();

        /*
         * 删除全部改装。
         *
         * 不判断具体改装类型。
         */
        GunUpgradeManager.removeAll(result);

        /*
         * 设置砂轮输出。
         */
        event.setOutput(result);

        /*
         * 卸载改装不消耗经验。
         */
        event.setXp(0);
    }

    /**
     * 玩家拿走砂轮输出时：
     *
     * 1. 找到当前使用砂轮的玩家
     * 2. 从原枪械 NBT 中读取全部改装
     * 3. 每个改装件返还 1 个
     * 4. 背包放不下的直接掉落
     * 5. 清空砂轮输入
     */
    @SubscribeEvent
    public static void onGrindstoneTake(
            GrindstoneEvent.OnTakeItem event
    ) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();

        ItemStack gun = ItemStack.EMPTY;

        /*
         * 找到原来的改装枪。
         */
        if (!top.isEmpty()
                && top.getItem() instanceof GunItem
                && hasInstalledUpgrades(top)
                && bottom.isEmpty()) {

            gun = top;
        }

        else if (!bottom.isEmpty()
                && bottom.getItem() instanceof GunItem
                && hasInstalledUpgrades(bottom)
                && top.isEmpty()) {

            gun = bottom;
        }

        /*
         * 不是我们的枪械卸载操作。
         */
        if (gun.isEmpty()) {
            return;
        }

        /*
         * Forge 1.20.1 的 OnTakeItem 没有直接提供 Player。
         *
         * 通过当前打开的 GrindstoneMenu 找到真正操作砂轮的玩家。
         */
        ServerPlayer player = findGrindstonePlayer(top, bottom);

        /*
         * 如果没有找到玩家，
         * 不进行返还，避免把改装件错误掉落给其他玩家。
         */
        if (player == null) {
            return;
        }

        /*
         * 读取全部已安装改装。
         *
         * 这里完全通用。
         */
        List<GunUpgradeItem> upgrades =
                GunUpgradeManager.getInstalledUpgrades(gun);

        /*
         * 逐个返还。
         */
        for (GunUpgradeItem upgrade : upgrades) {

            ItemStack upgradeStack = new ItemStack(upgrade);

            /*
             * 尝试放入玩家背包。
             */
            boolean inserted =
                    player.getInventory().add(upgradeStack);

            /*
             * 背包放不下。
             *
             * 直接掉落到玩家位置。
             */
            if (!inserted && !upgradeStack.isEmpty()) {

                player.drop(
                        upgradeStack,
                        false
                );
            }
        }

        /*
         * 清空砂轮输入。
         *
         * 输出枪械已经被玩家拿走，
         * 所有改装件也已经返还。
         */
        event.setNewTopItem(ItemStack.EMPTY);
        event.setNewBottomItem(ItemStack.EMPTY);

        /*
         * 不给予经验。
         */
        event.setXp(0);
    }

    /**
     * 判断枪械是否安装了改装。
     *
     * 不写死任何改装名称。
     */
    private static boolean hasInstalledUpgrades(ItemStack gun) {

        if (!(gun.getItem() instanceof GunItem)) {
            return false;
        }

        return GunUpgradeManager.getUsedSlots(gun) > 0;
    }

    /**
     * 找到当前正在操作这个砂轮的玩家。
     *
     * Forge 1.20.1 的 GrindstoneEvent.OnTakeItem
     * 没有直接提供 Player，
     * 所以从当前服务器玩家中寻找打开对应 GrindstoneMenu 的玩家。
     *
     * 使用 ItemStack 对象引用进行匹配，而不是只比较物品类型，
     * 避免两个玩家同时使用相同枪械时发生误判。
     */
    private static ServerPlayer findGrindstonePlayer(
            ItemStack eventTop,
            ItemStack eventBottom
    ) {

        MinecraftServer server =
                ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return null;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {

            if (!(player.containerMenu instanceof GrindstoneMenu menu)) {
                continue;
            }

            /*
             * GrindstoneMenu：
             *
             * 0 = 上方输入槽
             * 1 = 下方输入槽
             * 2 = 输出槽
             *
             * 这里比较对象本身。
             */
            ItemStack menuTop =
                    menu.getSlot(0).getItem();

            ItemStack menuBottom =
                    menu.getSlot(1).getItem();

            /*
             * 必须是同一个 ItemStack 对象。
             *
             * Forge 创建 OnTakeItem 时使用的就是
             * 当前输入槽中的 ItemStack。
             */
            if (menuTop == eventTop
                    && menuBottom == eventBottom) {

                return player;
            }
        }

        return null;
    }
}