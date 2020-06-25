package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface PlayerJoinedCallback {
    Event<PlayerJoinedCallback> EVENT = EventFactory.createArrayBacked(PlayerJoinedCallback.class,
            (listeners) -> {
                return (uuid, sender) -> {
                    for (PlayerJoinedCallback event : listeners) {
                        event.joined(uuid, sender);
                    }
                };
            }
    );

    void joined(UUID uuid, String sender);
}
