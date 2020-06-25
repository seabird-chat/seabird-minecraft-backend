package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface PlayerLeftCallback {
    Event<PlayerLeftCallback> EVENT = EventFactory.createArrayBacked(PlayerLeftCallback.class,
            (listeners) -> {
                return (uuid, sender) -> {
                    for (PlayerLeftCallback event : listeners) {
                        event.left(uuid, sender);
                    }
                };
            }
    );

    void left(UUID uuid, String sender);
}
