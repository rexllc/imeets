package gq.fora.app.notify;

import android.app.Notification;
import static gq.fora.app.notify.helpers.ResourcesHelper.getStringResourceByKey;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import gq.fora.app.R;
import gq.fora.app.notify.exceptions.NotifyDefaultChannelInfoNotFoundException;
import gq.fora.app.notify.helpers.BitmapHelper;

public class Notify {
    public NotificationCompat.Builder getNotificationBuilder() {
        return builder;
    }

    public enum Importance {
        MIN,
        LOW,
        HIGH,
        MAX
    }

    public interface DefaultChannelKeys {
        String ID = "notify_channel_id",
                NAME = "notify_channel_name",
                DESCRIPTION = "notify_channel_description";
    }

    private Context context;

    private String channelId;
    private String channelName;
    private String channelDescription;

    private Object largeIcon, bigPicture;
    private String title, content;
    private int id, smallIcon, oreoImportance, importance, color;
    private Intent action;
    private long[] vibrationPattern;
    private boolean autoCancel, vibration, circle;
    private NotificationCompat.Builder builder;
    private int progress;
    private int max;
    private String group_id;
    private String group_name;
	private boolean indeterminate;

    public Notify(Context _context) {
        this.context = _context;

        ApplicationInfo applicationInfo = this.context.getApplicationInfo();

        this.id = (int) System.currentTimeMillis();

        try {

            this.channelId = getStringResourceByKey(context, DefaultChannelKeys.ID);
            this.channelName = getStringResourceByKey(context, DefaultChannelKeys.NAME);
            this.channelDescription =
                    getStringResourceByKey(context, DefaultChannelKeys.DESCRIPTION);

        } catch (Resources.NotFoundException e) {
            throw new NotifyDefaultChannelInfoNotFoundException();
        }

        this.title = "Notify";
        this.content = "Hello world!";
        this.largeIcon = applicationInfo.icon;
        this.smallIcon = applicationInfo.icon;
        this.bigPicture = null;
        this.progress = 0;
        this.max = 0;
        this.group_id = null;
        this.group_name = null;
		this.indeterminate = false;

        builder = new NotificationCompat.Builder(context, channelId);

        this.color = -1;
        this.action = null;
        this.vibration = true;
        this.vibrationPattern = new long[] {0, 250, 250, 250};
        this.autoCancel = false;
        this.circle = false;
        this.setImportanceDefault();
    }

    public static Notify create(@NonNull Context context) {
        return new Notify(context);
    }

    public void show() {
        if (context == null) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (this.group_id != null && this.group_name != null) {
            // The id of the group.
            String groupId = this.group_id;
            // The user-visible name of the group.
            CharSequence groupName = this.group_name;
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(groupId, groupName));
        }

