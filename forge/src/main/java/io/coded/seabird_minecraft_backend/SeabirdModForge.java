package io.coded.seabird_minecraft_backend;

import org.apache.commons.lang3.tuple.Pair;
import me.shedaniel.architectury.platform.forge.EventBuses;
import io.coded.seabird_minecraft_backend.SeabirdMod;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(SeabirdMod.MOD_ID)
public class SeabirdModForge {
    public SeabirdModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(SeabirdMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(
            () -> FMLNetworkConstants.IGNORESERVERONLY, // ignore me, I'm a server only mod
            (s,b) -> true // i accept anything from the server or the save, if I'm asked
        ));

        SeabirdMod.init();
    }
}
