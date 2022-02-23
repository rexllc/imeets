package gq.fora.app.models.list.viewmodel;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String userId;
    public String email;
    public long timestamp;
    public String firstName;
    public String lastName;
    public String displayName;
    public String username;
    public String userPhoto;
    public String fcmToken;
    public boolean isAdmin;
    public boolean isBanned;
    public String status;
    public boolean isOnline;
    public long lastSession;
    public boolean isVerified;
    public String middleName;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }
}
