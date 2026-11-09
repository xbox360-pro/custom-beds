package dev.xbox360pro.custombeds.item;

import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.client.ClientHooks;
import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.entity.CustomPaintingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class CustomPaintingItem extends Item {

    public CustomPaintingItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(@Nullable DesignRef design, int w, int h) {
        ItemStack stack = new ItemStack(ModRegistry.CUSTOM_PAINTING_ITEM.get());
        CompoundTag tag = stack.getOrCreateTag();
        if (design != null) tag.put("Design", design.toTag());
        tag.putInt("PaintingW", w);
        tag.putInt("PaintingH", h);
        return stack;
    }

    @Nullable
    public static DesignRef getDesign(ItemStack stack) {
        if (!stack.hasTag()) return null;
        return DesignRef.fromTag(stack.getTag().getCompound("Design"));
    }

    public static int getW(ItemStack stack) {
        return stack.hasTag() ? Mth.clamp(stack.getTag().getInt("PaintingW"), 1, CustomPaintingEntity.MAX_SIZE) : 1;
    }

    public static int getH(ItemStack stack) {
        return stack.hasTag() ? Mth.clamp(stack.getTag().getInt("PaintingH"), 1, CustomPaintingEntity.MAX_SIZE) : 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockPos pos = clicked.relative(face);
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (face.getAxis().isVertical() || (player != null && !player.mayUseItemAt(pos, face, stack))) {
            return InteractionResult.FAIL;
        }
        Level level = ctx.getLevel();
        CustomPaintingEntity painting = new CustomPaintingEntity(level, pos, face, getDesign(stack), getW(stack), getH(stack));
        if (!painting.survives()) {
            if (player != null && level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.custombeds.painting_no_room",
                        getW(stack), getH(stack)).withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            painting.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, painting.position());
            level.addFreshEntity(painting);
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        DesignRef design = getDesign(stack);
        if (design == null) return super.getName(stack);
        return Component.translatable("item.custombeds.custom_painting.named", design.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.custombeds.painting_size", getW(stack), getH(stack))
                .withStyle(ChatFormatting.GRAY));
        DesignRef design = getDesign(stack);
        if (design != null) {
            tooltip.add(Component.translatable("tooltip.custombeds.designed_by", design.designer())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ClientHooks.paintingItemRenderer();
            }
        });
    }
}
