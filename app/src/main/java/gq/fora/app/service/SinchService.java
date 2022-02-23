package gq.fora.app.service;

import android.Manifest;
import android.content.pm.PackageManager;
import static gq.fora.app.activities.calling.IncomingCallScreenActivity.ACTION_ANSWER;
import static gq.fora.app.activities.calling.IncomingCallScreenActivity.ACTION_IGNORE;
import static gq.fora.app.activities.calling.IncomingCallScreenActivity.MESSAGE_ID;
import static gq.fora.app.activities.calling.IncomingCallScreenActivity.NOTIF_ID;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.Internals;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.NotificationResult;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.video.VideoController;
import gq.fora.app.R;
import gq.fora.app.activities.calling.IncomingCallScreenActivity;
import gq.fora.app.initializeApp;
import gq.fora.app.listener.FirebaseCloudMessaging;
import gq.fora.app.models.JWT;
import gq.fora.app.models.UserConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SinchService extends Service {

    /*
    IMPORTANT!

    This sample application was designed to provide the simplest possible way
    to evaluate Sinch Android SDK right out of the box, omitting crucial feature of handling
    incoming calls via managed push notifications, which requires registering in FCM console and
    procuring google-services.json in order to build and work.

    Android 8.0 (API level 26) imposes limitation on background services and we strongly encourage
    you to use Sinch Managed Push notifications to handle incoming calls when app is closed or in
    background or phone is locked.

    DO NOT USE THIS APPLICATION as a skeleton of your project!

    Instead, use:
    - sinch-rtc-sample-push (for audio calls) and
    - sinch-rtc-sample-video-push (for video calls)


    To ensure that SinchClient is not created more than once, make sure that SinchService lifecycle is managed correctly.
    E.g. in this sample apps we demonstrate that the service is bound in the BaseActivity, which is the parent
    class for all activities that uses SinchClient. So, whenever a new activity starts, it's being bound to SinchService.
    Thus, SinchService always has at least one `client` activity and is never terminated while the relevant activities are
    on the foreground. That ensures that there wwould be at most one instance of the SinchClient, which is especially important
    for the registration step.

    SinchClient entry points are 'start()' to make an outbound call, and `relayRemotePushNotificationPayload()` to handle
    an incoming call received via push notification.

    Useful links:

    Activity lifecycle: https://developer.android.com/guide/components/activities/activity-lifecycle
    Bound services lifecycle: https://developer.android.com/guide/components/bound-services#Lifecycle
    Navigating between activities: https://developer.android.com/guide/components/activities/activity-lifecycle.html#tba

    */

    public static final String APP_KEY = initializeApp.context.getString(R.string.SINCH_APP_KEY);
    public static final String APP_SECRET =
            initializeApp.context.getString(R.string.SINCH_APP_SECRET);
    public static final String ENVIRONMENT =
            initializeApp.context.getString(R.string.SINCH_ENVIRONMENT);

    private static final String CHANNEL_ID = "Calls";
    public static final int MESSAGE_PERMISSIONS_NEEDED = 1;
    public static final String REQUIRED_PERMISSION = "REQUIRED_PERMISSION";
    public static final String MESSENGER = "MESSENGER";
    private Messenger messenger;

    public static final String EXTRA_ID = "EXTRA_ID";

    public static final String CALL_ID = "CALL_ID";
    static final String TAG = SinchService.class.getSimpleName();

    private PersistedSettings mSettings;
    private SinchServiceInterface mSinchServiceInterface = new SinchServiceInterface();
    public static SinchClient mSinchClient;

    private StartFailedListener mListener;
    private String mUserId;
    private String display_name;
    private String profile_url;
    private String appKey = "";
    private String appSecret = "";
    private String environment = "";

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");

    @Override
    public void onCreate() {
        super.onCreate();
        mSettings = new PersistedSettings(getApplicationContext());
        attemptAutoStart();
    }

    private void attemptAutoStart() {
        if (messenger != null) {
            start();
        }
    }

    private void createClient(String userId) {
        mSinchClient =
                Sinch.getSinchClientBuilder()
                        .context(getApplicationContext())
                        .userId(userId)
                        .applicationKey(APP_KEY)
                        .environmentHost(ENVIRONMENT)
                        .build();

        mSinchClient.setSupportManagedPush(true);
        mSinchClient.startListeningOnActiveConnection();
        mSinchClient.addSinchClientListener(new ForaSinchClientListener());
        mSinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
        mSinchClient.setPushNotificationDisplayName(UserConfig.getInstance().getDisplayName());
    }

    @Override
    public void onDestroy() {
        if (mSinchClient != null && mSinchClient.isStarted()) {
            mSinchClient.terminateGracefully();
        }
        super.onDestroy();
    }

    private boolean hasUsername() {
        if (mSettings.getUsername().isEmpty()) {
            Log.e(TAG, "Can't start a SinchClient as no username is available!");
            return false;
        }
        return true;
    }

    private void createClientIfNecessary() {
        if (mSinchClient != null) return;
        createClient(UserConfig.getInstance().getUid());
    }

    private void start() {
        boolean permissionsGranted = true;
        createClientIfNecessary();
        try {
            // mandatory checks
            mSinchClient.checkManifest();
            if (getApplicationContext()
                            .checkCallingOrSelfPermission(android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new MissingPermissionException(Manifest.permission.CAMERA);
            }
        } catch (MissingPermissionException e) {
            permissionsGranted = false;
            if (messenger != null) {
                Message message = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putString(REQUIRED_PERMISSION, e.getRequiredPermission());
                message.setData(bundle);
                message.what = MESSAGE_PERMISSIONS_NEEDED;
                try {
                    messenger.send(message);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (permissionsGranted) {
            Log.d(TAG, "Starting SinchClient");
            try {
                mSinchClient.start();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Can't start SinchClient - " + e.getMessage());
            }
        }
    }

    private void stop() {
        if (mSinchClient != null) {
            mSinchClient.terminateGracefully();
            mSinchClient = null;
        }
    }

    private boolean isStarted() {
        return (mSinchClient != null && mSinchClient.isStarted());
    }

    @Override
    public IBinder onBind(Intent intent) {
        messenger = intent.getParcelableExtra(MESSENGER);
        return mSinchServiceInterface;
    }

    public class SinchServiceInterface extends Binder {

        public Call callUser(String userId, Map<String, String> headers) {
            return mSinchClient.getCallClient().callUser(userId, headers);
        }

        public Call callUserVideo(String userId, Map<String, String> headers2) {
            return mSinchClient.getCallClient().callUserVideo(userId, headers2);
        }
		
		public Call callConference(String userId, Map<String, String> headers3) {
			return mSinchClient.getCallClient().callConference(userId, headers3);
		}

        public String getUsername() {
            return mSettings.getUsername();
        }

        public void setUsername(String username) {
            mSettings.setUsername(username);
        }

        public void retryStartAfterPermissionGranted() {
            SinchService.this.attemptAutoStart();
        }

        public boolean isStarted() {
            return SinchService.this.isStarted();
        }

        public void startClient() {
            start();
        }

        public void stopClient() {
            stop();
        }

        public void setStartListener(StartFailedListener listener) {
            mListener = listener;
        }

        public Call getCall(String callId) {
            return mSinchClient != null ? mSinchClient.getCallClient().getCall(callId) : null;
        }

        public VideoController getVideoController() {
            if (!isStarted()) {
                return null;
            }
            return mSinchClient.getVideoController();
        }

        public AudioController getAudioController() {
            if (!isStarted()) {
                return null;
            }
            return mSinchClient.getAudioController();
        }

        public NotificationResult relayRemotePushNotificationPayload(final Map payload) {
            if (!hasUsername()) {
                Log.e(TAG, "Unable to relay the push notification!");
                return null;
            }
            createClientIfNecessary();
            return mSinchClient.relayRemotePushNotificationPayload(payload);
        }
    }

    public interface StartFailedListener {

        void onFailed(SinchError error);

        void onStarted();
    }

    private class ForaSinchClientListener implements SinchClientListener {

        @Override
        public void onClientFailed(SinchClient client, SinchError error) {
            if (mListener != null) {
                mListener.onFailed(error);
            }
            Internals.terminateForcefully(mSinchClient);
            mSinchClient = null;
        }

        @Override
        public void onClientStarted(SinchClient client) {
            Log.d(TAG, "SinchClient started");
            if (mListener != null) {
                mListener.onStarted();
            }
        }

        @Override
        public void onLogMessage(int level, String area, String message) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(area, message);
                    break;
                case Log.ERROR:
                    Log.e(area, message);
                    break;
                case Log.INFO:
                    Log.i(area, message);
                    break;
                case Log.VERBOSE:
                    Log.v(area, message);
                    break;
                case Log.WARN:
                    Log.w(area, message);
                    break;
            }
        }

        @Override
        public void onCredentialsRequired(ClientRegistration arg0) {
            arg0.register(JWT.create(APP_KEY, APP_SECRET, UserConfig.getInstance().getUid()));
        }

        @Override
        public void onUserRegistered() {}

        @Override
        public void onUserRegistrationFailed(SinchError arg0) {}

        @Override
        public void onPushTokenRegistered() {}

        @Override
        public void onPushTokenRegistrationFailed(SinchError arg0) {}
    }

    private class SinchCallClientListener implements CallClientListener {

        @Override
        public void onIncomingCall(CallClient callClient, Call call) {
            Log.d(TAG, "onIncomingCall: " + call.getCallId());
            Intent intent = new Intent(SinchService.this, IncomingCallScreenActivity.class);
            intent.putExtra(EXTRA_ID, call.getHeaders().get("caller_id"));
            intent.putExtra(CALL_ID, call.getCallId());
            intent.putExtra(NOTIF_ID, MESSAGE_ID);
            boolean inForeground = isAppOnForeground(getApplicationContext());
            if (!inForeground) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !inForeground) {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                        .notify(MESSAGE_ID, createIncomingCallNotification(call, intent));
            } else {
                SinchService.this.startActivity(intent);
            }
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses =
                    activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance
                                == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        private Bitmap getBitmap(Context context, int resId) {
            int largeIconWidth =
                    (int)
                            context.getResources()
                                    .getDimension(R.dimen.notification_large_icon_width);
            int largeIconHeight =
                    (int)
                            context.getResources()
                                    .getDimension(R.dimen.notification_large_icon_height);
            Drawable d = context.getResources().getDrawable(resId);
            Bitmap b =
                    Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, largeIconWidth, largeIconHeight);
            d.draw(c);
            return b;
        }

        private PendingIntent getPendingIntent(Intent intent, String action) {
            intent.setAction(action);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            getApplicationContext(),
                            111,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        @TargetApi(29)
        private Notification createIncomingCallNotification(Call call, Intent fullScreenIntent) {

            users.addChildEventListener(
                    new ChildEventListener() {

                        @Override
                        public void onChildAdded(DataSnapshot arg0, String arg1) {
                            GenericTypeIndicator<HashMap<String, Object>> _ind =
                                    new GenericTypeIndicator<HashMap<String, Object>>() {};
                            String key = arg0.getKey();
                            Map<String, Object> map = arg0.getValue(_ind);

                            if (key.equals(call.getHeaders().get("caller_id"))) {
                                if (map.containsKey("first_name")
                                        && map.containsKey("last_name")
                                        && map.containsKey("user_photo")) {
                                    display_name = map.get("display_name").toString();
                                    profile_url = map.get("user_photo").toString();
                                }
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot arg0, String arg1) {}

                        @Override
                        public void onChildRemoved(DataSnapshot arg0) {}

                        @Override
                        public void onChildMoved(DataSnapshot arg0, String arg1) {}

                        @Override
                        public void onCancelled(DatabaseError arg0) {}
                    });
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            getApplicationContext(),
                            112,
                            fullScreenIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(
                                    getApplicationContext(), FirebaseCloudMessaging.CHANNEL_ID)
                            .setContentTitle(display_name)
                            .setContentText("Voice Call")
                            .setLargeIcon(getBitmap(getApplicationContext(), R.drawable.user_icon))
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(pendingIntent)
                            .setFullScreenIntent(pendingIntent, true)
                            .addAction(
                                    R.drawable.button_accept,
                                    "Answer",
                                    getPendingIntent(fullScreenIntent, ACTION_ANSWER))
                            .addAction(
                                    R.drawable.button_decline,
                                    "Ignore",
                                    getPendingIntent(fullScreenIntent, ACTION_IGNORE))
                            .setOngoing(true);
            return builder.build();
        }
    }

    private class PersistedSettings {

        private SharedPreferences mStore;

        private static final String PREF_KEY = "Sinch";

        public PersistedSettings(Context context) {
            mStore = context.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        }

        public String getUsername() {
            return mStore.getString("Username", "");
        }

        public void setUsername(String username) {
            SharedPreferences.Editor editor = mStore.edit();
            editor.putString("Username", username);
            editor.commit();
        }
    }

    private void createNotificationChannel(int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Fora";
            String description = "Fora Push Notifications.";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
