package gq.fora.app.models.sessions;

public class Session {

    public String senderId;
    public String receiverId;
    public long lastSent;

    public Session(String chat_id, String author_id) {
        this.receiverId = chat_id;
        this.senderId = author_id;
        this.lastSent = System.currentTimeMillis();
    }
}
