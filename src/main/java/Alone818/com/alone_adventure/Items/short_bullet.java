package Alone818.com.alone_adventure.Items;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import net.minecraft.world.item.Rarity;

/**
 * 短子弹 —— 手枪弹药（低威力、快射速、轻后坐）。
 * 无特殊激发效果，作为特种弹药制作的对照模板。
 */
public class short_bullet extends AmmoItem {

    public short_bullet() {
        super(new Properties().rarity(Rarity.COMMON));
    }
}
