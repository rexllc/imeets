package gq.fora.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.analytics.FirebaseAnalytics;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;
import dev.shreyaspatil.MaterialDialog.model.TextAlignment;
import gq.fora.app.utils.Utils;

public class DebugActivity extends Activity {

    private String madeErrorMessage = "";
    private String errorMessage = "";
    private FirebaseAnalytics mFirebaseAnalytics;

    private String[] exceptionTypes = {
        "StringIndexOutOfBoundsException",
        "IndexOutOfBoundsException",
        "ArithmeticException",
        "NumberFormatException",
        "ActivityNotFoundException"
    };

    private String[] exceptionMessages = {
        "Invalid string operation\n",
        "Invalid list operation\n",
        "Invalid arithmetical operation\n",
        "Invalid toNumber block operation\n",
        "Invalid intent operation"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Intent intent = getIntent();

        if (intent != null) {
            errorMessage = intent.getStringExtra("error");

            String[] split = errorMessage.split("\n");
            // errorMessage = split[0];
            try {
                for (int j = 0; j < exceptionTypes.length; j++) {
                    if (split[0].contains(exceptionTypes[j])) {
                        madeErrorMessage = exceptionMessages[j];

                        int addIndex =
                                split[0].indexOf(exceptionTypes[j]) + exceptionTypes[j].length();

                        madeErrorMessage += split[0].substring(addIndex, split[0].length());
                        madeErrorMessage += "\n\nDetailed error message:\n" + errorMessage;
                        break;
                    }
                }

                if (madeErrorMessage.isEmpty()) {
                    madeErrorMessage = errorMessage;
                }
            } catch (Exception e) {
                madeErrorMessage =
                        madeErrorMessage
                                + "\n\nError while getting error: "
                                + Log.getStackTraceString(e);
            }
        }

        MaterialDialog mDialog =
                new MaterialDialog.Builder(DebugActivity.this)
                        .setTitle("Error", TextAlignment.CENTER)
                        .setMessage(madeErrorMessage, TextAlignment.START)
                        .setCancelable(false)
                        .setPositiveButton(
                                "Send Log",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        // Operation
                                        Bundle bundle = new Bundle();
                                        bundle.putString("device", Build.MANUFACTURER);
                                        bundle.putString("user", Build.MODEL);
										bundle.putString("error", madeErrorMessage);
                                        mFirebaseAnalytics.logEvent(
                                                "Error Logs", bundle);
										mFirebaseAnalytics.setUserProperty("device_version", Build.VERSION.RELEASE);
                                        Toast.makeText(
                                                        DebugActivity.this,
                                                        "Log sent.",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                })
                        .setNegativeButton(
                                "Copy Log",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        Utils textCopy = new Utils(DebugActivity.this);
                                        textCopy.copyText(madeErrorMessage);
                                        Toast.makeText(
                                                        DebugActivity.this,
                                                        "Log copied.",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                        finish();
                                        dialogInterface.dismiss();
                                    }
                                })
                        .build();

        // Show Dialog
        mDialog.show();
    }
}
