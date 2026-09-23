package Alone818.com.alone_adventure.Items.contract;

import Alone818.com.alone_adventure.Alone_adventure;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import top.theillusivec4.curios.api.CuriosApi;
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
 *       {@link CollectTask} 收集判定），任务进度存于<b>物品堆栈 NBT</b>，
 *       每个契约饰品独立记录各自的进度，换掉后进度留在原物品上</li>
 *   <li><b>合成材料</b>——<b>全部任务完成后</b>堆栈打上 {@value #TAG_DONE} 标记，
 *       此时（且仅此时）可被配方用作材料；未完成的物品在配方里不被接受</li>
 * </ol>
 *
 * <b>任务判定与自愈</b>：仅在契约槽佩戴时，每 {@value #EVAL_INTERVAL_TICKS} tick 服务端重估一次：
 * 收集类任务可回退（材料花掉后标记自动摘除），击杀进度随物品堆栈 NBT 独立记录。
 * 物品离开玩家（箱子/掉落）后保持最后状态。
 *
 * <b>材料提交</b>——右键点击：将材料（副手）消耗后统计任务进度。
 *   佩戴契约饰品在主手，所需材料在副手，右键点击完成材料提交。
 *   材料提交后，周期评估会在下一次重估时自动检查完成状态。
 *
 * <b>配方的材料门控</b>——用 {@link #material} 生成"已完成"成分，或 JSON 里写：
 * <pre>{@code
 * {
 *   "type": "forge:partial_nbt",
 *   "item": "alone_adventure:resurrection_contract",
 *   "nbt": "{QuestDone:1b}"
 * }
 * }</pre>
 *
 * <b>设计</b>：击杀进度存储在物品堆栈 NBT 中，每个契约饰品实例独立。
 * 换掉契约饰品后，原物品的击杀进度保持不变，新饰品从零开始。
 */
public class QuestItem extends Item implements ICurioItem {

    /** NBT 标签：全部任务完成（= 可作为合成材料） */
    public static final String TAG_DONE = "QuestDone";
    /** NBT 标签：各任务完成状态（任务 id -> 1b） */
    public static final String TAG_QUEST = "Quest";
    /** NBT 标签：击杀进度（questId#taskId -> progress） */
    public static final String TAG_KILL_PROGRESS = "KillProgress";
    /** NBT 标签：收集进度（questId#taskId -> progress），存于物品堆栈 NBT */
    public static final String TAG_COLLECT_PROGRESS = "CollectProgress";
    /** 玩家持久 NBT 下的击杀进度根标签 */
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

    /** 判断玩家是否佩戴指定物品（契约槽） */
    public static boolean isWearing(Player player, ItemStack stack) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack.getItem())
                .isPresent();
    }

    /** 判断玩家是否佩戴了指定的任务物品 */
    public static boolean isWearing(Player player, Item questItem) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, questItem)
                .isPresent();
    }

    /** 获取玩家当前佩戴的对应任务物品堆栈 */
    public static ItemStack getPlayerStack(Player player, Item questItem) {
        return CuriosApi.getCuriosHelper()
                .findFirstCurio(player, questItem)
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
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

    /** 右键点击：消耗副手材料，统计任务进度。
     * 佩戴契约饰品在主手，所需材料在副手，右键点击完成材料提交。
     * 优先级最高：仅当副手物品与任务要求的收集物品匹配时才消耗。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ItemStack questStack = player.getItemInHand(hand);
        if (!(questStack.getItem() instanceof QuestItem questItem)) return InteractionResultHolder.pass(questStack);

        // 获取副手中的物品（作为材料）
        ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhandStack.isEmpty()) return InteractionResultHolder.pass(questStack);

        CompoundTag tag = questStack.getOrCreateTag();
        CompoundTag quest = tag.getCompound(TAG_QUEST);
        boolean anyProgress = false;

        for (Task task : questItem.tasks) {
            if (quest.getBoolean(task.id())) continue; // 已完成的任务跳过

            if (task instanceof CollectTask collect && collect.item == offhandStack.getItem()) {
                // 消耗副手材料
                offhandStack.shrink(1);
                // 累计进度到物品堆栈 NBT（每个契约饰品独立）
                int now = collect.submitProgress(questStack, task.id());
                anyProgress = true;
                // 显示提交进度
                player.displayClientMessage(Component.translatable("message.alone_adventure.quest.submitted_progress",
                        task.title(questItem.questId()), now, collect.count), true);
            }
        }

        // 提交后检查全部任务是否完成，打上完成标记
        checkAllDone((ServerPlayer) player, questStack);

        if (anyProgress) {
            player.displayClientMessage(Component.translatable("message.alone_adventure.quest.submitted"), true);
        }

        return InteractionResultHolder.success(questStack);
    }

    /** 任务条目：可判定（服务端周期评估）、可展示（tooltip / 进度文本） */
    public static abstract class Task {

        private final String id;

        protected Task(String id) {
            this.id = id;
        }

        /** 任务 id（堆栈 NBT 标记键 / 翻译键尾段） */
        public String id() {
            return id;
        }

        /** 服务端判定任务是否达成（questId = 所属任务物品的注册名） */
        public abstract boolean test(ServerPlayer player, String questId, Item owningQuestItem);

        /** 展示标题（键：quest.<modid>.<questId>.<id>） */
        public Component title(String questId) {
            return Component.translatable(
                    "quest." + Alone_adventure.MODID + "." + questId + "." + id);
        }
    }

    /** 收集任务：右键点击提交指定材料，进度存于物品堆栈 NBT 中，每个契约饰品独立 */
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

        /**
         * 提交一次收集材料，累加进度到物品堆栈 NBT。
         * 进度存储在佩戴的契约饰品上，每个饰品独立记录。
         */
        public int submitProgress(ItemStack questStack, String taskId) {
            CompoundTag tag = questStack.getOrCreateTag();
            CompoundTag collectProgress = tag.getCompound(TAG_COLLECT_PROGRESS);

            int now = collectProgress.getInt(taskId) + 1;
            collectProgress.putInt(taskId, now);
            tag.put(TAG_COLLECT_PROGRESS, collectProgress);

            return now;
        }

        /** 从物品堆栈 NBT 读取收集进度 */
        public static int getCollectProgress(ItemStack stack, String taskId) {
            CompoundTag collectProgress = stack.getOrCreateTag().getCompound(TAG_COLLECT_PROGRESS);
            return collectProgress.getInt(taskId);
        }

        @Override
        public boolean test(ServerPlayer player, String questId, Item owningQuestItem) {
            // 读取物品堆栈 NBT 中的收集进度
            ItemStack stack = getPlayerStack(player, owningQuestItem);
            if (stack.isEmpty()) return false;
            return getCollectProgress(stack, id()) >= count;
        }
    }

    /** 击杀任务：佩戴契约饰品期间击杀目标 ×N；进度存储在物品堆栈 NBT 中，每个饰品独立 */
    public static final class KillTask extends Task {

        private final EntityTypeHolder target;
        private final int count;

        private KillTask(String id, EntityTypeHolder target, int count) {
            super(id);
            this.target = target;
            this.count = Math.max(1, count);
        }

        /** {@code QuestItem.KillTask.of("kill_zombie", EntityType.ZOMBIE, 10)} */
        public static KillTask of(String id, net.minecraft.world.entity.EntityType<?> type, int count) {
            return new KillTask(id, EntityTypeHolder.ofType(type), count);
        }

        /** 按实体类型标签批量指定击杀目标 */
        public static KillTask ofTag(String id, TagKey<net.minecraft.world.entity.EntityType<?>> tag, int count) {
            return new KillTask(id, EntityTypeHolder.ofTag(tag), count);
        }

        /** 击杀事件结算用：受害者是否为本任务目标（QuestEvent 调用） */
        public boolean matchesKill(LivingEntity victim) {
            return target.matches(victim);
        }

        /** 动作栏进度文本："任务进度：%s（%d/%d）"（QuestEvent 调用） */
        public Component progressText(int now, String questId) {
            return Component.translatable("message.alone_adventure.quest.kill_progress",
                    title(questId), now, count);
        }

        @Override
        public boolean test(ServerPlayer player, String questId, Item owningQuestItem) {
            // 通过契约饰品实例获取对应的堆栈，读取 NBT 中的击杀进度
            ItemStack stack = getPlayerStack(player, owningQuestItem);
            if (stack.isEmpty()) return false;
            return killProgress(stack, id()) >= count;
        }

        /** 累加击杀进度到物品堆栈 NBT */
        public int addKillProgress(ItemStack stack) {
            return QuestItem.addKillProgress(stack, id());
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

    // ===== 评估（仅契约槽佩戴时，每 20 tick 服务端） =====

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // 仅在契约槽佩戴时激活任务（通过 curioTick 触发）
        // 此实现不再在背包中激活任务
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
     * 重估全部任务：此方法现在为空实现，因为所有任务进度更新都通过手动右键提交完成
     * 评估间隔仍然保留，但不再执行任何任务状态检测
     */
    private void evaluate(ServerPlayer player, ItemStack stack) {
        checkAllDone(player, stack);
    }

    /**
     * 提交后检查所有任务是否已完成，若全部完成则打上完成标记并播报
     */
    private void checkAllDone(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag quest = tag.getCompound(TAG_QUEST);
        boolean all = true;

        for (Task task : tasks) {
            // 根据任务类型分别检查完成状态
            boolean done;
            if (task instanceof CollectTask collect) {
                // 收集任务：从物品堆栈 NBT 读取进度
                done = CollectTask.getCollectProgress(stack, task.id()) >= collect.count;
            } else if (task instanceof KillTask kill) {
                // 击杀任务：从物品堆栈 NBT 读取进度
                done = killProgress(stack, task.id()) >= kill.count;
            } else {
                done = false;
            }

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

    // ===== 击杀进度存取（物品堆栈 NBT，换饰品即重置） =====

    /** 从物品堆栈 NBT 读取击杀进度 */
    public static int killProgress(ServerPlayer player, String questId, String taskId) {
        // 此方法不再使用，仅保留接口兼容
        return 0;
    }

    /** 从物品堆栈 NBT 读取击杀进度（传入栈） */
    public static int killProgress(ItemStack stack, String taskId) {
        CompoundTag killProgressTag = stack.getOrCreateTag().getCompound(TAG_KILL_PROGRESS);
        return killProgressTag.getInt(taskId);
    }

    /** 在物品堆栈 NBT 中累加击杀进度 */
    public static int addKillProgress(ItemStack stack, String taskId) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag killProgressTag = tag.getCompound(TAG_KILL_PROGRESS);

        int now = killProgressTag.getInt(taskId) + 1;
        killProgressTag.putInt(taskId, now);
        tag.put(TAG_KILL_PROGRESS, killProgressTag);

        return now;
    }

    /** 测试击杀任务达成：读取 NBT 中的进度 */
    public static boolean killTaskDone(ItemStack stack, String taskId, int requiredCount) {
        return killProgress(stack, taskId) >= requiredCount;
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