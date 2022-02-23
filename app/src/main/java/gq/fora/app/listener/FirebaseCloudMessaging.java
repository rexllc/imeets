package gq.fora.app.listener;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sinch.android.rtc.NotificationResult;
import com.sinch.android.rtc.SinchHelpers;
import com.sinch.android.rtc.calling.CallNotificationResult;

import gq.fora.app.R;
import gq.fora.app.activities.LaunchActivity;
import gq.fora.app.models.UserConfig;
import gq.fora.app.notify.Notify;
import gq.fora.app.service.SinchService;

import java.util.Map;

public class FirebaseCloudMessaging extends FirebaseMessagingService {

    public static String CHANNEL_ID = "Fora Push Notifications";
    private static final String TAG = FirebaseCloudMessaging.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map data = remoteMessage.getData();

        // Optional: inspect the payload w/o starting Sinch Client and thus avoiding
        // onIncomingCall()
        // e.g. useful to fetch user related polices (blacklist), resources (to show a picture,
        // etc).
        NotificationResult result =
                SinchHelpers.queryPushNotificationPayload(getApplicationContext(), data);
        if (result.isValid() && result.isCall()) {
            CallNotificationResult callResult = result.getCallResult();
            Log.d(
                    TAG,
                    "queryPushNotificationPayload() -> display name: " + result.getDisplayName());
            if (callResult != null) {
                Log.d(TAG, "queryPushNotificationPayload() -> headers: " + result.getCallResult());
                Log.d(
                        TAG,
                        "queryPushNotificationPayload() -> remote user ID: "
                                + result.getCallResult().getRemoteUserId());
            }
        }

        // Mandatory: forward payload to the SinchClient.
        if (SinchHelpers.isSinchPushPayload(data)) {
            new ServiceConnection() {
                private Map payload;

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (payload != null) {
                        SinchService.SinchServiceInterface sinchService =
                                (SinchService.SinchServiceInterface) service;
                        if (sinchService != null) {
                            NotificationResult result =
                                    sinchService.relayRemotePushNotificationPayload(payload);
                            if (result != null) {
                                if (result.isValid() && result.isCall()) {
                                    // Optional: handle result, e.g. show a notification or similar.
                                }
                            }
                        }
                    }
                    payload = null;
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {}

                public void relayMessageData(Map<String, String> data) {
                    payload = data;
                    createNotificationChannel(NotificationManager.IMPORTANCE_MAX);
                    getApplicationContext()
                            .bindService(
                                    new Intent(getApplicationContext(), SinchService.class),
                                    this,
                                    BIND_AUTO_CREATE);
                }
            }.relayMessageData(data);
        }
		
		String bigPictureUrl = data.containsKey("bigPictureUrl") ? data.get("bigPictureUrl").toString() : "null";

        if (remoteMessage.getNotification() != null) {
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.putExtra("id", data.get("chat_id").toString());
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            getApplicationContext(), 112, intent, PendingIntent.FLAG_IMMUTABLE);
            Notify.create(this)
                    .setTitle(remoteMessage.getNotification().getTitle())
                    .setContent(remoteMessage.getNotification().getBody())
                    .setLargeIcon(data.get("image_url").toString())
                    .circleLargeIcon()
                    .setSmallIcon(R.drawable.ic_launcher_round)
					.setBigPicture(bigPictureUrl)
                    .setChannelId("iMeets")
                    .setChannelName("Messages")
                    .setChannelDescription("Default channel for iMeets messages.")
                    .setColor(R.color.primary)
                    .setImportance(Notify.Importance.MAX)
                    .enableVibration(true)
                    .setAction(intent)
                    .show();
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Sent token to the server
        if (UserConfig.getInstance().isLogin()) {
            UserConfig.getInstance().updateToken(token);
        }
    }

    private void createNotificationChannel(int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "iMeets Calls";
            String description = "iMeets Calls";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
