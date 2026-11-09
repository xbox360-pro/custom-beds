package dev.xbox360pro.custombeds;

import dev.xbox360pro.custombeds.block.CustomBedBlock;
import dev.xbox360pro.custombeds.block.CustomBedBlockEntity;
import dev.xbox360pro.custombeds.item.CustomBedItem;
import dev.xbox360pro.custombeds.item.GiftWrapItem;
import dev.xbox360pro.custombeds.item.WrappedGiftItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CustomBeds.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CustomBeds.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CustomBeds.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CustomBeds.MODID);

    public static final RegistryObject<CustomBedBlock> CUSTOM_BED = BLOCKS.register("custom_bed",
            () -> new CustomBedBlock(BlockBehaviour.Properties.copy(Blocks.WHITE_BED)));

    public static final RegistryObject<BlockEntityType<CustomBedBlockEntity>> CUSTOM_BED_BE =
            BLOCK_ENTITIES.register("custom_bed",
                    () -> BlockEntityType.Builder.of(CustomBedBlockEntity::new, CUSTOM_BED.get()).build(null));

    public static final RegistryObject<CustomBedItem> CUSTOM_BED_ITEM = ITEMS.register("custom_bed",
            () -> new CustomBedItem(CUSTOM_BED.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<GiftWrapItem> GIFT_WRAP = ITEMS.register("gift_wrap",
            () -> new GiftWrapItem(new Item.Properties()));

    public static final RegistryObject<WrappedGiftItem> WRAPPED_GIFT = ITEMS.register("wrapped_gift",
            () -> new WrappedGiftItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.custombeds"))
                    .icon(() -> new ItemStack(WRAPPED_GIFT.get()))
                    .displayItems((params, output) -> output.accept(GIFT_WRAP.get()))
                    .build());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }
}
