package gq.fora.app.datasync.worker;

/*Use to synchronize contacts in user device.*/

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncContacts extends Worker {
	
	public SyncContacts(@NonNull Context ctx, @NonNull WorkerParameters params) {
		super(ctx, params);
	}
	
	@Override
	public Result doWork() {
		// Sync Contacts
	    return Result.success();
	}

}