package gq.fora.app.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import gq.fora.app.models.ui.Comments;

import java.util.HashMap;

public class DAOComments {

    private FirebaseDatabase db;
    private DatabaseReference ref;

    public String comment_id;
    public String userId;
    public String text;

    public DAOComments() {
        db = FirebaseDatabase.getInstance();
        ref = db.getReference("comments");
    }

    public Task<Void> add(String key, Comments data) {
        return ref.child(key).push().setValue(data);
    }

    public Task<Void> update(String key, HashMap<String, Object> map) {
        return ref.child(key).updateChildren(map);
    }

    public Task<Void> remove(String key) {
        return ref.child(key).removeValue();
    }

    public Query get(String key, int limit) {
        if (key == null) return ref.orderByKey().limitToFirst(10);
        return ref.child(key).limitToFirst(limit);
    }
}
