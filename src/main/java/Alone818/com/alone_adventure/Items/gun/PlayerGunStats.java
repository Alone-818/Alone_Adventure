package Alone818.com.alone_adventure.Items.gun;

import net.minecraft.world.entity.player.Player;

/**
 * 玩家枪械加成属性 —— 由饰品等来源提供，叠加到枪械的 {@link GunStats} 之上。
 *
 * 默认全部为"无变化"：加成型字段默认 0，倍率型字段默认 1。
 * 未注册任何 {@link IGunStatProvider} 时（现状），collect 结果即默认值，
 * 后续饰品实现 {@link IGunStatProvider} 并向 {@link GunStatRegistry} 注册即可生效。
 *
 * 字段语义：
 * <ul>
 *   <li>{@code damageBonus} —— 子弹伤害 +X 点</li>
 *   <li>{@code bulletSpeedBonus} —— 子弹速度 +X 格/tick</li>
 *   <li>{@code penetrationBonus} —— 穿透能力 +X 个实体</li>
 *   <li>{@code ricochetBonus} —— 反弹能力 +X 次</li>
 *   <li>{@code knockbackBonus} —— 击退能力 +X</li>
 *   <li>{@code rangeBonus} —— 有效射程 +X 格</li>
 *   <li>{@code fireRateMultiplier} —— 射速倍率（开火间隔 ÷ 该值，&gt;1 更快）</li>
 *   <li>{@code reloadSpeedMultiplier} —— 装填速度倍率（装填时间 ÷ 该值，&gt;1 更快）</li>
 *   <li>{@code recoilReduction} —— 后坐力减免比例（0~1，0.3 = 减免 30%）</li>
 *   <li>{@code spreadReduction} —— 射击偏移减免比例（0~1）</li>
 * </ul>
 */
public class PlayerGunStats {

    /** 子弹伤害加成（点） */
    public float damageBonus = 0.0F;
    /** 子弹速度加成（格/tick） */
    public float bulletSpeedBonus = 0.0F;
    /** 穿透能力加成（实体数） */
    public int penetrationBonus = 0;
    /** 反弹能力加成（次数） */
    public int ricochetBonus = 0;
    /** 击退能力加成 */
    public float knockbackBonus = 0.0F;
    /** 有效射程加成（格） */
    public float rangeBonus = 0.0F;
    /** 射速倍率（1 = 不变） */
    public float fireRateMultiplier = 1.0F;
    /** 装填速度倍率（1 = 不变） */
    public float reloadSpeedMultiplier = 1.0F;
    /** 后坐力减免（0~1） */
    public float recoilReduction = 0.0F;
    /** 射击偏移减免（0~1） */
    public float spreadReduction = 0.0F;

    /** 把另一份加成合并到本份上（对应字段相加/相乘，减免取相加后钳制） */
    public void merge(PlayerGunStats other) {
        this.damageBonus += other.damageBonus;
        this.bulletSpeedBonus += other.bulletSpeedBonus;
        this.penetrationBonus += other.penetrationBonus;
        this.ricochetBonus += other.ricochetBonus;
        this.knockbackBonus += other.knockbackBonus;
        this.rangeBonus += other.rangeBonus;
        this.fireRateMultiplier *= other.fireRateMultiplier;
        this.reloadSpeedMultiplier *= other.reloadSpeedMultiplier;
        this.recoilReduction = Math.min(1.0F, this.recoilReduction + other.recoilReduction);
        this.spreadReduction = Math.min(1.0F, this.spreadReduction + other.spreadReduction);
    }

    /** 默认（无加成）实例 */
    public static PlayerGunStats neutral() {
        return new PlayerGunStats();
    }
}
