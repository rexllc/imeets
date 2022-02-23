package gq.fora.app.activities.result;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.activity.result.contract.ActivityResultContract;
import gq.fora.app.activities.CameraActivity;

public class ResultCallback extends ActivityResultContract<String, String> {

    private String data;

    @Override
    public Intent createIntent(Context context, String input) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra("uri", input);
        context.startActivity(intent);
        return intent;
    }

    @Override
    public String parseResult(int resultCode, Intent intent) {
        if (resultCode != Activity.RESULT_OK) {
            data = null;
        } else {
            String data = intent.getStringExtra("data");
        }
        return data;
    }
}
