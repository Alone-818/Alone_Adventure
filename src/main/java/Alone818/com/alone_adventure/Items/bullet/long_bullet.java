package Alone818.com.alone_adventure.Items.bullet;

import Alone818.com.alone_adventure.Items.gun.AmmoItem;
import net.minecraft.world.item.Rarity;

/**
 * 长子弹 —— 步枪弹药（高威力、远射程、强穿透）。
 * 无特殊激发效果，作为特种弹药制作的对照模板。
 */
public class long_bullet extends AmmoItem {

    public long_bullet() {
        super(new Properties().rarity(Rarity.COMMON));
    }
}
