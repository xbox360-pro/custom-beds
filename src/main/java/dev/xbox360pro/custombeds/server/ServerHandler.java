package dev.xbox360pro.custombeds.server;

import dev.xbox360pro.custombeds.CustomBeds;
import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.data.BedDesign;
import dev.xbox360pro.custombeds.data.BedDesignStorage;
import dev.xbox360pro.custombeds.entity.CustomPaintingEntity;
import dev.xbox360pro.custombeds.item.CustomBedItem;
import dev.xbox360pro.custombeds.item.CustomPaintingItem;
import dev.xbox360pro.custombeds.item.WrappedGiftItem;
import dev.xbox360pro.custombeds.network.Network;
import dev.xbox360pro.custombeds.network.Packets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CustomBeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerHandler {
    private ServerHandler() {}

    public static final int TEX_W = 64;
    public static final int TEX_H = 128;
    private static final int MAX_CHUNKS = 16;              // ~480 KB, plenty for a 256x256 painting PNG
    private static final int MAX_DESIGNS_PER_PLAYER = 500;

    private static final class Pending {
        final String name;
        final int kind, w, h;
        final int total;
        int next;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();

        Pending(String name, int kind, int w, int h, int total) {
            this.name = name;
            this.kind = kind;
            this.w = w;
            this.h = h;
            this.total = total;
        }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ uploads

    public static void onUploadChunk(ServerPlayer player, Packets.UploadChunk msg) {
        UUID key = player.getUUID();
        if (msg.total() < 1 || msg.total() > MAX_CHUNKS) {
            PENDING.remove(key);
            fail(player, Component.translatable("message.custombeds.upload_too_big"));
            return;
        }
        if (msg.index() == 0) PENDING.put(key, new Pending(msg.name(), msg.kind(), msg.w(), msg.h(), msg.total()));
        Pending p = PENDING.get(key);
        if (p == null || msg.index() != p.next || msg.total() != p.total) {
            PENDING.remove(key);
            fail(player, Component.translatable("message.custombeds.upload_broken"));
            return;
        }
        p.data.writeBytes(msg.data());
        p.next++;
        if (p.next == p.total) {
            PENDING.remove(key);
            finishUpload(player, p, p.data.toByteArray());
        }
    }

    private static void finishUpload(ServerPlayer player, Pending p, byte[] png) {
        boolean painting = p.kind == BedDesign.KIND_PAINTING;
        int w = painting ? p.w : 0, h = painting ? p.h : 0;
        if (painting && (w < 1 || h < 1 || w > CustomPaintingEntity.MAX_SIZE || h > CustomPaintingEntity.MAX_SIZE)) {
            fail(player, Component.translatable("message.custombeds.upload_bad_image"));
            return;
        }
        int texW = painting ? w * PAINTING_PX : TEX_W;
        int texH = painting ? h * PAINTING_PX : TEX_H;
        if (!isValidTexture(png, texW, texH)) {
            fail(player, Component.translatable("message.custombeds.upload_bad_image"));
            return;
        }
        BedDesignStorage storage = BedDesignStorage.get(player.server);
        if (storage.countOwnedBy(player.getUUID()) >= MAX_DESIGNS_PER_PLAYER && !player.hasPermissions(2)) {
            fail(player, Component.translatable("message.custombeds.too_many", MAX_DESIGNS_PER_PLAYER));
            return;
        }

        String name = cleanName(p.name);
        BedDesign design = new BedDesign(UUID.randomUUID(), name, player.getUUID(),
                player.getGameProfile().getName(), System.currentTimeMillis(), png,
                painting ? BedDesign.KIND_PAINTING : BedDesign.KIND_BED, w, h);
        storage.add(design);

        give(player, itemFor(design));
        Network.sendTo(player, new Packets.BedImage(design.id(), design.name(), design.ownerName(), design.png()));
        Network.sendTo(player, new Packets.UploadResult(true,
                Component.translatable(painting ? "message.custombeds.upload_ok_painting" : "message.custombeds.upload_ok", name)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
        CustomBeds.LOGGER.info("{} created custom {} '{}' ({})", player.getGameProfile().getName(),
                painting ? "painting" : "bed", name, design.id());
    }

    public static final int PAINTING_PX = 64;

    private static ItemStack itemFor(BedDesign d) {
        return d.isPainting() ? CustomPaintingItem.create(d.ref(), d.width(), d.height()) : CustomBedItem.create(d.ref());
    }

    public static boolean isGiftable(ItemStack stack) {
        return stack.getItem() instanceof CustomBedItem || stack.getItem() instanceof CustomPaintingItem;
    }

    private static boolean isValidTexture(byte[] png, int texW, int texH) {
        if (png.length < 8 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return false;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            return img != null && img.getWidth() == texW && img.getHeight() == texH;
        } catch (Exception e) {
            return false;
        }
    }

    private static String cleanName(String raw) {
        String s = ChatFormatting.stripFormatting(raw);
        if (s == null) s = "";
        s = s.replaceAll("\\p{Cntrl}", "").trim();
        if (s.length() > Packets.MAX_NAME) s = s.substring(0, Packets.MAX_NAME).trim();
        return s.isEmpty() ? "Custom" : s;
    }

    private static void fail(ServerPlayer player, Component reason) {
        Network.sendTo(player, new Packets.UploadResult(false, reason));
    }

    // ------------------------------------------------------------------ library

    public static void onRequestImage(ServerPlayer player, UUID id) {
        BedDesign d = BedDesignStorage.get(player.server).get(id);
        if (d == null) {
            Network.sendTo(player, new Packets.BedImage(id, "", "", new byte[0]));
        } else {
            Network.sendTo(player, new Packets.BedImage(d.id(), d.name(), d.ownerName(), d.png()));
        }
    }

    public static void onRequestList(ServerPlayer player) {
        List<Packets.DesignEntry> entries = new ArrayList<>();
        for (BedDesign d : BedDesignStorage.get(player.server).newestFirst()) {
            entries.add(new Packets.DesignEntry(d.id(), d.name(), d.ownerName(), d.ownerId(), d.kind(), d.width(), d.height()));
        }
        Network.sendTo(player, new Packets.DesignList(entries, canCopyAll(player)));
    }

    public static void onGetCopy(ServerPlayer player, UUID id) {
        BedDesign d = BedDesignStorage.get(player.server).get(id);
        if (d == null) return;
        if (!d.ownerId().equals(player.getUUID()) && !canCopyAll(player)) {
            player.displayClientMessage(Component.translatable("message.custombeds.not_your_design")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        give(player, itemFor(d));
        player.displayClientMessage(Component.translatable(d.isPainting() ? "message.custombeds.got_copy_painting" : "message.custombeds.got_copy", d.name())
                .withStyle(ChatFormatting.GREEN), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static boolean canCopyAll(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    // ------------------------------------------------------------------ gifts

    public static void onWrapGift(ServerPlayer player, Packets.WrapGift msg) {
        Inventory inv = player.getInventory();
        if (msg.slot() < 0 || msg.slot() >= inv.getContainerSize()) return;
        ItemStack bed = inv.getItem(msg.slot());
        if (!isGiftable(bed)) {
            player.displayClientMessage(Component.translatable("message.custombeds.wrap_no_bed").withStyle(ChatFormatting.RED), true);
            return;
        }

        ServerPlayer target = msg.recipient() == null ? null : player.server.getPlayerList().getPlayer(msg.recipient());
        if (msg.recipient() != null && target == null) {
            player.displayClientMessage(Component.translatable("message.custombeds.player_offline").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (msg.send() && (target == null || target == player)) {
            player.displayClientMessage(Component.translatable("message.custombeds.pick_player").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!player.getAbilities().instabuild && !takeOneGiftWrap(inv)) {
            player.displayClientMessage(Component.translatable("message.custombeds.need_wrap").withStyle(ChatFormatting.RED), true);
            return;
        }

        String message = msg.message();
        if (message.length() > Packets.MAX_MESSAGE) message = message.substring(0, Packets.MAX_MESSAGE);
        message = message.replaceAll("\\p{Cntrl}", "").trim();

        ItemStack content = bed.split(1);
        ItemStack gift = WrappedGiftItem.wrap(content, player, target, message);
        inv.setChanged();

        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (msg.send()) {
            give(target, gift);
            target.sendSystemMessage(Component.translatable("message.custombeds.gift_received",
                    player.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
            target.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.8f);
            player.displayClientMessage(Component.translatable("message.custombeds.gift_sent",
                    target.getGameProfile().getName()).withStyle(ChatFormatting.GREEN), true);
        } else {
            give(player, gift);
            player.displayClientMessage(Component.translatable("message.custombeds.gift_wrapped")
                    .withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static boolean takeOneGiftWrap(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModRegistry.GIFT_WRAP.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
