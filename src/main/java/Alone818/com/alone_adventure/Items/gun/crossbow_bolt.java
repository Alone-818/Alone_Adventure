package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.item.Rarity;

/**
 * 弩箭弹药 —— 步枪可切换装填的箭形弹药（对护甲更有效等潜力由特种弹药扩展）。
 * 无特殊激发效果，作为特种弹药制作的对照模板。
 */
public class crossbow_bolt extends AmmoItem {

    public crossbow_bolt() {
        super(new Properties().rarity(Rarity.COMMON));
    }
}
