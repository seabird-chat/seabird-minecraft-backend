package chat.seabird.seabird_minecraft_backend.events;

public class PlayerDiedEvent {
    private final String deathMessage;

    public PlayerDiedEvent(String deathMessage) {
        this.deathMessage = deathMessage;
    }

    public String getDeathMessage() {
        return deathMessage;
    }
}
