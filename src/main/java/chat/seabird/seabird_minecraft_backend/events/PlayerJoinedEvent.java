package chat.seabird.seabird_minecraft_backend.events;

public class PlayerJoinedEvent {
    private final String playerName;

    public PlayerJoinedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
