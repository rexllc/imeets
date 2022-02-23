package gq.fora.app.models.list;
import java.util.ArrayList;

public class Story {
	
	public String userId;
	public long timestamp;
	public String imageUrl;
	public ArrayList<Story> storyList;
	
	public String getUserId() {
		return this.userId;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public String getImageUrl() {
		return this.imageUrl;
	}
	
	public ArrayList<Story> getAllStory() {
		return this.storyList;
	}
}