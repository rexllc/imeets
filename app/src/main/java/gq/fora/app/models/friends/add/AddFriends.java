package gq.fora.app.models.friends.add;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AddFriends {

    private static AddFriends instance = null;

    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference friends = firebase.getReference("friends");

    public static AddFriends getInstance() {
        if (instance == null) {
            instance = new AddFriends();
        }
        return instance;
    }

    public void Add(String follower_id, String following_id) {
        Map<String, Object> map = new HashMap<>();
        map.put("isFriends", false);
        map.put("friendsId", following_id);
        map.put("status", "pending");
        map.put("userId", follower_id);
		map.put("senderId", follower_id);
		map.put("receiverId", following_id);
        friends.child(follower_id).child(following_id).updateChildren(map);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("isFriends", false);
        map2.put("status", "pending");
        map2.put("friendsId", follower_id);
        map2.put("userId", following_id);
		map2.put("senderId", following_id);
		map2.put("receiverId", follower_id);
        friends.child(following_id).child(follower_id).updateChildren(map2);
    }

    public void Unfriend(String follower_id, String following_id) {
        friends.child(follower_id).child(following_id).removeValue();
        friends.child(following_id).child(follower_id).removeValue();
    }

    public void confirmFriends(String follower_id, String following_id) {
        Map<String, Object> map = new HashMap<>();
        map.put("isFriends", true);
        map.put("status", "friends");
        friends.child(follower_id).child(following_id).updateChildren(map);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("isFriends", true);
        map2.put("status", "friends");
        friends.child(following_id).child(follower_id).updateChildren(map2);
    }

    public void removeRequest(String follower_id, String following_id) {
        friends.child(follower_id).child(following_id).removeValue();
        friends.child(following_id).child(follower_id).removeValue();
    }
}
