package Alone818.com.alone_adventure.Items.gun;

import Alone818.com.alone_adventure.Items.BulletProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 弹药基类 —— 所有枪械弹药的模板（长子弹 / 霰弹 / 短子弹 / 弩箭弹药…）。
 *
 * <b>特种弹药事件钩子</b>（子类覆写即可制作特种弹药）：
 * <ul>
 *   <li>{@link #onFired} —— 子弹激发后（开火时、子弹实体生成后）调用：
 *       可改写弹体数值、向 {@link BulletProjectile#getAmmoData() 弹体 NBT} 写入
 *       特种弹药数据（爆炸标记、元素附加、计时器等）</li>
 *   <li>{@link #onBulletHit} —— 本弹药射出的子弹命中实体时调用：
 *       可读取弹体 NBT 结算特种效果（爆炸、点燃、上毒等），
 *       与枪械自身的击中钩子（GunItem#onBulletHitEntity）独立触发</li>
 * </ul>
 *
 * 弹体实体类型固定为 {@link BulletProjectile}（不随弹药变化），
 * 弹药差异全部通过弹体 NBT 标签携带（见 {@code BulletProjectile#getAmmoData}）。
 */
public class AmmoItem extends Item {

    public AmmoItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    /**
     * 子弹激发钩子：本弹药被开火射出时（服务端，子弹实体已生成、即将入世界）调用。
     * 特种弹药在此改写弹体属性或写入弹体 NBT。
     */
    public void onFired(ServerPlayer shooter, ItemStack gun, BulletProjectile bullet) {
    }

    /**
     * 命中钩子：本弹药射出的子弹命中实体时（服务端，伤害结算前）调用。
     * damage 为经射程衰减后的实际伤害值。
     */
    public void onBulletHit(ServerLevel level, BulletProjectile bullet,
                             Entity hit, float damage) {
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.ammo.tooltip.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
