package gq.fora.app.models.story;

import java.util.Calendar;

public class StoryBuilder {
	
	private Calendar cal = Calendar.getInstance();
	
	public String userId;
	public long timestamp;
	public String imageUrl;
	public boolean isVisible;
	public String storyId;
	public int type;
	
	public StoryBuilder() {
		//Default
	}
	
	public StoryBuilder(String userId, String imageUrl, String storyId, int type) {
		this.userId = userId;
		this.imageUrl = imageUrl;
		this.timestamp = cal.getTimeInMillis();
		this.isVisible = true;
		this.storyId = storyId;
		this.type = type;
		
	}
}