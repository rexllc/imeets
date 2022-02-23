package gq.fora.app.service;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.ListenerService;
import gq.fora.app.activities.ChatActivity;
import java.util.Map;

public class UploadTaskService extends ListenerService {

    @Override
    public void onStart(String requestId) {
        // your code here
    }

    @Override
    public void onProgress(String requestId, long bytes, long totalBytes) {
        // example code starts here
        long progress = (long) bytes / totalBytes;
        // post progress to app UI (e.g. progress bar, notification)
        // example code ends here
		int prog = (int) progress;
		
    }

    @Override
    public void onSuccess(String requestId, Map resultData) {
        // your code here
		
    }

    @Override
    public void onError(String requestId, ErrorInfo error) {
        // your code here
		
    }

    @Override
    public void onReschedule(String requestId, ErrorInfo error) {
        // your code here
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
