package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface AdvancementCallback {
    Event<AdvancementCallback> EVENT = EventFactory.createArrayBacked(AdvancementCallback.class,
            (listeners) -> {
                return (uuid, sender, advancement) -> {
                    for (AdvancementCallback event : listeners) {
                        event.advancement(uuid, sender, advancement);
                    }
                };
            }
    );

    void advancement(UUID uuid, String sender, String advancement);
}
