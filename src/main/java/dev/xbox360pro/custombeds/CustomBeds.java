package dev.xbox360pro.custombeds;

import com.mojang.logging.LogUtils;
import dev.xbox360pro.custombeds.network.Network;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CustomBeds.MODID)
public class CustomBeds {
    public static final String MODID = "custombeds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CustomBeds() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(modBus);
        Network.register();
    }
}
