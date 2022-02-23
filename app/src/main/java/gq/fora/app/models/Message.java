package gq.fora.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.security.MessageDigest;
import java.util.Calendar;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@IgnoreExtraProperties
public class Message {

    private Calendar cal = Calendar.getInstance();

    public String message_id;
    public String author_id;
    public String chat_id;
    public boolean isSeen;
    public boolean isUnsent;
    public boolean isGroup;
    public long timestamp;
    public String content;
    public String imageUrl;
    public int call_type;
    public int type;
    private byte[] encVal;

    public Message() {}

    public Message(
            String chat_id,
            String author_id,
            String content,
            String message_id,
            String imageUrl,
            int type,
            boolean isGroup,
            int call_type) {
        this.chat_id = chat_id;
        this.author_id = author_id;
        this.content = content;
        this.message_id = message_id;
        this.timestamp = cal.getTimeInMillis();
        this.isSeen = false;
        this.imageUrl = imageUrl;
        this.type = type;
        this.isUnsent = false;
        this.isGroup = isGroup;
        this.call_type = call_type;
    }

    private SecretKey generateKey(String pwd) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] b = pwd.getBytes("UTF-8");

        digest.update(b, 0, b.length);

        byte[] key = digest.digest();

        SecretKeySpec sec = new SecretKeySpec(key, "AES");

        return sec;
    }
}