        Uri path = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.tweet);
        RingtoneManager.setActualDefaultRingtoneUri(
                context.getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION, path);

        builder.setAutoCancel(this.autoCancel)
                .setSound(path, AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        Bitmap largeIconBitmap;
        if (largeIcon instanceof String)
            largeIconBitmap = BitmapHelper.getBitmapFromUrl(String.valueOf(largeIcon));
        else largeIconBitmap = BitmapHelper.getBitmapFromRes(this.context, (int) largeIcon);

        if (largeIconBitmap != null) {
            if (this.circle) largeIconBitmap = BitmapHelper.toCircleBitmap(largeIconBitmap);
            builder.setLargeIcon(largeIconBitmap);
        }

        if (bigPicture != null) {
            Bitmap bigPictureBitmap;
            if (bigPicture instanceof String)
                bigPictureBitmap = BitmapHelper.getBitmapFromUrl(String.valueOf(bigPicture));
            else bigPictureBitmap = BitmapHelper.getBitmapFromRes(this.context, (int) bigPicture);

            if (bigPictureBitmap != null) {
                NotificationCompat.BigPictureStyle bigPictureStyle =
                        new NotificationCompat.BigPictureStyle()
                                .bigPicture(bigPictureBitmap)
                                .setSummaryText(content);
                bigPictureStyle.bigLargeIcon(largeIconBitmap);
                builder.setStyle(bigPictureStyle);
            }
        }

        int realColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            realColor = color == -1 ? Color.BLACK : context.getResources().getColor(color, null);
        else realColor = color == -1 ? Color.BLACK : context.getResources().getColor(color);

        builder.setColor(realColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /*
             * OREO^ NOTIFICATION CHANNEL
             * */
            NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, channelName, oreoImportance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(realColor);
            notificationChannel.setDescription(this.channelDescription);
            notificationChannel.setVibrationPattern(this.vibrationPattern);
            notificationChannel.enableVibration(this.vibration);

            notificationManager.createNotificationChannel(notificationChannel);
        } else {
            builder.setPriority(this.importance);
        }

        if (this.vibration) builder.setVibrate(this.vibrationPattern);
        else builder.setVibrate(new long[] {0});

        if (this.action != null) {
            PendingIntent pi =
                    PendingIntent.getActivity(
                            context, id, this.action, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.setContentIntent(pi);
        }

        if (this.progress != 0 && this.max != 0) {
            builder.setProgress(this.max, this.progress, this.indeterminate);
        }

        notificationManager.notify(id, builder.build());
    }

    public Notify setTitle(@NonNull String title) {
        if (!title.trim().isEmpty()) this.title = title.trim();
        return this;
    }

    public Notify setContent(@NonNull String content) {
        if (!content.trim().isEmpty()) this.content = content.trim();
        return this;
    }

    public Notify setChannelId(@NonNull String channelId) {
        if (!channelId.trim().isEmpty()) {
            this.channelId = channelId.trim();
            this.builder.setChannelId(channelId);
        }
        return this;
    }

    public Notify setChannelName(@NonNull String channelName) {
        if (!channelName.trim().isEmpty()) this.channelName = channelName.trim();
        return this;
    }

    public Notify setChannelDescription(@NonNull String channelDescription) {
        if (!channelDescription.trim().isEmpty())
            this.channelDescription = channelDescription.trim();
        return this;
    }

    public Notify setImportance(@NonNull Importance importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (importance) {
                case MIN:
                    this.oreoImportance = NotificationManager.IMPORTANCE_MIN;
                    break;

                case LOW:
                    this.importance = Notification.PRIORITY_LOW;
                    this.oreoImportance = NotificationManager.IMPORTANCE_LOW;
                    break;

                case HIGH:
                    this.importance = Notification.PRIORITY_HIGH;
                    this.oreoImportance = NotificationManager.IMPORTANCE_HIGH;
                    break;

                case MAX:
                    this.importance = Notification.PRIORITY_MAX;
                    this.oreoImportance = NotificationManager.IMPORTANCE_MAX;
                    break;
            }
        }
        return this;
    }

    private void setImportanceDefault() {
        this.importance = Notification.PRIORITY_DEFAULT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.oreoImportance = NotificationManager.IMPORTANCE_DEFAULT;
        }
    }

    public Notify enableVibration(boolean vibration) {
        this.vibration = vibration;
        return this;
    }

    public Notify setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
        return this;
    }

    public Notify circleLargeIcon() {
        this.circle = true;
        return this;
    }

    public Notify setVibrationPattern(long[] vibrationPattern) {
        this.vibrationPattern = vibrationPattern;
        return this;
    }

    public Notify setColor(@ColorRes int color) {
        this.color = color;
        return this;
    }

    public Notify setSmallIcon(@DrawableRes int smallIcon) {
        this.smallIcon = smallIcon;
        return this;
    }

    public Notify setLargeIcon(@DrawableRes int largeIcon) {
        this.largeIcon = largeIcon;
        return this;
    }

    public Notify setLargeIcon(@NonNull String largeIconUrl) {
        this.largeIcon = largeIconUrl;
        return this;
    }

    public Notify setBigPicture(@DrawableRes int bigPicture) {
        this.bigPicture = bigPicture;
        return this;
    }

    public Notify setBigPicture(@NonNull String bigPictureUrl) {
        this.bigPicture = bigPictureUrl;
        return this;
    }

    public Notify setAction(@NonNull Intent action) {
        this.action = action;
        return this;
    }

    public Notify setId(int id) {
        this.id = id;
        return this;
    }

    public Notify setGroup(String id, String name) {
        this.group_id = id;
        this.group_name = name;
        return this;
    }

    public Notify setProgress(@NonNull int max, @NonNull int progress, boolean indeterminate) {
        this.max = max;
        this.progress = progress;
		this.indeterminate = indeterminate;
        return this;
    }

    public int getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelDescription() {
        return channelDescription;
    }

    public int getProgress() {
        return progress;
    }

    public static void cancel(@NonNull Context context, int id) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancel(id);
    }

    public static void cancelAll(@NonNull Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancelAll();
    }
}
