package gq.fora.app.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;

import gq.fora.app.utils.ForaUtil;

import java.util.Calendar;

@IgnoreExtraProperties
public class User {

    private Calendar cal = Calendar.getInstance();
    private static User sInstance = null;

    public String userId;
    public String email;
    public long timestamp;
    public String firstName;
    public String lastName;
    public String displayName;
    public String username;
    public String userPhoto;
    public String fcmToken;
    public boolean iAdmin;
    public boolean isBanned;
    public String status;
    public boolean isOnline;
    public long lastSession;
    public boolean isVerified;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    @NonNull
    public static User getInstance() {

        if (sInstance == null) {
            sInstance = new User();
        }

        return sInstance;
    }

    public User(
            String userId,
            String email,
            String password,
            String first_name,
            String last_name,
            String token) {
        this.userId = userId;
        this.email = email;
        this.firstName = first_name;
        this.lastName = last_name;
        this.displayName = first_name + " " + last_name;
        this.username =
                first_name.toLowerCase()
                        + "."
                        + last_name.toLowerCase()
                        + "."
                        + String.valueOf((long) (ForaUtil.getRandom((int) (2), (int) (9999))));
        this.fcmToken = token;
        this.timestamp = cal.getTimeInMillis();
        this.lastSession = cal.getTimeInMillis();
        this.userPhoto =
                "https://firebasestorage.googleapis.com/v0/b/fora-store.appspot.com/o/images%2FWindows-10-user-icon-big.png?alt=media&token=14f5a679-de20-49ca-ae7f-0df1f16e3660";
        this.iAdmin = false;
        this.isBanned = false;
        this.status = "active";
        this.isOnline = false;
        this.isVerified = false;
    }
}
