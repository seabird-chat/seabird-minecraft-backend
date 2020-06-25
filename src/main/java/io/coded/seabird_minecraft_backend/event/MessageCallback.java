package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.UUID;

public interface MessageCallback {
    Event<MessageCallback> EVENT = EventFactory.createArrayBacked(MessageCallback.class,
            (listeners) -> {
                return (uuid, sender, text) -> {
                    for (MessageCallback event : listeners) {
                        event.message(uuid, sender, text);
                    }
                };
            }
    );

    void message(UUID uuid, String sender, String text);
}
