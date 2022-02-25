package gq.fora.app.models.friends.add;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddFriends {

    private static AddFriends instance = null;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();

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
        map.put("userId", following_id);
        map.put("senderId", follower_id);
        map.put("receiverId", following_id);
        database.collection("friends")
                .document(follower_id)
                .collection("list")
                .document(following_id)
                .set(map);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("isFriends", false);
        map2.put("status", "pending");
        map2.put("friendsId", follower_id);
        map2.put("userId", following_id);
        map2.put("senderId", following_id);
        map2.put("receiverId", follower_id);
        database.collection("friends")
                .document(following_id)
                .collection("list")
                .document(follower_id)
                .set(map2);
    }

    public void Unfriend(String follower_id, String following_id) {
        database.collection("friends")
                .document(follower_id)
                .collection("list")
                .document(following_id)
                .delete();
        database.collection("friends")
                .document(following_id)
                .collection("list")
                .document(follower_id)
                .delete();
    }

    public void confirmFriends(String follower_id, String following_id) {
        Map<String, Object> map = new HashMap<>();
        map.put("isFriends", true);
        map.put("status", "friends");
		database.collection("friends")
                .document(follower_id)
                .collection("list")
                .document(following_id)
                .update(map);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("isFriends", true);
        map2.put("status", "friends");
        database.collection("friends")
                .document(following_id)
                .collection("list")
                .document(follower_id)
                .update(map2);
    }

    public void removeRequest(String follower_id, String following_id) {
        database.collection("friends")
                .document(follower_id)
                .collection("list")
                .document(follower_id)
                .delete();
        database.collection("friends")
                .document(following_id)
                .collection("list")
                .document(follower_id)
                .delete();
    }
}
