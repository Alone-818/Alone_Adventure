package Alone818.com.alone_adventure.events;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.reaper_scythe;
import Alone818.com.alone_adventure.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * 巨大镰刀事件处理器
 *
 * 核心机制：
 * 1. 每个物品的击杀记录独立存储在物品NBT中
 * 2. onLivingDeath：击杀生物后，将击杀记录写入物品NBT（每个物品独立）
 * 3. onLivingDamage：读取物品NBT中的击杀计数，为伤害添加加成
 *
 * 效果：
 * - 镰刀A击杀生物A → 镰刀A NBT记录1次 → 镰刀A伤害5+2=7
 * - 镰刀B击杀生物A → 镰刀B NBT记录1次 → 镰刀B伤害5+2=7
 * - 镰刀A和镰刀B互不影响
 */
@Mod.EventBusSubscriber(modid = Alone_adventure.MODID)
public class ReaperScytheEvent {

    private static final String NBT_TAG_DATA = "ReaperScytheData";

    /**
     * 在造成伤害前动态计算并添加击杀奖励伤害
     * 读取物品NBT中的击杀计数，每个物品独立
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        // 必须是服务端
        if (event.getEntity().level().isClientSide()) return;

        // 只有玩家攻击时才检查
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // 检查是否持有巨大镰刀
        ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
        if (!heldItem.is(ModItems.REAPER_SCYTHE.get())) return;

        // 检查物品NBT中是否有击杀记录
        if (!heldItem.hasTag() || !heldItem.getTag().contains(NBT_TAG_DATA)) return;

        CompoundTag scytheData = heldItem.getTag().getCompound(NBT_TAG_DATA);
        int killCount = scytheData.getInt("killCount");
        if (killCount <= 0) return;

        // 添加伤害加成：每击杀一种生物 +2
        int bonusDamage = killCount * reaper_scythe.DAMAGE_PER_NEW_KILL;
        event.setAmount(event.getAmount() + bonusDamage);
    }

    /**
     * 处理击杀事件：记录击杀的新种类到物品NBT
     * 每个物品的击杀记录独立存储，互不影响
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        // 必须是服务端
        if (event.getEntity().level().isClientSide()) return;

        // 只有玩家击杀生物时触发
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        LivingEntity entity = event.getEntity();

        // 检查玩家手中是否持有巨大镰刀
        ItemStack heldItem = serverPlayer.getItemInHand(serverPlayer.getUsedItemHand());
        if (!heldItem.is(ModItems.REAPER_SCYTHE.get())) return;

        // 获取生物的实体类型ID
        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityType == null) {
            entityType = new ResourceLocation(entity.getType().toString());
        }

        // 将击杀记录写入物品NBT（每个物品独立）
        updateScytheKillData(serverPlayer, heldItem, entityType);
    }

    /**
     * 更新镰刀的击杀数据到物品NBT
     */
    private static void updateScytheKillData(ServerPlayer player, ItemStack heldItem, ResourceLocation entityType) {
        String entityTypeStr = entityType.toString();

        // 从物品NBT读取现有的击杀类型
        CompoundTag itemTag = heldItem.getOrCreateTag();
        CompoundTag scytheData = new CompoundTag();

        if (itemTag.contains(NBT_TAG_DATA)) {
            scytheData = itemTag.getCompound(NBT_TAG_DATA);
        }

        // 读取已击杀的种类
        Set<String> killedTypes = new HashSet<>();
        if (scytheData.contains("killedTypes")) {
            String[] types = scytheData.getString("killedTypes").split(",");
            for (String type : types) {
                if (!type.isEmpty()) {
                    killedTypes.add(type);
                }
            }
        }

        // 已击杀过同类，直接返回
        if (killedTypes.contains(entityTypeStr)) {
            return;
        }

        // 是新种类，增加击杀计数
        int killCount = scytheData.getInt("killCount") + 1;
        scytheData.putInt("killCount", killCount);
        killedTypes.add(entityTypeStr);

        // 更新种类列表
        StringBuilder sb = new StringBuilder();
        for (String type : killedTypes) {
            if (sb.length() > 0) sb.append(",");
            sb.append(type);
        }
        scytheData.putString("killedTypes", sb.toString());

        // 写回物品NBT
        itemTag.put(NBT_TAG_DATA, scytheData);
    }
}