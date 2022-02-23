package gq.fora.app.models.ui;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Comments {
    
	public String storyId;
    public String userId;
    public String text;

    public Comments() {}

    public Comments(String userId, String text) {
        this.userId = userId;
        this.text = text;
    }
}
