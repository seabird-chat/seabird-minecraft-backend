package io.coded.seabird_minecraft_backend.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SystemMessageCallback {
    Event<SystemMessageCallback> EVENT = EventFactory.createArrayBacked(SystemMessageCallback.class,
            (listeners) -> (sender, text) -> {
                for (SystemMessageCallback event : listeners) {
                    event.systemMessage(sender, text);
                }
            }
    );

    void systemMessage(String sender, String text);
}
