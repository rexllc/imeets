package gq.fora.app.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.*;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.initializeApp;
import gq.fora.app.models.UserConfig;
import gq.fora.app.notify.Notify;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.Utils;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProfilePictureActivity extends BaseFragment {

    private LinearLayout toolbar;
    private ImageView back;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CircleImageView avatar;
    private MaterialButton save_button;
    private static final int PROFILE_IMAGE_REQ_CODE = 101;
    private String imagePath;
    private FirebaseStorage storage = FirebaseStorage.getInstance("gs://fora-store.appspot.com");
    private StorageReference storageRef = storage.getReference();
    private UploadTask uploadTask;
    private TimerTask timer;
    private Timer _timer = new Timer();
    private ExecutorService executor;
    private Handler handler;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (imagePath != null) {
                        FileUtils.deleteFile(imagePath);
                    }
                }
            };

    ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri uri = result.getData().getData();
                            // Use the uri to load the image
                            Glide.with(getActivity()).load(uri).into(avatar);
                            imagePath = uri.getPath();
                            save_button.setEnabled(true);
                        } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
                            // Use ImagePicker.Companion.getError(result.getData()) to show an error
                        }
                    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_add_profile, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
            } else {
                // Granted
            }
        } else {
            initializeLogic();
        }
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            // Granted
        }
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        toolbar = view.findViewById(R.id.toolbar);
        avatar = view.findViewById(R.id.avatar);
        save_button = view.findViewById(R.id.save_button);
        back = view.findViewById(R.id.back);

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        save_button.setEnabled(false);

        avatar.setOnClickListener(
                (View v) -> {
                    ImagePicker.with(getActivity())
                            .crop() // Crop image(Optional), Check Customization for more option
                            .compress(524)
                            .galleryOnly() // We have to define what image provider we want to use
                            .galleryMimeTypes(new String[] {"image/png", "image/jpg", "image/jpeg"})
                            .maxResultSize(
                                    1080, 1080) // Final image resolution will be less than 1080 x
                            // 1080(Optional)
                            .createIntent(
                                    (Function1)
                                            (new Function1() {
                                                public Object invoke(Object var1) {
                                                    this.invoke((Intent) var1);
                                                    return Unit.INSTANCE;
                                                }

                                                public final void invoke(@NotNull Intent it) {
                                                    Intrinsics.checkNotNullParameter(it, "it");
                                                    launcher.launch(it);
                                                }
                                            }));
                });

        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (imagePath != null) {
                        FileUtils.deleteFile(imagePath);
                    }
                });

        save_button.setOnClickListener(
                (View v) -> {
                    if (imagePath != null) {
                        executor.execute(
                                () -> {
                                    // Background work here
                                    uploadProfilePicture();
                                    handler.post(
                                            () -> {
                                                // UI Thread work here
                                                getActivity()
                                                        .getSupportFragmentManager()
                                                        .popBackStack();
                                            });
                                });
                    }
                });
    }

    public void initializeLogic() {
        toolbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
    }

    public void showNotification(String title, String body, int progress, int max) {
        try {
            Notify.create(initializeApp.context)
                    .setTitle(title)
                    .setContent(body)
                    .setProgress(max, progress, false)
                    .setChannelId("1")
                    .setChannelName("Upload Task")
                    .setChannelDescription("Notification for uploading.")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setImportance(Notify.Importance.HIGH)
                    .setAutoCancel(true)
                    .enableVibration(true)
                    .setId(100)
                    .setColor(R.color.primary)
                    .show();
        } catch (Exception ex) {
            ex.printStackTrace();
            FileUtils.writeFile(
                    ex.toString(),
                    FileUtils.getPublicDir(Environment.DIRECTORY_DOCUMENTS)
                            + "/"
                            + "logs-"
                            + System.currentTimeMillis()
                            + ".txt");
        }
    }

    public void uploadProfilePicture() {
        if (imagePath != null) {
            Uri file = Uri.fromFile(new File(imagePath));
            StorageReference picRef =
                    storageRef.child(
                            "images/"
                                    + UserConfig.getInstance().getUid()
                                    + "/"
                                    + file.getLastPathSegment());
            uploadTask = picRef.putFile(file);

            // Register observers to listen for when the download is done or if it
            // fails

            uploadTask
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress =
                                            (100.0 * taskSnapshot.getBytesTransferred())
                                                    / taskSnapshot.getTotalByteCount();
                                    int currentprogress = (int) progress;
                                    showNotification(
                                            "iMeets",
                                            "Uploading Profile Picture...",
                                            100,
                                            currentprogress);
                                    save_button.setEnabled(false);
                                }
                            })
                    .addOnPausedListener(
                            new OnPausedListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                    System.out.println("Upload is paused");
                                }
                            })
                    .continueWithTask(
                            new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(Task<UploadTask.TaskSnapshot> task)
                                        throws Exception {
                                    return picRef.getDownloadUrl();
                                }
                            })
                    .addOnCompleteListener(
                            new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        updateProfile(task.getResult().toString());
                                    } else {
                                        ForaUtil.showMessage(getActivity(), "Upload failed.");
                                    }
                                }
                            });
        }
    }

    private void updateProfile(String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("userPhoto", url);
        database.collection("users").document(UserConfig.getInstance().getUid()).update(map);
        FirebaseAuth.getInstance()
                .getCurrentUser()
                .updateProfile(
                        new UserProfileChangeRequest.Builder()
                                .setDisplayName(
                                        FirebaseAuth.getInstance()
                                                .getCurrentUser()
                                                .getDisplayName())
                                .setPhotoUri(Uri.parse(url))
                                .build())
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(Task<Void> _param1) {
                                final boolean _success = _param1.isSuccessful();
                                final String _errorMessage =
                                        _param1.getException() != null
                                                ? _param1.getException().getMessage()
                                                : "";

                                if (_success) {
                                    showNotification("iMeets", "Profile Picture updated.", 0, 0);
                                    ForaUtil.showMessage(initializeApp.context, "Upload complete.");
                                } else {
                                    showNotification("iMeets", "Upload failed..", 0, 0);
                                }
                                FileUtils.deleteFile(imagePath);
                                executor.shutdown();
                            }
                        });
    }
}
