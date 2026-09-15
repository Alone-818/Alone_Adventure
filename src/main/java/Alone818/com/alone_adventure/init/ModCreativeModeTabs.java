package Alone818.com.alone_adventure.init;

import Alone818.com.alone_adventure.Alone_adventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alone_adventure.MODID);

    // ===== 武器标签 =====
    public static final RegistryObject<CreativeModeTab> WEAPONS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_weapons",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.POWERSWORD.get()))
                            .title(Component.translatable("tab.alone_adventure.weapons"))
                            .displayItems((itemDisplayParameters, output) -> {
                                // 武器：动力剑
                                output.accept(new ItemStack(ModItems.POWERSWORD.get()));
                                // 武器：招架之盾
                                output.accept(new ItemStack(ModItems.PARRYSHIELD.get()));
                                // 武器：链锯剑
                                output.accept(new ItemStack(ModItems.CHAINSAW_SWORD.get()));
                                // 武器：痛击之锤
                                output.accept(new ItemStack(ModItems.PAINSTRIKE_HAMMER.get()));
                                // 武器：墨制刀刃
                                output.accept(new ItemStack(ModItems.INK_BLADE.get()));
                            })
                            .build());

    // ===== 饰品标签 =====
    public static final RegistryObject<CreativeModeTab> CURIOS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_curios",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.CRYSTALLINE_HEART.get()))
                            .title(Component.translatable("tab.alone_adventure.curios"))
                            .displayItems((itemDisplayParameters, output) -> {
                                // 饰品
                                output.accept(new ItemStack(ModItems.BLEEDINGSHIELD.get()));
                                output.accept(new ItemStack(ModItems.CRYSTALLINE_HEART.get()));
                                output.accept(new ItemStack(ModItems.NIGHT_CONTRACT.get()));
                                output.accept(new ItemStack(ModItems.SURVIVAL_WHIMPER.get()));
                                output.accept(new ItemStack(ModItems.ADAPTIVE_FLESH.get()));
                                output.accept(new ItemStack(ModItems.DRAGON_POWER.get()));
                                output.accept(new ItemStack(ModItems.IMPERIAL_EAGLE.get()));
                                output.accept(new ItemStack(ModItems.BROKEN_MASK.get()));
                                output.accept(new ItemStack(ModItems.BINDING_BANDAGE.get()));
                            })
                            .build());

    // ===== 道具标签 =====
    public static final RegistryObject<CreativeModeTab> ITEMS_TAB =
            CREATIVE_MODE_TABS.register("alone_adventure_items",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.INJECTION_SYRINGE.get()))
                            .title(Component.translatable("tab.alone_adventure.items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept((new ItemStack(ModItems.INJECTION_EMPTY.get())));
                                output.accept((new ItemStack(ModItems.INJECTION_SYRINGE.get())));
                            })
                            .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}