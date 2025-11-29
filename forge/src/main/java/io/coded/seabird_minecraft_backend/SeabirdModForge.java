package io.coded.seabird_minecraft_backend;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(SeabirdMod.MOD_ID)
public class SeabirdModForge {
    private static final Logger LOGGER = LogManager.getLogger();

    public SeabirdModForge() {
        LOGGER.info("Bootstrapping SeabirdMod from Forge");

        // Submit our event bus to let Architectury register our content on the right time
        EventBuses.registerModEventBus(SeabirdMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Mark this mod as fine to be server only - it doesn't need to be installed on the client.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(
                // Ignore this mod if not present on the client
                () -> NetworkConstants.IGNORESERVERONLY,
                // If present on the client, accept any version if from a server
                (remoteVersion, isFromServer) -> isFromServer
        ));

        SeabirdMod.init();
    }
}
