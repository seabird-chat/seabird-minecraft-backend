package io.coded.seabird_minecraft_backend;

import me.shedaniel.architectury.platform.forge.EventBuses;
import io.coded.seabird_minecraft_backend.SeabirdMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SeabirdMod.MOD_ID)
public class SeabirdModForge {
    public SeabirdModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(SeabirdMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        SeabirdMod.init();
    }
}
