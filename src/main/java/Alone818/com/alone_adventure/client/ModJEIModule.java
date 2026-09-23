package Alone818.com.alone_adventure.client;

import Alone818.com.alone_adventure.Alone_adventure;
import Alone818.com.alone_adventure.Items.contract.QuestItem;
import Alone818.com.alone_adventure.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI 插件 —— 为 Quest Contract 提供材料状态展示。
 *
 * 作用：
 * <ul>
 *   <li>在 JEI 中高亮显示 {@link ModItems#QUEST_CONTRACT}，</li>
 *   <li>在物品悬浮提示中展示当前任务进度（{@link QuestItem} 的 NBT 状态），</li>
 *   <li>把"已完成"契约（{@code QuestDone=1b}）作为物品变体加入 JEI 搜索列表，
 *       玩家可直接搜到该材料形态；未完成形态由物品栏自动列出。</li>
 * </ul>
 *
 * 客户端专用（Dist.CLIENT），通过 {@code META-INF/mods.toml} 的 {@code jei} 依赖约束加载。
 */
@JeiPlugin
@OnlyIn(Dist.CLIENT)
public class ModJEIModule implements IModPlugin {

    /** 插件唯一标识 */
    public static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(Alone_adventure.MODID, "quest_contract");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // "已完成"契约（QuestDone=1b）—— JEI 运行时加入物品变体，作为可搜索的合成材料
        ItemStack doneContract = new ItemStack(ModItems.RESURRECTION_CONTRACT.get());
        doneContract.getOrCreateTag().putBoolean(QuestItem.TAG_DONE, true);
        registration.getIngredientManager()
                .addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(doneContract));
    }
}
