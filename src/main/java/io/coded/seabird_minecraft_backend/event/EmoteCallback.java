package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface EmoteCallback {
    Event<EmoteCallback> EVENT = EventFactory.createArrayBacked(EmoteCallback.class,
            (listeners) -> {
                return (uuid, sender, action) -> {
                    for (EmoteCallback event : listeners) {
                        event.emote(uuid, sender, action);
                    }
                };
            }
    );

    void emote(UUID uuid, String sender, String action);
}
