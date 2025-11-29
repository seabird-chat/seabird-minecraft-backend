package chat.seabird.seabird_minecraft_backend.events;

public class SystemMessageEvent {
    private final String sender;
    private final String text;

    public SystemMessageEvent(String sender, String text) {
        this.sender = sender;
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }
}
