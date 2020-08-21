package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface PlayerDiedCallback {
    Event<PlayerDiedCallback> EVENT = EventFactory.createArrayBacked(PlayerDiedCallback.class,
            (listeners) -> (uuid, sender, text) -> {
                for (PlayerDiedCallback event : listeners) {
                    event.death(uuid, sender, text);
                }
            }
    );

    void death(UUID uuid, String sender, String text);
}
