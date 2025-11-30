//? neoforge {
package chat.seabird.seabird_minecraft_backend;

import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(SeabirdMod.MOD_ID)
public class SeabirdEntrypointNeoforge {
    private static final Logger LOGGER = LogManager.getLogger();

    public SeabirdEntrypointNeoforge() {
        LOGGER.info("Bootstrapping SeabirdMod from Neoforge");

        SeabirdEntrypointArchitectury.init();
    }
}
//?}
