package dev.xbox360pro.custombeds.network;

import dev.xbox360pro.custombeds.client.ClientPacketHandler;
import dev.xbox360pro.custombeds.server.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class Packets {
    private Packets() {}

    /** Image uploads are split into chunks because client->server packets are capped at ~32KB. */
    public static final int CHUNK_SIZE = 30000;
    public static final int MAX_NAME = 32;
    public static final int MAX_MESSAGE = 100;

    // ---------------------------------------------------------------- client -> server

    /** kind: 0 = bed, 1 = painting (w/h in blocks, ignored for beds). */
    public record UploadChunk(String name, int kind, int w, int h, int index, int total, byte[] data) {
        public static void encode(UploadChunk m, FriendlyByteBuf buf) {
            buf.writeUtf(m.name, MAX_NAME * 2);
            buf.writeByte(m.kind);
            buf.writeByte(m.w);
            buf.writeByte(m.h);
            buf.writeVarInt(m.index);
            buf.writeVarInt(m.total);
            buf.writeByteArray(m.data);
        }

        public static UploadChunk decode(FriendlyByteBuf buf) {
            return new UploadChunk(buf.readUtf(MAX_NAME * 2), buf.readByte(), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(CHUNK_SIZE));
        }

        public static void handle(UploadChunk m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerHandler.onUploadChunk(player, m);
        }
    }

    public record RequestImage(UUID id) {
        public static void encode(RequestImage m, FriendlyByteBuf buf) {
            buf.writeUUID(m.id);
        }

        public static RequestImage decode(FriendlyByteBuf buf) {
            return new RequestImage(buf.readUUID());
        }

        public static void handle(RequestImage m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerHandler.onRequestImage(player, m.id);
        }
    }

    public record RequestList() {
        public static void encode(RequestList m, FriendlyByteBuf buf) {}

        public static RequestList decode(FriendlyByteBuf buf) {
            return new RequestList();
        }

        public static void handle(RequestList m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerHandler.onRequestList(player);
        }
    }

    public record GetCopy(UUID id) {
        public static void encode(GetCopy m, FriendlyByteBuf buf) {
            buf.writeUUID(m.id);
        }

        public static GetCopy decode(FriendlyByteBuf buf) {
            return new GetCopy(buf.readUUID());
        }

        public static void handle(GetCopy m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerHandler.onGetCopy(player, m.id);
        }
    }

    public record WrapGift(int slot, @Nullable UUID recipient, String message, boolean send) {
        public static void encode(WrapGift m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.slot);
            buf.writeBoolean(m.recipient != null);
            if (m.recipient != null) buf.writeUUID(m.recipient);
            buf.writeUtf(m.message, MAX_MESSAGE * 2);
            buf.writeBoolean(m.send);
        }

        public static WrapGift decode(FriendlyByteBuf buf) {
            int slot = buf.readVarInt();
            UUID recipient = buf.readBoolean() ? buf.readUUID() : null;
            String message = buf.readUtf(MAX_MESSAGE * 2);
            boolean send = buf.readBoolean();
            return new WrapGift(slot, recipient, message, send);
        }

        public static void handle(WrapGift m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) ServerHandler.onWrapGift(player, m);
        }
    }

    // ---------------------------------------------------------------- server -> client

    /** The 64x128 PNG texture for a design. Empty {@code png} means "unknown design". */
    public record BedImage(UUID id, String name, String designer, byte[] png) {
        public static void encode(BedImage m, FriendlyByteBuf buf) {
            buf.writeUUID(m.id);
            buf.writeUtf(m.name, 256);
            buf.writeUtf(m.designer, 256);
            buf.writeByteArray(m.png);
        }

        public static BedImage decode(FriendlyByteBuf buf) {
            return new BedImage(buf.readUUID(), buf.readUtf(256), buf.readUtf(256), buf.readByteArray(1 << 20));
        }

        public static void handle(BedImage m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onBedImage(m));
        }
    }

    public record DesignEntry(UUID id, String name, String ownerName, UUID ownerId, int kind, int w, int h) {
        public boolean isPainting() {
            return kind == 1;
        }
    }

    public record DesignList(List<DesignEntry> entries, boolean canCopyAll) {
        public static void encode(DesignList m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.entries.size());
            for (DesignEntry e : m.entries) {
                buf.writeUUID(e.id());
                buf.writeUtf(e.name(), 256);
                buf.writeUtf(e.ownerName(), 256);
                buf.writeUUID(e.ownerId());
                buf.writeByte(e.kind());
                buf.writeByte(e.w());
                buf.writeByte(e.h());
            }
            buf.writeBoolean(m.canCopyAll);
        }

        public static DesignList decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<DesignEntry> entries = new ArrayList<>(Math.min(n, 4096));
            for (int i = 0; i < n; i++) {
                entries.add(new DesignEntry(buf.readUUID(), buf.readUtf(256), buf.readUtf(256), buf.readUUID(),
                        buf.readByte(), buf.readByte(), buf.readByte()));
            }
            return new DesignList(entries, buf.readBoolean());
        }

        public static void handle(DesignList m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onDesignList(m));
        }
    }

    public record UploadResult(boolean ok, Component message) {
        public static void encode(UploadResult m, FriendlyByteBuf buf) {
            buf.writeBoolean(m.ok);
            buf.writeComponent(m.message);
        }

        public static UploadResult decode(FriendlyByteBuf buf) {
            return new UploadResult(buf.readBoolean(), buf.readComponent());
        }

        public static void handle(UploadResult m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onUploadResult(m));
        }
    }
}
