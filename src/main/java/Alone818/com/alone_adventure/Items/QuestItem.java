package Alone818.com.alone_adventure.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务物品模板 —— 可佩戴为契约饰品 + 完成任务后解锁的合成材料。
 *
 * <b>定位</b>：一件任务物品同时具备三重身份：
 * <ol>
 *   <li><b>契约饰品</b>——实现 {@link ICurioItem},自动可佩戴到契约槽
 *       （需把注册名加入 {@code data/curios/tags/items/contract.json}）</li>
 *   <li><b>任务载体</b>——构造时给定任务列表（{@link KillTask} 击杀计数 /
 *       {@link CollectTask} 收集判定），任务进度存在玩家持久 NBT
 *       （跨死亡、跨重登录），完成状态逐任务标记到<b>堆栈 NBT</b>（随物品同步）</li>
 *   <li><b>合成材料</b>——<b>全部任务完成后</b>堆栈打上 {@value #TAG_DONE} 标记，
 *       此时（且仅此时）可被配方用作材料；未完成的物品在配方里不被接受</li>
 * </ol>
 *
 * <b>任务判定与自愈</b>：物品在背包（{@link #inventoryTick}）或契约槽
 * （{@link #curioTick}）中每 {@value #EVAL_INTERVAL_TICKS} tick 服务端重估一次：
 * 收集类任务可回退（材料花掉后标记自动摘除），击杀进度持久累计不回退。
 * 物品离开玩家（箱子/掉落）后保持最后状态。
 *
 * <b>配方的材料门控</b>——用 {@link #material} 生成"已完成"成分，或 JSON 里写：
 * <pre>{@code
 * {
 *   "type": "forge:partial_nbt",
 *   "item": "alone_adventure:quest_contract",
 *   "nbt": "{QuestDone:1b}"
 * }
 * }</pre>
 *
 * <b>节省内存的设计</b>：无能力系统、无额外网络包——击杀进度是玩家持久 NBT 里
 * 的 int，完成状态是堆栈 NBT 的 byte；注册表仅一份任务列表（不可变），
 * 评估按固定间隔节流。
 */
public class QuestItem extends Item implements ICurioItem {

    /** NBT 标签：全部任务完成（= 可作为合成材料） */
    public static final String TAG_DONE = "QuestDone";
    /** NBT 标签：各任务完成状态（任务 id → 1b） */
    public static final String TAG_QUEST = "Quest";
    /** 玩家持久 NBT（PlayerPersisted）下的击杀进度根标签 */
    public static final String TAG_PROGRESS_ROOT = "AloneQuest";

    /** 评估节流间隔（tick） */
    private static final int EVAL_INTERVAL_TICKS = 20;

    /** 已注册的任务物品（击杀事件遍历用，构造即登记） */
    private static final List<QuestItem> REGISTERED = new ArrayList<>();

    /** 本物品的任务列表（不可变） */
    private final List<Task> tasks;

    public QuestItem(Properties properties, Task... tasks) {
        super(properties);
        this.tasks = List.of(tasks);
        REGISTERED.add(this);
    }

    /** 已注册的全部任务物品 */
    public static List<QuestItem> getRegistered() {
        return REGISTERED;
    }

    /** 本物品的注册名（进度键与任务翻译键使用） */
    public String questId() {
        return BuiltInRegistries.ITEM.getKey(this).getPath();
    }

    /** 本物品的任务列表（不可变） */
    public List<Task> getTasks() {
        return tasks;
    }

    /** 堆栈是否已达成全部任务（= 可作为合成材料） */
    public static boolean isQuestDone(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_DONE);
    }

    /**
     * "已完成"合成成分：仅匹配全部任务完成的本物品堆栈。
     * 代码注册配方用；JSON 配方见类注释（forge:partial_nbt）。
     */
    public static Ingredient material(ItemLike questItem) {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(TAG_DONE, true);
        return PartialNBTIngredient.of(questItem, nbt);
    }

    // ===== 任务模板 =====

    /** 一项任务：服务端判定完成；完成状态逐任务标记到堆栈 NBT */
    public abstract static class Task {

        private final String id;

        protected Task(String id) {
            this.id = id;
        }

        /** 任务 id（进度键 / 堆栈 NBT 键 / 翻译键尾段） */
        public String id() {
            return id;
        }

        /** 任务名（翻译键自动派生：{@code quest.alone_adventure.<物品id>.<任务id>}） */
        public Component title(String questId) {
            return Component.translatable("quest.alone_adventure." + questId + "." + id);
        }

        /** 服务端判定本任务当前是否完成 */
        public abstract boolean test(ServerPlayer player, String questId);

        /** 击杀事件是否命中本任务（仅击杀计数类实现） */
        public boolean matchesKill(LivingEntity victim) {
            return false;
        }
    }

    /** 击杀任务：累计击杀指定类型生物 N 只（进度持久，跨死亡/重登录） */
    public static final class KillTask extends Task {

        private final EntityTypeHolder target;
        private final int count;

        private KillTask(String id, EntityTypeHolder target, int count) {
            super(id);
            this.target = target;
            this.count = Math.max(1, count);
        }

        /** 按实体类型：{@code QuestItem.KillTask.of("kill_zombie", EntityType.ZOMBIE, 10)} */
        public static KillTask of(String id, net.minecraft.world.entity.EntityType<?> type, int count) {
            return new KillTask(id, EntityTypeHolder.ofType(type), count);
        }

        /** 按实体类型标签（如 {@code EntityTypeTags.UNDEAD}） */
        public static KillTask of(String id, TagKey<net.minecraft.world.entity.EntityType<?>> tag, int count) {
            return new KillTask(id, EntityTypeHolder.ofTag(tag), count);
        }

        @Override
        public boolean test(ServerPlayer player, String questId) {
            return killProgress(player, questId, id()) >= count;
        }

        @Override
        public boolean matchesKill(LivingEntity victim) {
            return target.matches(victim);
        }

        /** 击杀进度文本（动作栏反馈，QuestEvent 调用） */
        public Component progressText(int current, String questId) {
            return Component.translatable("message.alone_adventure.quest.kill_progress",
                    title(questId), current, count);
        }
    }

    /** 收集任务：背包中持有指定物品 N 个即完成（可回退：花掉后标记自动摘除） */
    public static final class CollectTask extends Task {

        private final Item item;
        private final int count;

        private CollectTask(String id, Item item, int count) {
            super(id);
            this.item = item;
            this.count = Math.max(1, count);
        }

        /** {@code QuestItem.CollectTask.of("collect_beetroot", ModItems.GOLDEN_BEETROOT.get(), 3)} */
        public static CollectTask of(String id, ItemLike item, int count) {
            return new CollectTask(id, item.asItem(), count);
        }

        @Override
        public boolean test(ServerPlayer player, String questId) {
            return player.getInventory().countItem(item) >= count;
        }
    }

    /** 击杀目标（实体类型或类型标签，二选一） */
    private record EntityTypeHolder(
            @Nullable net.minecraft.world.entity.EntityType<?> type,
            @Nullable TagKey<net.minecraft.world.entity.EntityType<?>> tag) {

        static EntityTypeHolder ofType(net.minecraft.world.entity.EntityType<?> type) {
            return new EntityTypeHolder(type, null);
        }

        static EntityTypeHolder ofTag(TagKey<net.minecraft.world.entity.EntityType<?>> tag) {
            return new EntityTypeHolder(null, tag);
        }

        boolean matches(LivingEntity victim) {
            return type != null ? victim.getType() == type : victim.getType().is(tag);
        }
    }

    // ===== 评估（背包内 / 契约槽上，每 20 tick 服务端） =====

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        // 契约槽不触发 inventoryTick，佩戴时的评估走 curioTick；这里覆盖背包/快捷栏
        if (entity instanceof ServerPlayer player && entity.tickCount % EVAL_INTERVAL_TICKS == 0) {
            evaluate(player, stack);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!entity.level().isClientSide && entity instanceof ServerPlayer player
                && entity.tickCount % EVAL_INTERVAL_TICKS == 0) {
            evaluate(player, stack);
        }
    }

    /**
     * 重估全部任务：逐任务写堆栈 NBT 标记（随物品同步，tooltip 直接可读），
     * 全部完成时打上 {@value #TAG_DONE}（材料解锁）并播报；
     * 进度回退（收集材料花掉）时标记自愈摘除。
     */
    private void evaluate(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag quest = tag.getCompound(TAG_QUEST);
        boolean all = true;

        for (Task task : tasks) {
            boolean done = task.test(player, questId());
            if (done != quest.getBoolean(task.id())) {
                quest.putBoolean(task.id(), done);
                if (done) {
                    player.displayClientMessage(Component.translatable(
                            "message.alone_adventure.quest.task_done", task.title(questId())), true);
                }
            }
            if (!done) all = false;
        }
        tag.put(TAG_QUEST, quest);

        if (all) {
            if (!tag.getBoolean(TAG_DONE)) {
                tag.putBoolean(TAG_DONE, true);
                player.level().playSound(null, player, SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.8F, 1.0F);
                if (player.level() instanceof ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 1.2D, player.getZ(),
                            12, 0.5D, 0.5D, 0.5D, 0.05D);
                }
                player.displayClientMessage(Component.translatable(
                        "message.alone_adventure.quest.all_done"), false);
            }
        } else if (tag.getBoolean(TAG_DONE)) {
            tag.putBoolean(TAG_DONE, false);
        }
    }

    // ===== 击杀进度存取（玩家持久 NBT，跨死亡/重登录） =====

    /** 读取指定任务物品/任务的击杀进度 */
    public static int killProgress(ServerPlayer player, String questId, String taskId) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                .getCompound(TAG_PROGRESS_ROOT)
                .getCompound(questId)
                .getInt(taskId);
    }

    /** 击杀进度 +1 并落盘，返回累加后的值（QuestEvent 调用） */
    public static int addKillProgress(ServerPlayer player, String questId, String taskId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag root = persisted.getCompound(TAG_PROGRESS_ROOT);
        CompoundTag perQuest = root.getCompound(questId);

        int now = perQuest.getInt(taskId) + 1;
        perQuest.putInt(taskId, now);
        root.put(questId, perQuest);
        persisted.put(TAG_PROGRESS_ROOT, root);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return now;
    }

    // ===== 展示 =====

    /** 契约达成后附魔光效（一眼可辨材料可用） */
    @Override
    public boolean isFoil(ItemStack stack) {
        return isQuestDone(stack);
    }

    /** 契约达成后稀有度升为史诗 */
    @Override
    public Rarity getRarity(ItemStack stack) {
        return isQuestDone(stack) ? Rarity.EPIC : super.getRarity(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            CompoundTag tag = stack.getTag();
            CompoundTag quest = tag != null ? tag.getCompound(TAG_QUEST) : new CompoundTag();
            for (Task task : tasks) {
                boolean done = quest.getBoolean(task.id());
                tooltip.add(Component.literal(done ? "✔ " : "✘ ")
                        .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                        .append(task.title(questId())));
            }
            tooltip.add(isQuestDone(stack)
                    ? Component.translatable("tooltip.alone_adventure.quest.material_ready")
                            .withStyle(ChatFormatting.GREEN)
                    : Component.translatable("tooltip.alone_adventure.quest.material_locked")
                            .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.alone_adventure." + questId() + ".tooltip.desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.alone_adventure.press_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
