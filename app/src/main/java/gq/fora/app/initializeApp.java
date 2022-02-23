package gq.fora.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;
import androidx.work.Configuration;
import com.cloudinary.android.MediaManager;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.ios.IosEmojiProvider;
import gq.fora.app.activities.DebugActivity;
import gq.fora.app.models.UserConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class initializeApp extends Application
        implements CameraXConfig.Provider, Configuration.Provider {

    private String TAG = "FORA";
    public static String token;
    private SharedPreferences data;
    private SharedPreferences sharedPreferences;
    public static Context context;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        data = getSharedPreferences("token", Activity.MODE_PRIVATE);
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(
                        new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if (!task.isSuccessful()) {
                                    Log.w(
                                            TAG,
                                            "Fetching FCM registration token failed",
                                            task.getException());
                                    return;
                                }

                                // Get new FCM registration token
                                token = task.getResult();
                                data.edit().putString("token", token).commit();

                                if (UserConfig.getInstance().isLogin()) {
                                    checkToken(token);
                                }

                                // Log and toast
                                String msg = getString(R.string.msg_token_fmt, token);
                                Log.d(TAG, msg);
                            }
                        });
        context = getApplicationContext();
        sharedPreferences = getSharedPreferences("themes", Context.MODE_PRIVATE);
        // Emoji Installer
        EmojiManager.install(new IosEmojiProvider());
        MobileAds.initialize(this);

        List<String> testDeviceIds = Arrays.asList("863BA90934F03C1DBAEF27BB5D73828E");
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build());

        /*Install Cloudinary*/
        MediaManager.init(this);

        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable throwable) {
                        Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("error", Log.getStackTraceString(throwable));
                        PendingIntent pendingIntent =
                                PendingIntent.getActivity(
                                        getApplicationContext(),
                                        11111,
                                        intent,
                                        PendingIntent.FLAG_ONE_SHOT);

                        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent);

                        Process.killProcess(Process.myPid());
                        System.exit(1);

                        uncaughtExceptionHandler.uncaughtException(thread, throwable);
                    }
                });

        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        return super.startForegroundService(service);
    }

    public void checkToken(String token) {

        FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
        DatabaseReference users = _firebase.getReference("users");

        users.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot _dataSnapshot) {

                        try {
                            GenericTypeIndicator<HashMap<String, Object>> _ind =
                                    new GenericTypeIndicator<HashMap<String, Object>>() {};
                            for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                                HashMap<String, Object> _map = _data.getValue(_ind);

                                if (_map.containsKey("userId")) {
                                    if (_map.get("userId")
                                            .toString()
                                            .equals(UserConfig.getInstance().getUid())) {
                                        if (!_map.get("fcm_token").toString().equals(token)) {
                                            UserConfig.getInstance().updateToken(token);
                                        }
                                    }
                                }
                            }
                        } catch (Exception _e) {
                            _e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError _databaseError) {}
                });
    }
}
