package dev.xbox360pro.custombeds.item;

import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.client.ClientHooks;
import dev.xbox360pro.custombeds.data.DesignRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class CustomBedItem extends BedItem {

    public CustomBedItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    public static DesignRef getDesign(ItemStack stack) {
        if (!stack.hasTag()) return null;
        return DesignRef.fromTag(stack.getTag().getCompound("Design"));
    }

    public static ItemStack create(DesignRef design) {
        ItemStack stack = new ItemStack(ModRegistry.CUSTOM_BED_ITEM.get());
        stack.getOrCreateTag().put("Design", design.toTag());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        DesignRef design = getDesign(stack);
        if (design == null) return super.getName(stack);
        return Component.translatable("item.custombeds.custom_bed.named", design.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DesignRef design = getDesign(stack);
        if (design != null) {
            tooltip.add(Component.translatable("tooltip.custombeds.designed_by", design.designer())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.custombeds.blank").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ClientHooks.bedItemRenderer();
            }
        });
    }
}
