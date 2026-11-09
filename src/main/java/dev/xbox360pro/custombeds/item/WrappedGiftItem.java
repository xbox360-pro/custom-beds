package dev.xbox360pro.custombeds.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import dev.xbox360pro.custombeds.ModRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** A wrapped present holding one item. Right-click to unwrap it. */
public class WrappedGiftItem extends Item {

    public WrappedGiftItem(Properties properties) {
        super(properties);
    }

    public static ItemStack wrap(ItemStack content, ServerPlayer from, @Nullable ServerPlayer to, String message) {
        ItemStack gift = new ItemStack(ModRegistry.WRAPPED_GIFT.get());
        CompoundTag g = new CompoundTag();
        g.put("Item", content.save(new CompoundTag()));
        g.putUUID("FromId", from.getUUID());
        g.putString("From", from.getGameProfile().getName());
        if (to != null) {
            g.putUUID("ToId", to.getUUID());
            g.putString("To", to.getGameProfile().getName());
        }
        if (!message.isBlank()) g.putString("Message", message);
        gift.getOrCreateTag().put("Gift", g);
        return gift;
    }

    @Nullable
    private static CompoundTag giftTag(ItemStack stack) {
        return stack.getTagElement("Gift");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag g = giftTag(stack);
        if (g == null) return InteractionResultHolder.fail(stack);

        if (g.hasUUID("ToId")) {
            UUID to = g.getUUID("ToId");
            UUID from = g.hasUUID("FromId") ? g.getUUID("FromId") : null;
            boolean allowed = player.getUUID().equals(to) || player.getUUID().equals(from) || player.isCreative();
            if (!allowed) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.custombeds.not_for_you",
                            g.getString("To")).withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ItemStack content = ItemStack.of(g.getCompound("Item"));
        String fromName = g.getString("From");
        String message = g.getString("Message");
        stack.shrink(1);

        if (!content.isEmpty()) {
            Component itemName = content.getHoverName();
            if (!player.getInventory().add(content)) player.drop(content, false);
            player.sendSystemMessage(Component.translatable("message.custombeds.unwrapped", fromName, itemName)
                    .withStyle(ChatFormatting.GOLD));
            if (!message.isEmpty()) {
                player.sendSystemMessage(Component.literal("\"" + message + "\"")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW));
            }
        }

        if (level instanceof ServerLevel server) {
            double x = player.getX(), y = player.getY() + 1.2, z = player.getZ();
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.4, 0.5, 0.4, 0.35);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 15, 0.5, 0.5, 0.5, 0.1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.8f, 1.0f);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag g = giftTag(stack);
        if (g == null) return;
        if (g.contains("To")) {
            tooltip.add(Component.translatable("tooltip.custombeds.gift_to", g.getString("To")).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.custombeds.gift_from", g.getString("From")).withStyle(ChatFormatting.GRAY));
        if (g.contains("Message")) {
            tooltip.add(Component.literal("\"" + g.getString("Message") + "\"").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.custombeds.unwrap_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
