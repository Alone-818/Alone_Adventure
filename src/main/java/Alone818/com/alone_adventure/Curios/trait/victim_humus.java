package Alone818.com.alone_adventure.Curios.trait;

import Alone818.com.alone_adventure.init.ModEffects;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

/**
 * 受害者腐殖 - 特质槽位饰品
 *
 * 描述："如果大地不能缓和您的愤怒，就请拿走我的血与骨。"
 *
 * 效果：
 * - 攻击时损失的红色生命的 50% 转换为可叠加黄心
 */
public class victim_humus extends Item implements ICurioItem {

    /** NBT 键：黄心叠加层数 */
    public static final String TAG_ABSORPTION_LAYERS = "HumusAbsorptionLayers";
    /** 每层黄心提供的吸收值 */
    public static final float ABSORPTION_PER_LAYER = 2.0f;
    /** 黄心上限 */
    public static final int MAX_LAYERS = 20;

    public victim_humus() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /**
     * 处理伤害事件：将损失的红色生命的 50% 转换为黄心
     *
     * @param player 玩家
     * @param damage 受到的伤害
     * @return 实际造成的红心伤害
     */
    public static float onHurt(ServerPlayer player, float damage) {
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.VICTIM_HUMUS.get());
        if (curioOpt.isEmpty()) return damage;

        ItemStack stack = curioOpt.get().stack();
        CompoundTag tag = stack.getOrCreateTag();

        // 计算会损失的红心（不能超过当前红心）
        float redHeartLost = Math.min(damage, player.getHealth());

        // 50% 转换为黄心
        float absorptionGain = redHeartLost * 0.5f;

        // 转换为黄心层数（每层 2 点吸收）
        int layersToAdd = (int) (absorptionGain / ABSORPTION_PER_LAYER);

        if (layersToAdd > 0) {
            int currentLayers = tag.getInt(TAG_ABSORPTION_LAYERS);
            int newLayers = Math.min(currentLayers + layersToAdd, MAX_LAYERS);
            tag.putInt(TAG_ABSORPTION_LAYERS, newLayers);

            // 更新吸收效果
            float totalAbsorption = newLayers * ABSORPTION_PER_LAYER;
            player.setAbsorptionAmount(totalAbsorption);

            stack.setTag(tag);

            // 更新 Curios 槽位
            SlotResult slotResult = curioOpt.get();
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.setEquippedCurio(slotResult.slotContext().identifier(),
                            slotResult.slotContext().index(), stack));
        }

        return damage;
    }

    /**
     * 判断玩家是否佩戴受害者腐殖
     */
    public static boolean isWearing(Player player) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.VICTIM_HUMUS.get())
                .isPresent();
    }

    /**
     * 获取当前黄心层数
     */
    public static int getAbsorptionLayers(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getInt(TAG_ABSORPTION_LAYERS);
    }

    /**
     * 获取玩家当前的黄心层数（从饰品 NBT 读取）
     */
    public static int getAbsorptionLayers(ServerPlayer player) {
        Optional<SlotResult> curioOpt = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.VICTIM_HUMUS.get());
        if (curioOpt.isEmpty()) return 0;
        return getAbsorptionLayers(curioOpt.get().stack());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.alone_adventure.victim_humus.tooltip.desc")
                .withStyle(ChatFormatting.DARK_GREEN));

        if (level != null && !level.isClientSide()) {
            int layers = getAbsorptionLayers(stack);
            float absorption = layers * ABSORPTION_PER_LAYER;
            tooltip.add(Component.translatable("item.alone_adventure.victim_humus.tooltip.layers",
                            layers, MAX_LAYERS, (int) absorption)
                    .withStyle(ChatFormatting.GREEN));
        }

        tooltip.add(Component.translatable("item.alone_adventure.victim_humus.tooltip.mechanism")
                .withStyle(ChatFormatting.BLUE));
    }
}
