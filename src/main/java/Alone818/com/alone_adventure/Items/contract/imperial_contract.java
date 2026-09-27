package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;

public class imperial_contract extends QuestItem {

    public imperial_contract() {
        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC),

                // 击杀僵尸猪人 x20
                QuestItem.KillTask.of(
                        "kill_zombie_pigman",
                        EntityType.ZOMBIFIED_PIGLIN,
                        20
                ),

                // 击杀坚守者 x1
                QuestItem.KillTask.of(
                        "kill_warden",
                        EntityType.WARDEN,
                        1
                ),

                // 到达沙漠群系
                QuestItem.BiomeTask.of(
                        "reach_desert",
                        "minecraft:desert"
                ),

                // 提交下届合金碎片 x4
                QuestItem.CollectTask.of(
                        "collect_netherite_scrap",
                        Items.NETHERITE_SCRAP,
                        4
                )
        );
    }
}
