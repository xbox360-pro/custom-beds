package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.xbox360pro.custombeds.CustomBeds;
import dev.xbox360pro.custombeds.network.Network;
import dev.xbox360pro.custombeds.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Downloads bed images from the server on demand and turns them into live GPU textures.
 * This is what makes new beds show up instantly with no restart / resource reload.
 */
public final class ClientBedTextures {
    private ClientBedTextures() {}

    private static final class Entry {
        ResourceLocation texture;
        boolean requested;
        boolean missing;
    }

    private static final Map<UUID, Entry> CACHE = new HashMap<>();
    private static ResourceLocation fallback;
    private static ResourceLocation paintingFallback;

    public static ResourceLocation texture(@Nullable UUID id) {
        return texture(id, false);
    }

    public static ResourceLocation texture(@Nullable UUID id, boolean painting) {
        if (id == null) return painting ? paintingFallback() : fallback();
        Entry e = CACHE.computeIfAbsent(id, k -> new Entry());
        if (e.texture != null) return e.texture;
        if (!e.requested && !e.missing && Minecraft.getInstance().getConnection() != null) {
            e.requested = true;
            Network.sendToServer(new Packets.RequestImage(id));
        }
        return painting ? paintingFallback() : fallback();
    }

    public static void accept(UUID id, byte[] png) {
        Entry e = CACHE.computeIfAbsent(id, k -> new Entry());
        e.requested = true;
        if (png.length == 0) {
            e.missing = true;
            return;
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            ResourceLocation rl = new ResourceLocation(CustomBeds.MODID, "dynamic/bed_" + id);
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(image));
            e.texture = rl;
        } catch (Exception ex) {
            CustomBeds.LOGGER.warn("Couldn't load bed texture {}", id, ex);
            e.missing = true;
        }
    }

    public static ResourceLocation fallback() {
        if (fallback == null) {
            try {
                byte[] png = BedImageMaker.toPng(BedImageMaker.placeholder());
                NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                fallback = new ResourceLocation(CustomBeds.MODID, "dynamic/placeholder");
                Minecraft.getInstance().getTextureManager().register(fallback, new DynamicTexture(image));
            } catch (Exception ex) {
                CustomBeds.LOGGER.error("Couldn't build placeholder bed texture", ex);
                fallback = new ResourceLocation("minecraft", "textures/block/white_wool.png");
            }
        }
        return fallback;
    }

    public static ResourceLocation paintingFallback() {
        if (paintingFallback == null) {
            try {
                byte[] png = BedImageMaker.toPng(BedImageMaker.paintingPlaceholder());
                NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                paintingFallback = new ResourceLocation(CustomBeds.MODID, "dynamic/painting_placeholder");
                Minecraft.getInstance().getTextureManager().register(paintingFallback, new DynamicTexture(image));
            } catch (Exception ex) {
                paintingFallback = new ResourceLocation("minecraft", "textures/painting/back.png");
            }
        }
        return paintingFallback;
    }

    /** Forget everything when leaving a world/server (designs are per-world). */
    public static void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        for (Entry e : CACHE.values()) {
            if (e.texture != null) tm.release(e.texture);
        }
        CACHE.clear();
    }
}
