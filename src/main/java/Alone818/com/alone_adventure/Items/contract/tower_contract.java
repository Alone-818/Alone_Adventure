package Alone818.com.alone_adventure.Items.contract;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import Alone818.com.alone_adventure.init.ModItems;


public class tower_contract extends QuestItem {

    public tower_contract() {

        super(new Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC),


                QuestItem.PositionTask.of(
                        "reach_high_place",
                        QuestItem.AxisType.Y,
                        200
                ),


                // 击杀凋灵
                QuestItem.KillTask.of(
                        "kill_wither",
                        EntityType.WITHER,
                        1
                ),


                // 提交怪物残灰
                QuestItem.CollectTask.of(
                        "collect_monster_ash",
                        ModItems.MONSTER_ASH.get(),
                        8
                ),


                // 提交血脉碎片
                QuestItem.CollectTask.of(
                        "collect_blood_shard",
                        ModItems.BLOOD_SHARD.get(),
                        8
                )
        );
    }
}