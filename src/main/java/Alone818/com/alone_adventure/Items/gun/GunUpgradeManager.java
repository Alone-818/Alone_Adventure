package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 枪械改装数据管理。
 *
 * 已安装的改装件只保存改装件注册名，不直接把数值写进枪械 NBT。
 * 因此基础 GunStats 始终不可变，卸下改装后可以重新从基础属性计算。
 */
public final class GunUpgradeManager {

    /** 枪械 ItemStack NBT：已安装改装件 */
    public static final String TAG_UPGRADES = "GunUpgrades";

    private GunUpgradeManager() {
    }

    /** 获取枪械已安装的改装件。 */
    public static List<GunUpgradeItem> getInstalledUpgrades(ItemStack gun) {
        List<GunUpgradeItem> result = new ArrayList<>();

        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(TAG_UPGRADES, 9)) {
            return result;
        }

        ListTag list = tag.getList(TAG_UPGRADES, 8);

        for (int i = 0; i < list.size(); ++i) {
            String id = list.getString(i);

            if (id.isEmpty()) {
                continue;
            }

            Item item =
                    BuiltInRegistries.ITEM.get(
                            new ResourceLocation(id)
                    );

            if (item instanceof GunUpgradeItem upgrade) {
                result.add(upgrade);
            }
        }

        return result;
    }

    /** 已占用改装槽数量。 */
    public static int getUsedSlots(ItemStack gun) {
        return getInstalledUpgrades(gun).size();
    }

    /** 当前枪械是否安装了指定改装件。 */
    public static boolean hasUpgrade(ItemStack gun, GunUpgradeItem upgrade) {
        CompoundTag tag = gun.getTag();
        if (tag == null || !tag.contains(TAG_UPGRADES, 9)) {
            return false;
        }

        String targetId =
                BuiltInRegistries.ITEM
                        .getKey(upgrade)
                        .toString();

        ListTag list = tag.getList(TAG_UPGRADES, 8);

        for (int i = 0; i < list.size(); ++i) {
            if (targetId.equals(list.getString(i))) {
                return true;
            }
        }

        return false;
    }

    /** 判断枪械是否拥有追踪行为。 */
    public static boolean hasTracking(ItemStack gun) {
        for (GunUpgradeItem upgrade : getInstalledUpgrades(gun)) {
            if (upgrade.enablesTracking()) {
                return true;
            }
        }

        return false;
    }

    /** 计算枪械安装所有改装件后的最终属性。 */
    public static GunStats getEffectiveStats(GunItem gun, ItemStack stack) {
        GunStats result = gun.getStats();

        for (GunUpgradeItem upgrade : getInstalledUpgrades(stack)) {
            result = upgrade.modifyStats(result);
        }

        return result;
    }

    /** 是否还有空余改装槽。 */
    public static boolean canInstall(ItemStack gun, GunUpgradeItem upgrade) {
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            return false;
        }

        return getUsedSlots(gun) < gunItem.getStats().modSlots();
    }

    /** 安装一个改装件。 */
    public static boolean install(ItemStack gun, GunUpgradeItem upgrade) {
        if (!canInstall(gun, upgrade)) {
            return false;
        }

        CompoundTag tag = gun.getOrCreateTag();
        ListTag list = tag.contains(TAG_UPGRADES, 9)
                ? tag.getList(TAG_UPGRADES, 8)
                : new ListTag();

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(upgrade);

        list.add(StringTag.valueOf(id.toString()));
        tag.put(TAG_UPGRADES, list);

        return true;
    }

    /** 卸下全部改装件。 */
    public static void removeAll(ItemStack gun) {
        CompoundTag tag = gun.getTag();

        if (tag != null) {
            tag.remove(TAG_UPGRADES);
        }
    }
}
