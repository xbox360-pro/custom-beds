package dev.xbox360pro.custombeds.network;

import dev.xbox360pro.custombeds.CustomBeds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private Network() {}

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CustomBeds.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        // client -> server
        CHANNEL.messageBuilder(Packets.UploadChunk.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Packets.UploadChunk::encode).decoder(Packets.UploadChunk::decode)
                .consumerMainThread(Packets.UploadChunk::handle).add();
        CHANNEL.messageBuilder(Packets.RequestImage.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Packets.RequestImage::encode).decoder(Packets.RequestImage::decode)
                .consumerMainThread(Packets.RequestImage::handle).add();
        CHANNEL.messageBuilder(Packets.RequestList.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Packets.RequestList::encode).decoder(Packets.RequestList::decode)
                .consumerMainThread(Packets.RequestList::handle).add();
        CHANNEL.messageBuilder(Packets.GetCopy.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Packets.GetCopy::encode).decoder(Packets.GetCopy::decode)
                .consumerMainThread(Packets.GetCopy::handle).add();
        CHANNEL.messageBuilder(Packets.WrapGift.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Packets.WrapGift::encode).decoder(Packets.WrapGift::decode)
                .consumerMainThread(Packets.WrapGift::handle).add();
        // server -> client
        CHANNEL.messageBuilder(Packets.BedImage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Packets.BedImage::encode).decoder(Packets.BedImage::decode)
                .consumerMainThread(Packets.BedImage::handle).add();
        CHANNEL.messageBuilder(Packets.DesignList.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Packets.DesignList::encode).decoder(Packets.DesignList::decode)
                .consumerMainThread(Packets.DesignList::handle).add();
        CHANNEL.messageBuilder(Packets.UploadResult.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Packets.UploadResult::encode).decoder(Packets.UploadResult::decode)
                .consumerMainThread(Packets.UploadResult::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
