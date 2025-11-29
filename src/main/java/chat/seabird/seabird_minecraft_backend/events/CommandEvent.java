package chat.seabird.seabird_minecraft_backend.events;

import java.util.List;

public class CommandEvent {
    private final String commandName;
    private final List<String> arguments;
    private final String playerId;
    private final String playerName;
    private final String senderDisplayName;

    public CommandEvent(String commandName, List<String> arguments, String playerId, String playerName, String senderDisplayName) {
        this.commandName = commandName;
        this.arguments = arguments;
        this.playerId = playerId;
        this.playerName = playerName;
        this.senderDisplayName = senderDisplayName;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }
}
