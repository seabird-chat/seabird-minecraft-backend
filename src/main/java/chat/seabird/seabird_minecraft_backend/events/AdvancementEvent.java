package chat.seabird.seabird_minecraft_backend.events;

public class AdvancementEvent {
    private final String playerName;
    private final String advancementName;

    public AdvancementEvent(String playerName, String advancementName) {
        this.playerName = playerName;
        this.advancementName = advancementName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAdvancementName() {
        return advancementName;
    }
}
