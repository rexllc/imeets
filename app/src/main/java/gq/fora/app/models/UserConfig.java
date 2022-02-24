package gq.fora.app.models;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import gq.fora.app.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserConfig {

    private static UserConfig sInstance = null;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private Calendar cal = Calendar.getInstance();
    private String name;
    private String avatar_url;
    private boolean online;
    private String username;
    private String password;

    public UserConfig() {}

    @NonNull
    public static UserConfig getInstance() {

        if (sInstance == null) {
            sInstance = new UserConfig();
        }

        return sInstance;
    }

    public void initializeConfig() {}

    public String getDisplayName() {
        return FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    }

    public String getUserPhoto() {
        return FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString();
    }

    public void updateToken(String token) {
        Map<String, Object> map = new HashMap<>();
        map.put("fcmToken", token);
        database.collection("users").document(getUid()).update(map);
    }

    public void updateStatus(String userId, boolean isOnline) {
        Map<String, Object> map1 = new HashMap<>();
        if (isOnline) {
            map1.put("isOnline", true);
        } else {
            map1.put("isOnline", false);
        }
        map1.put("lastSession", System.currentTimeMillis());
        database.collection("users").document(getUid()).update(map1);
    }

    public void setStatus(Context ctx, TextView status, String userId) {
        database.collection("users")
                .document(userId)
                .addSnapshotListener(
                        (value, exception) -> {
							if (exception != null) return;
                            if (value != null) {
                                if (value.getBoolean("isOnline")) {
                                    status.setText(ctx.getString(R.string.online));
                                } else {
                                    cal.setTimeInMillis((value.getLong("lastSession")));
                                    status.setText("Offline");
                                }
                            }
                        });
    }

    public boolean isOnline(String userId) {
        database.collection("users")
                .document(userId)
                .addSnapshotListener(
                        (value, exception) -> {
                            if (value.getBoolean("isOnline")) {
                                online = true;
                            } else {
                                online = false;
                            }
                        });

        return this.online;
    }

    public boolean isLogin() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public String getUsername() {
        database.collection("users")
                .document(getUid())
                .addSnapshotListener(
                        (value, exception) -> {
                            if (exception != null) return;
                            if (value != null) {
                                username = value.getString("username");
                            } else {
                                username = "unidentified";
                            }
                        });
        return this.username;
    }

    public String getPassword() {
        /*What the fuck are you think? You're an idiot.*/
        return this.password;
    }
}
