package gq.fora.app.utils;

import android.animation.*;
import android.app.*;
import android.app.AlertDialog;
import android.content.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.Signature;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.res.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.os.Environment;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ProgressBar;

import androidx.annotation.*;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.ContextCompat;

import gq.fora.app.R;

import org.json.*;

import java.io.File;
import java.text.*;
import java.util.*;
import java.util.Locale;
import java.util.regex.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static Context mContext;

    private static Utils mInstance = null;

    public Utils(Context mContext) {
        this.mContext = mContext;
    }

    public static Utils getInstance(final Context ctx) {
        if (mInstance == null) {
            mInstance = new Utils(ctx);
        }

        return mInstance;
    }

    public void clearCache(Context context) {

        try {
            File dir = context.getCacheDir();
            deleteDir(dir);

        } catch (Exception e) {

        }
    }

    public boolean deleteDir(File dir) {

        if (dir != null && dir.isDirectory()) {

            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {

                boolean success = deleteDir(new File(dir, children[i]));

                if (!success) {

                    return false;
                }
            }

            return dir.delete();

        } else if (dir != null && dir.isFile()) {

            return dir.delete();

        } else {

            return false;
        }
    }

    public void setLightNavigationBar(Window window, boolean enable) {

        View view = window.getDecorView();
		WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
		if (enable) {
			insets.setAppearanceLightNavigationBars(true);
		} else {
			insets.setAppearanceLightNavigationBars(false);
		}
    }

    public static String getRootDirPath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file =
                    ContextCompat.getExternalFilesDirs(context.getApplicationContext(), null)[0];
            return file.getAbsolutePath();
        } else {
            return context.getApplicationContext().getFilesDir().getAbsolutePath();
        }
    }

    public static String getProgressDisplayLine(long currentBytes, long totalBytes) {
        return getBytesToMBString(currentBytes) + "/" + getBytesToMBString(totalBytes);
    }

    private static String getBytesToMBString(long bytes) {
        return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.00 * 1024.00));
    }

    public boolean isValidEmail(String _emailStr) {
        Pattern validate =
                Pattern.compile(
                        "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$",
                        Pattern.CASE_INSENSITIVE);
        Matcher matcher = validate.matcher(_emailStr);
        return matcher.matches();
    }

    public String getSignture(final Context context) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager()
                            .getPackageInfo(
                                    context.getPackageName(),
                                    PackageManager.GET_SIGNING_CERTIFICATES);
            // note sample just checks the first signature
            for (Signature signature : packageInfo.signatures) {
                // SHA1 the signature
                String sha1 = getSHA1_(signature.toByteArray());
                // check is matches hardcoded value
                return sha1;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        return "No valid signature.";
    }

    // computed the sha1 hash of the signature
    public final String getSHA1_(byte[] sig) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA1");
            digest.update(sig);
            byte[] hashtext = digest.digest();
            return bytes_To_Hex_(hashtext);
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    // util method to convert byte array to hex string
    public String bytes_To_Hex_(byte[] bytes) {
        char[] hexArray = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static final void rippleRoundStroke(
            final View _view,
            final String _focus,
            final String _pressed,
            final double _round,
            final double _stroke,
            final String _strokeclr) {
        final GradientDrawable GG = new GradientDrawable();
        GG.setColor(Color.parseColor(_focus));
        GG.setCornerRadius((float) _round);
        GG.setStroke((int) _stroke, Color.parseColor("#" + _strokeclr.replace("#", "")));
        final RippleDrawable RE =
                new android.graphics.drawable.RippleDrawable(
                        new android.content.res.ColorStateList(
                                new int[][] {new int[] {}}, new int[] {Color.parseColor(_pressed)}),
                        GG,
                        null);
        _view.setBackground(RE);
    }

    public void showLoading(@NonNull boolean isLoading) {
        try {
            final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final View convertView = inflater.inflate(R.layout.loading_view, null);
            final ProgressBar progressbar1 =
                    (ProgressBar) convertView.findViewById(R.id.progressbar1);
            alertDialog.setCancelable(false);
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            if (isLoading) {
                alertDialog.show();
            } else {
                alertDialog.dismiss();
            }
        } catch (Exception e) {

        }
    }

    public static void copyText(final String text) {
        final ClipboardManager clipboard =
                (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(null, text);
        clipboard.setPrimaryClip(clip);
    }

    public static void rippleEffects(View _view, String _color) {
        final ColorStateList clr =
                new ColorStateList(
                        new int[][] {new int[] {}}, new int[] {Color.parseColor(_color)});
        final RippleDrawable ripdr = new RippleDrawable(clr, null, null);
        _view.setBackground(ripdr);
    }

    public static String formatTimespan(int totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public static boolean isNotificationChannelEnabled(
            Context context, @Nullable String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!TextUtils.isEmpty(channelId)) {
                NotificationManager manager =
                        (NotificationManager)
                                context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
            return false;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    public static boolean isContainsLinks(String text) {
        Pattern p =
                Pattern.compile(
                        "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$");
        Matcher m = p.matcher(text); // replace with string to compare
        return m.find();
    }

    private static final Pattern urlPattern =
            Pattern.compile(
                    "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static String getLinks(String text) {
        Matcher matcher = urlPattern.matcher(text);
        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            // now you have the offsets of a URL match
        }

        return text.substring(matcher.start(0), matcher.end(0));
    }

    public void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
}
