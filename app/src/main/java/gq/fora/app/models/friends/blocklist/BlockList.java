package gq.fora.app.models.friends.blocklist;

public class BlockList {
	
	public String friendsId;
	public String userId;
	public boolean isBlock;
	public String status;
	
	public String getFriendsId() {
		return this.friendsId;
	}
	
	public String getUserId() {
		return this.userId;
	}
	
	public boolean isBlock() {
		return this.isBlock == true;
	}
	
	public String getStatus() {
		return this.status;
	}
}