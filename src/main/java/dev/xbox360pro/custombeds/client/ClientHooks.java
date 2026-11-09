package dev.xbox360pro.custombeds.client;

import dev.xbox360pro.custombeds.client.screen.GiftWrapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

/** Client-only entry points called from common code (always behind a client check). */
public final class ClientHooks {
    private ClientHooks() {}

    private static CustomBedItemRenderer itemRenderer;

    public static BlockEntityWithoutLevelRenderer bedItemRenderer() {
        if (itemRenderer == null) itemRenderer = new CustomBedItemRenderer();
        return itemRenderer;
    }

    private static CustomPaintingItemRenderer paintingRenderer;

    public static BlockEntityWithoutLevelRenderer paintingItemRenderer() {
        if (paintingRenderer == null) paintingRenderer = new CustomPaintingItemRenderer();
        return paintingRenderer;
    }

    public static void openGiftScreen() {
        Minecraft.getInstance().setScreen(new GiftWrapScreen(null));
    }
}
