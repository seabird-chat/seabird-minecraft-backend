package chat.seabird.seabird_minecraft_backend.events;

public class EmoteEvent {
    private final String playerId;
    private final String playerName;
    private final String message;

    public EmoteEvent(String playerId, String playerName, String message) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.message = message;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }
}
