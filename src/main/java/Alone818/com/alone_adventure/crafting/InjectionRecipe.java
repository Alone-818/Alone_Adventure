package Alone818.com.alone_adventure.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

import Alone818.com.alone_adventure.Items.injection_template;
import Alone818.com.alone_adventure.init.ModItems;

/**
 * 药水针剂合成配方（通用模板）
 *
 * 形状自由匹配：任意位置放
 *   1 瓶瓶装药水（任意含效果药水，含喷溅/滞留）
 * + 1 萤石粉
 * + 2 空针管
 * → 2 个针剂（效果时长减半 0.5，使用时间为药水 × 0.8）
 *
 * 药水瓶会退回一个玻璃瓶（合成余物）。
 */
public class InjectionRecipe extends CustomRecipe {

    public InjectionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int potion = 0;
        int glowstone = 0;
        int empty = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof PotionItem
                    && !PotionUtils.getMobEffects(stack).isEmpty()) {
                potion++;
            } else if (stack.is(Items.GLOWSTONE_DUST)) {
                glowstone += stack.getCount();
            } else if (stack.is(ModItems.INJECTION_EMPTY.get())) {
                empty += stack.getCount();
            } else {
                return false; // 存在其他物品则不匹配
            }
        }
        return potion == 1 && glowstone == 1 && empty == 2;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof PotionItem) {
                List<MobEffectInstance> effects = injection_template.getEffects(stack);
                return injection_template.createSyringe(effects, 2);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 药水退回玻璃瓶，其余材料正常消耗 */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remaining =
                NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof PotionItem) {
                remaining.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    /** 序列化器：配方本身无数据，仅注册类型标识 */
    public static class Serializer implements RecipeSerializer<InjectionRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public InjectionRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    json.has("category") ? json.get("category").getAsString() : "misc",
                    CraftingBookCategory.MISC);
            return new InjectionRecipe(recipeId, category);
        }

        @Override
        public InjectionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new InjectionRecipe(recipeId, CraftingBookCategory.MISC);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InjectionRecipe recipe) {
            // 无数据需序列化
        }
    }
}
