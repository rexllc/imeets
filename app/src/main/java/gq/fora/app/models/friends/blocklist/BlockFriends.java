package gq.fora.app.models.friends.blocklist;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BlockFriends {

    private static BlockFriends instance = null;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();

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
        database.collection("block")
                .document(follower_id)
                .collection("users")
                .document(following_id)
                .set(map);
    }

    public void UnblockUser(String follower_id, String following_id) {
        Map<String, Object> map = new HashMap<>();
        map.put("friendsId", follower_id);
        map.put("userId", following_id);
        map.put("status", "unblocked");
        map.put("isBlock", false);
        database.collection("block")
                .document(follower_id)
                .collection("users")
                .document(following_id)
                .update(map);
    }
}
