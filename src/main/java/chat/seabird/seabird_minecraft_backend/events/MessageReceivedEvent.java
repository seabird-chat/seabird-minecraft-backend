package chat.seabird.seabird_minecraft_backend.events;

public class MessageReceivedEvent {
    private final String playerId;
    private final String playerName;
    private final String messageText;

    public MessageReceivedEvent(String playerId, String playerName, String messageText) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.messageText = messageText;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessageText() {
        return messageText;
    }
}
