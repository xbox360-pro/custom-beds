package dev.xbox360pro.custombeds.client;

import dev.xbox360pro.custombeds.client.screen.BedStudioScreen;
import dev.xbox360pro.custombeds.network.Network;
import dev.xbox360pro.custombeds.network.Packets;
import net.minecraft.client.Minecraft;

import java.util.Arrays;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onBedImage(Packets.BedImage m) {
        ClientBedTextures.accept(m.id(), m.png());
    }

    public static void onDesignList(Packets.DesignList m) {
        BedStudioScreen.updateList(m.entries(), m.canCopyAll());
    }

    public static void onUploadResult(Packets.UploadResult m) {
        if (Minecraft.getInstance().screen instanceof BedStudioScreen screen) {
            screen.onUploadResult(m.ok(), m.message());
        }
    }

    /** Sends a finished 64x128 PNG to the server in chunks. */
    public static void upload(String name, byte[] png) {
        int total = Math.max(1, (png.length + Packets.CHUNK_SIZE - 1) / Packets.CHUNK_SIZE);
        for (int i = 0; i < total; i++) {
            int from = i * Packets.CHUNK_SIZE;
            int to = Math.min(png.length, from + Packets.CHUNK_SIZE);
            Network.sendToServer(new Packets.UploadChunk(name, i, total, Arrays.copyOfRange(png, from, to)));
        }
    }
}
