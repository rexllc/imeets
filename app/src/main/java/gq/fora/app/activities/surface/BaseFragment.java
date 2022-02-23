package gq.fora.app.activities.surface;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import gq.fora.app.models.UserConfig;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.PreferencesManager;

public abstract class BaseFragment extends Fragment implements ServiceConnection {

    private SinchService.SinchServiceInterface mSinchServiceInterface;
    private PreferencesManager preferencesManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService();
        preferencesManager = new PreferencesManager(getActivity());
        if (UserConfig.getInstance().isLogin()) {
            if (!preferencesManager.hasKey("email_address")) {
                preferencesManager.addString(
                        "email_address", FirebaseAuth.getInstance().getCurrentUser().getEmail());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (UserConfig.getInstance().isLogin()) {
            UserConfig.getInstance().updateStatus(UserConfig.getInstance().getUid(), false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (UserConfig.getInstance().isLogin()) {
            UserConfig.getInstance().updateStatus(UserConfig.getInstance().getUid(), true);
        }
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface = (SinchService.SinchServiceInterface) iBinder;
            onServiceConnected();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        if (SinchService.class.getName().equals(componentName.getClassName())) {
            mSinchServiceInterface = null;
            onServiceDisconnected();
        }
    }

    protected void onServiceConnected() {
        // for subclasses
    }

    protected void onServiceDisconnected() {
        // for subclasses
    }

    protected SinchService.SinchServiceInterface getSinchServiceInterface() {
        return mSinchServiceInterface;
    }

    private Messenger messenger =
            new Messenger(
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                                case SinchService.MESSAGE_PERMISSIONS_NEEDED:
                                    Bundle bundle = msg.getData();
                                    String requiredPermission =
                                            bundle.getString(SinchService.REQUIRED_PERMISSION);
                                    ActivityCompat.requestPermissions(
                                            getActivity(), new String[] {requiredPermission}, 0);
                                    break;
                            }
                        }
                    });

    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        boolean granted = grantResults.length > 0;
        for (int grantResult : grantResults) {
            granted &= grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (granted) {
            // if permission granted
        } else {
            // if permission not granted
        }
        mSinchServiceInterface.retryStartAfterPermissionGranted();
    }

    private void bindService() {
        Intent serviceIntent = new Intent(getActivity(), SinchService.class);
        serviceIntent.putExtra(SinchService.MESSENGER, messenger);
        getActivity().bindService(serviceIntent, this, SinchService.BIND_AUTO_CREATE);
    }
}
