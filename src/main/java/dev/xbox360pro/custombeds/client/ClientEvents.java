package dev.xbox360pro.custombeds.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xbox360pro.custombeds.CustomBeds;
import dev.xbox360pro.custombeds.ModRegistry;
import dev.xbox360pro.custombeds.client.screen.BedStudioScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    private ClientEvents() {}

    public static final KeyMapping OPEN_STUDIO = new KeyMapping(
            "key.custombeds.open_studio",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.custombeds");

    @Mod.EventBusSubscriber(modid = CustomBeds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_STUDIO);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModRegistry.CUSTOM_BED_BE.get(), CustomBedRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = CustomBeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_STUDIO.consumeClick()) {
                if (mc.player != null && mc.screen == null) mc.setScreen(new BedStudioScreen());
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientBedTextures.clear();
            BedStudioScreen.forgetList();
        }
    }
}
