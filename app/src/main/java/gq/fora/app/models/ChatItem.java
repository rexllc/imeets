package gq.fora.app.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.security.MessageDigest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@IgnoreExtraProperties
public class ChatItem {
	
	private byte[] decval;
    private byte[] decode;

    public String message_id;
    public String author_id;
	public String chat_id;
	public boolean isSeen;
	public boolean isUnsent;
	public boolean isGroup;
    public String content;
	public String imageUrl;
	public int call_type;
	public int type;
    public long timestamp;

    public ChatItem() {}

    public String getMessageId() {
        return this.message_id;
    }

    public String getAuthorId() {
        return this.author_id;
    }

    public String getContent() {
        return this.content;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
	
	public boolean isSeen() {
		return this.isSeen;
	}
	
	public boolean isUnsent() {
		return this.isUnsent;
	}
	
	public boolean isGroup() {
		return this.isGroup;
	}
	
	public String getChatId() {
		return this.chat_id;
	}
	
	public String getImage() {
		return this.imageUrl;
	}
	
	public int getType() {
		return this.type;
	}
	
	public int getCallType() {
		return this.call_type;
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
