package gq.fora.app.models.friends.blocklist;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class BlockFriends {
	
	private static BlockFriends instance = null;
	private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
	private DatabaseReference blocklist = firebase.getReference("blocklist");
	
	public String friendsId;
	public String userId;
	public boolean isBlock;
	public String status;
	
	public static BlockFriends getInstance() {
		if (instance == null) {
			instance = new BlockFriends();
		}
		return instance;
	}
	
	public void BlockUser(String follower_id, String following_id) {
		Map<String, Object> map = new HashMap<>();
		map.put("friendsId", follower_id);
		map.put("userId", following_id);
		map.put("status", "blocked");
		map.put("isBlock", true);
		blocklist.child(follower_id).child(following_id).updateChildren(map);
	}
	
	public void UnblockUser(String follower_id, String following_id) {
		Map<String, Object> map = new HashMap<>();
		map.put("friendsId", follower_id);
		map.put("userId", following_id);
		map.put("status", "unblocked");
		map.put("isBlock", false);
		blocklist.child(follower_id).child(following_id).updateChildren(map);
	}
}