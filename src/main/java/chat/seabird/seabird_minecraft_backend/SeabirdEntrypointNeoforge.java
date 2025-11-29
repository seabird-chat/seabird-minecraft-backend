//? neoforge {
/*package chat.seabird.seabird_minecraft_backend;

import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.network.NetworkConstants;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(SeabirdMod.MOD_ID)
public class SeabirdEntrypointNeoforge {
    private static final Logger LOGGER = LogManager.getLogger();

    public SeabirdEntrypointNeoforge() {
        LOGGER.info("Bootstrapping SeabirdMod from Forge");

        // Mark this mod as fine to be server only - it doesn't need to be installed on the client.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(
                // Ignore this mod if not present on the client
                () -> NetworkConstants.IGNORESERVERONLY,
                // If present on the client, accept any version if from a server
                (remoteVersion, isFromServer) -> isFromServer
        ));

        SeabirdEntrypointArchitectury.init();
    }
}
*///?}
