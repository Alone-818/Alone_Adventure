package Alone818.com.alone_adventure.Items.ImperialItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;

public class decapitation_axe extends AxeItem {


    public decapitation_axe() {
        super(
                Tiers.DIAMOND,
                9,       // 总伤害 10
                -3.8F,   // 攻速 0.2
                new Properties()
        );
    }


    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {

        // 攻击者必须是玩家
        if (attacker instanceof Player player) {


            // 目标死亡时触发
            if (!target.level().isClientSide &&
                    target.getHealth() <= 0) {


                ItemStack head = getHead(target);


                if (!head.isEmpty()) {


                    ItemEntity item =
                            new ItemEntity(
                                    target.level(),
                                    target.getX(),
                                    target.getY(),
                                    target.getZ(),
                                    head
                            );


                    target.level().addFreshEntity(item);
                }
            }
        }


        return super.hurtEnemy(
                stack,
                target,
                attacker
        );
    }



    private ItemStack getHead(LivingEntity entity) {


        // 玩家头颅
        if(entity instanceof ServerPlayer player){

            ItemStack head =
                    new ItemStack(Items.PLAYER_HEAD);


            CompoundTag owner =
                    new CompoundTag();

            owner.putString(
                    "Name",
                    player.getGameProfile().getName()
            );


            head.getOrCreateTag()
                    .put(
                            "SkullOwner",
                            owner
                    );


            return head;
        }



        // 僵尸
        if(entity instanceof Zombie)
            return new ItemStack(
                    Items.ZOMBIE_HEAD
            );


        // 骷髅
        if(entity instanceof Skeleton)
            return new ItemStack(
                    Items.SKELETON_SKULL
            );


        // 凋零骷髅
        if(entity instanceof WitherSkeleton)
            return new ItemStack(
                    Items.WITHER_SKELETON_SKULL
            );


        // 苦力怕
        if(entity instanceof Creeper)
            return new ItemStack(
                    Items.CREEPER_HEAD
            );


        // 猪灵
        if(entity instanceof Piglin)
            return new ItemStack(
                    Items.PIGLIN_HEAD
            );


        return ItemStack.EMPTY;
    }
    @Override
    public void appendHoverText(
            ItemStack stack,
            net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag
    ) {

        tooltip.add(
                net.minecraft.network.chat.Component.translatable(
                        "tooltip.alone_adventure.decapitation_axe"
                )
        );

        super.appendHoverText(stack, level, tooltip, flag);
    }
}