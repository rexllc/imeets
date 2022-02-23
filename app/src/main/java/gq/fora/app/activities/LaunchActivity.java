package gq.fora.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.PushTokenRegistrationCallback;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.UserController;
import com.sinch.android.rtc.UserRegistrationCallback;

import gq.fora.app.R;
import gq.fora.app.activities.calling.BaseActivity;
import gq.fora.app.activities.surface.SelectAccount;
import gq.fora.app.models.JWT;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.FileUtils;

import java.util.Set;

public class LaunchActivity extends BaseActivity
        implements SinchService.StartFailedListener,
                PushTokenRegistrationCallback,
                UserRegistrationCallback {

    private String TAG = "FORA";
    private String key_intent;
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String id = Build.ID;
    private String name = Build.MANUFACTURER + "-" + Build.MODEL;
    private SharedPreferences data;
    private String mUserId;
    private SharedPreferences sharedPreferences;
    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");
    private ProgressBar loader;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseRemoteConfigSettings configSettings;

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        View view = window.getDecorView();
        sharedPreferences = getSharedPreferences("themes", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_launch);
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            insets.setAppearanceLightStatusBars(false);
            window.setStatusBarColor(0xFF212121);
        } else {
            insets.setAppearanceLightStatusBars(true);
            window.setStatusBarColor(0xFFFFFFFF);
        }
        initializeBundle(savedInstanceState);
        initializeLogic();
    }

    @Override
    public void onFailed(SinchError error) {
        // onClientFailed
    }

    @Override
    public void onStarted() {
        // onClientStarted
        openActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void initializeBundle(Bundle savedInstanceState) {
        loader = findViewById(R.id.loader);
        mAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        data = getSharedPreferences("token", Activity.MODE_PRIVATE);
    }

    public void initializeLogic() {
        loader.getIndeterminateDrawable().setTint(R.color.primary);
        android.content.pm.ShortcutManager shortcutManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager = getSystemService(android.content.pm.ShortcutManager.class);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (shortcutManager != null) {
                ShortcutInfo appLauncher =
                        new ShortcutInfo.Builder(LaunchActivity.this, "launcher")
                                .setShortLabel("iMeets")
                                .setLongLabel("iMeets Messenger")
                                .setRank(1)
                                .setIntent(
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                null,
                                                LaunchActivity.this,
                                                LaunchActivity.class))
                                .setIcon(
                                        Icon.createWithResource(
                                                LaunchActivity.this, R.drawable.ic_launcher))
                                .build();
                shortcutManager.setDynamicShortcuts(java.util.Arrays.asList(appLauncher));
            }
        } else {
        }
    }

    @Override
    protected void onServiceConnected() {

        if (getSinchServiceInterface().isStarted()) {
            openActivity();
        } else {
            getSinchServiceInterface().setStartListener(LaunchActivity.this);
        }

        if (UserConfig.getInstance().isLogin()) {
            UserController uc =
                    Sinch.getUserControllerBuilder()
                            .context(getApplicationContext())
                            .applicationKey(SinchService.APP_KEY)
                            .userId(UserConfig.getInstance().getUid())
                            .environmentHost(SinchService.ENVIRONMENT)
                            .build();
            uc.registerUser(this, this);
        } else {
            openActivity();
        }
    }

    public void openActivity() {
        if (UserConfig.getInstance().isLogin()) {
            if (getIntent().hasExtra("id")) {
                Intent intent = new Intent();
                intent.setClass(this, ChatActivity.class);
                intent.putExtra("id", getIntent().getStringExtra("id"));
                startActivity(intent);
            } else {
                try {
                    Intent intent = getIntent();
                    String action = intent.getAction();
                    Uri uri = intent.getData();

                    key_intent = uri.toString();

                    if (uri != null) {
                        if (key_intent.contains("https://m.imeets.gq/profile/?id=")) {
                            users.orderByChild("username")
                                    .equalTo(
                                            uri.toString()
                                                    .replace("https://m.imeets.gq/profile/?id=", ""))
                                    .addListenerForSingleValueEvent(
                                            new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        for (DataSnapshot dataSnapshot :
                                                                snapshot.getChildren()) {
                                                            User user =
                                                                    dataSnapshot.getValue(
                                                                            User.class);
                                                            if (user != null) {
                                                                Fragment profileFragment =
                                                                        new ProfilePageActivity();
                                                                Bundle bundle1 = new Bundle();
                                                                getSupportFragmentManager()
                                                                        .beginTransaction()
                                                                        .setTransition(
                                                                                FragmentTransaction
                                                                                        .TRANSIT_FRAGMENT_OPEN)
                                                                        .add(
                                                                                android.R
                                                                                        .id
                                                                                        .content,
                                                                                profileFragment)
                                                                        .addToBackStack(null)
                                                                        .commit();
                                                                bundle1.putString(
                                                                        "userId", user.userId);
                                                                profileFragment.setArguments(
                                                                        bundle1);
                                                            } else {
                                                                getSupportFragmentManager()
                                                                        .beginTransaction()
                                                                        .replace(
                                                                                android.R
                                                                                        .id
                                                                                        .content,
                                                                                new SplashActivity())
                                                                        .setTransition(
                                                                                FragmentTransaction
                                                                                        .TRANSIT_FRAGMENT_FADE)
                                                                        .addToBackStack(null)
                                                                        .commit();
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError error) {}
                                            });
                        } else if (key_intent.contains("https://www.imeets.gq/__")) {

                            String server = uri.getAuthority();
                            String path = uri.getPath();
                            String protocol = uri.getScheme();
                            Set<String> args = uri.getQueryParameterNames();
                            String oobCode = uri.getQueryParameter("oobCode");
                            String lang = uri.getQueryParameter("lang");

                            if (uri != null) {
                                Intent intent1 =
                                        new Intent(
                                                LaunchActivity.this,
                                                VerifyPasswordResetActivity.class);
                                intent1.putExtra("oobCode", oobCode);
                                intent1.putExtra("lang", lang);
                                startActivity(intent1);
                            }
                        }
                    }

                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(android.R.id.content, new ChatListActivity())
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(android.R.id.content, new SplashActivity())
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }
        } else {
            try {
                Intent intent = getIntent();
                Uri data = intent.getData();
                String shared_data = data.toString();
                Uri uri = Uri.parse(shared_data);
                String server = uri.getAuthority();
                String path = uri.getPath();
                String protocol = uri.getScheme();
                Set<String> args = uri.getQueryParameterNames();
                String oobCode = uri.getQueryParameter("oobCode");
                String lang = uri.getQueryParameter("lang");

                if (uri != null) {
                    if (key_intent.contains("https://account.fora.gq/")) {
                        Intent intent1 =
                                new Intent(LaunchActivity.this, VerifyPasswordResetActivity.class);
                        intent1.putExtra("oobCode", oobCode);
                        intent1.putExtra("lang", lang);
                        startActivity(intent1);
                    }
                }

            } catch (Exception e) {
                String path =
                        FileUtils.getPackageDataDir(getApplicationContext())
                                + "/user/accounts.json";
                if (FileUtils.isExistFile(path)) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, new SelectAccount())
                            .addToBackStack(null)
                            .commit();
                } else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(android.R.id.content, new AuthActivity())
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    }

    public void startClientAndOpenActivity() {
        if (!getSinchServiceInterface().isStarted()) {
            if (UserConfig.getInstance().isLogin()) {
                getSinchServiceInterface().startClient();
            }
        }
    }

    @Override
    public void onCredentialsRequired(ClientRegistration arg0) {
        if (UserConfig.getInstance().isLogin()) {
            arg0.register(
                    JWT.create(
                            SinchService.APP_KEY,
                            SinchService.APP_SECRET,
                            UserConfig.getInstance().getUid()));
        }
    }

    @Override
    public void onUserRegistered() {}

    @Override
    public void onUserRegistrationFailed(SinchError arg0) {
        Toast.makeText(getApplicationContext(), arg0.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPushTokenRegistered() {
        startClientAndOpenActivity();
    }

    @Override
    public void onPushTokenRegistrationFailed(SinchError arg0) {}
}
