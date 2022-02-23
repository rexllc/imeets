package gq.fora.app.models;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import gq.fora.app.R;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.ForaUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserConfig {

    private static UserConfig sInstance = null;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private ChildEventListener _users_child_listener;
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
        map.put("fcm_token", token);
        users.child(getUid()).updateChildren(map);
    }

    public void updateStatus(String userId, boolean isOnline) {
        Map<String, Object> map1 = new HashMap<>();
        if (isOnline) {
            map1.put("online", true);
        } else {
            map1.put("online", false);
        }
        map1.put("last_session", cal.getTimeInMillis());
        users.child(userId).updateChildren(map1);
        // To update when the user lose connection.
        users.child(getUid()).onDisconnect().updateChildren(map1);
    }

    public void setStatus(Context ctx, TextView status, String userId) {

        users.child(userId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot _dataSnapshot) {

                                User user2 = _dataSnapshot.getValue(User.class);

                                if (user2 != null) {
                                    if (user2.isOnline) {
                                        status.setText(ctx.getString(R.string.online));
                                    } else {
                                        cal.setTimeInMillis((user2.lastSession));
                                        status.setText(
                                                ctx.getString(
                                                        R.string.last_seen,
                                                        new SimpleDateFormat(
                                                                        "MM, d, yyyy", Locale.US)
                                                                .format(cal.getTime())));
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError _databaseError) {}
                        });
    }

    public boolean isOnline(String userId) {

        users.child(userId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot _dataSnapshot) {

                                User user = _dataSnapshot.getValue(User.class);

                                if (user != null) {
                                    if (user.isOnline) {
                                        online = true;
                                    } else {
                                        online = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError _databaseError) {}
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
        users.child(getUid())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot _dataSnapshot) {

                                User userModel = _dataSnapshot.getValue(User.class);

                                if (userModel != null) {
                                    if (userModel.username != null) {
                                        username = userModel.username;
                                    } else {
                                        Map<String, Object> data = new HashMap<>();
                                        data.put(
                                                "username",
                                                userModel
                                                                .displayName
                                                                .toLowerCase()
                                                                .replace(" ", ".")
                                                        + "."
                                                        + String.valueOf(
                                                                (long)
                                                                        (ForaUtil.getRandom(
                                                                                (int) (2),
                                                                                (int) (9999)))));
                                        users.child(getUid()).updateChildren(data);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError _databaseError) {}
                        });

        return this.username;
    }

    public String getPassword() {
        users.child(getUid())
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
								User user = dataSnapshot.getValue(User.class);
								if (user != null) {
									
								}
							}

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
						
						
		return this.password;
    }
}
