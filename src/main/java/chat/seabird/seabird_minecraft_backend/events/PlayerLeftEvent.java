package chat.seabird.seabird_minecraft_backend.events;

public class PlayerLeftEvent {
    private final String playerName;

    public PlayerLeftEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
