//? fabric {
package chat.seabird.seabird_minecraft_backend;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SeabirdEntrypointFabric implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("Bootstrapping SeabirdMod from Fabric");
        SeabirdEntrypointArchitectury.init();
    }
}
//?}
