package Alone818.com.alone_adventure.Items.gun;


public class PlayerGunStats {


    // 伤害
    public float damageBonus = 0.0F;


    // 子弹速度
    public float bulletSpeedBonus = 0.0F;


    // 穿透
    public int penetrationBonus = 0;


    // 反弹
    public int ricochetBonus = 0;


    // 击退
    public float knockbackBonus = 0.0F;


    // 射程
    public float rangeBonus = 0.0F;



    /**
     * 开火速度倍率
     * 1.0 = 正常
     * 1.3 = 快30%
     */
    public float fireRateMultiplier = 1.0F;



    /**
     * 装填速度倍率
     */
    public float reloadSpeedMultiplier = 1.0F;



    /**
     * 后坐力减少
     */
    public float recoilReduction = 0.0F;



    /**
     * 扩散减少
     */
    public float spreadReduction = 0.0F;



    /**
     * 扩散倍率
     *
     * 1.0 正常
     * 1.2 增加20%
     */
    public float spreadMultiplier = 1.0F;




    public void merge(PlayerGunStats other){


        this.damageBonus += other.damageBonus;


        this.bulletSpeedBonus += other.bulletSpeedBonus;


        this.penetrationBonus += other.penetrationBonus;


        this.ricochetBonus += other.ricochetBonus;


        this.knockbackBonus += other.knockbackBonus;


        this.rangeBonus += other.rangeBonus;



        this.fireRateMultiplier *= other.fireRateMultiplier;


        this.reloadSpeedMultiplier *= other.reloadSpeedMultiplier;



        this.spreadMultiplier *= other.spreadMultiplier;



        this.recoilReduction =
                Math.min(
                        1.0F,
                        this.recoilReduction
                                + other.recoilReduction
                );



        this.spreadReduction =
                Math.min(
                        1.0F,
                        this.spreadReduction
                                + other.spreadReduction
                );

    }




    public static PlayerGunStats neutral(){

        return new PlayerGunStats();

    }

}