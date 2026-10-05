package Alone818.com.alone_adventure.faction;

import net.minecraft.network.chat.Component;

/**
 * 派系生物的攻击方式。
 *
 * MELEE  近战：贴脸攻击
 * RANGED 远程：拉开距离射击
 *
 * 以后扩展攻击行为时按这个分类挂对应 AI。
 */
public enum AttackType {

    /** 近战 */
    MELEE,

    /** 远程 */
    RANGED;

    /**
     * 显示名。
     */
    public Component getDisplayName() {

        return Component.translatable(
                "attack_type.alone_adventure."
                        + name().toLowerCase()
        );
    }
}
