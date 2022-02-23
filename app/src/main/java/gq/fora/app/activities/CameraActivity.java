package gq.fora.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.Utils;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseFragment {

    private Calendar cal = Calendar.getInstance();
    private ImageView button, close, flash, switch_cam;
    private LinearLayout camera_header_layout, camera_button_layout;
    private PreviewView previewView;
    private View view;
    private int rotation;

    private static final String[] CAMERA_PERMISSION = new String[] {Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isFront = true;
    private SharedPreferences sharedPreferences;

    private String TAG = "FORA";
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    result -> {
                        if (result) {
                            init();
                        } else {

                        }
                    });

    @NonNull
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);
        Window window = getActivity().getWindow();
        View view = window.getDecorView();
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
        insets.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            insets.setAppearanceLightStatusBars(false);
            window.setStatusBarColor(0xFF000000);
        } else {
            insets.setAppearanceLightStatusBars(true);
            window.setStatusBarColor(0xFFFFFFFF);
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_camera, _container, false);
        initViews(_savedInstanceState, _view);
        if (hasCameraPermission()) {
            init();
        } else {
            requestPermission();
        }
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initViews(Bundle _savedInstanceState, View _view) {

        view = _view;

        button = _view.findViewById(R.id.button);
        close = _view.findViewById(R.id.close);
        flash = _view.findViewById(R.id.flash);
        switch_cam = _view.findViewById(R.id.switch_cam);

        camera_header_layout = _view.findViewById(R.id.camera_header_layout);
        camera_button_layout = _view.findViewById(R.id.camera_button_layout);

        previewView = (PreviewView) _view.findViewById(R.id.camera);

        Utils.rippleEffects(close, "#ffffff");
        Utils.rippleEffects(flash, "#ffffff");
        Utils.rippleEffects(switch_cam, "#ffffff");

        /*Create singleThreadInstance*/
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public void init() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());
        cameraProviderFuture.addListener(
                () -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        cameraProvider.unbindAll();
                        bindPreview(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                },
                ContextCompat.getMainExecutor(getActivity()));

        close.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        flash.setOnClickListener((View v) -> {});

        close.setColorFilter(0xFFFFFFFF);
        flash.setColorFilter(0xFFFFFFFF);
        switch_cam.setColorFilter(0xFFFFFFFF);
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();

        cameraSelector =
                new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        OrientationEventListener orientationEventListener =
                new OrientationEventListener((Context) getActivity()) {
                    @Override
                    public void onOrientationChanged(int orientation) {

                        // Monitors orientation values to determine the target rotation value
                        if (orientation >= 45 && orientation < 135) {
                            rotation = Surface.ROTATION_270;
                        } else if (orientation >= 135 && orientation < 225) {
                            rotation = Surface.ROTATION_180;
                        } else if (orientation >= 225 && orientation < 315) {
                            rotation = Surface.ROTATION_90;
                        } else {
                            rotation = Surface.ROTATION_0;
                        }
                    }
                };

        orientationEventListener.enable();

        ImageCapture imageCapture =
                new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .setTargetRotation(rotation)
                        .setIoExecutor(cameraExecutor)
                        .setTargetResolution(new Size(view.getWidth(), view.getHeight()))
                        .build();

        Camera camera =
                cameraProvider.bindToLifecycle(
                        (LifecycleOwner) getActivity(), cameraSelector, imageCapture, preview);
        CameraControl cameraControl = camera.getCameraControl();

        switch_cam.setOnClickListener(
                (View v) -> {
                    if (isFront) {
                        cameraSelector =
                                new CameraSelector.Builder()
                                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                        .build();
                        cameraProvider.unbindAll();
                        Camera camera2 =
                                cameraProvider.bindToLifecycle(
                                        (LifecycleOwner) getActivity(),
                                        cameraSelector,
                                        imageCapture,
                                        preview);
                        isFront = false;
                    } else {

                        cameraSelector =
                                new CameraSelector.Builder()
                                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                        .build();
                        cameraProvider.unbindAll();
                        Camera camera1 =
                                cameraProvider.bindToLifecycle(
                                        (LifecycleOwner) getActivity(),
                                        cameraSelector,
                                        imageCapture,
                                        preview);
                        isFront = true;
                    }
                });

        button.setOnClickListener(
                (View v) -> {
                    String SAVE_PATH = FileUtils.getPackageDataDir(getActivity()) + "/DCIM/";
                    if (!FileUtils.isExistFile(SAVE_PATH)) {
                        FileUtils.makeDir(SAVE_PATH);
                    }
                    File file = new File(SAVE_PATH, "FORA_" + cal.getTimeInMillis() + ".jpg");
                    ImageCapture.OutputFileOptions outputFileOptions =
                            new ImageCapture.OutputFileOptions.Builder(file).build();
                    imageCapture.takePicture(
                            outputFileOptions,
                            ContextCompat.getMainExecutor(getActivity()),
                            new ImageCapture.OnImageSavedCallback() {
                                @Override
                                public void onImageSaved(
                                        ImageCapture.OutputFileResults outputFileResults) {
                                    // insert your code here.
                                    Toast.makeText(
                                                    getActivity(),
                                                    "Photo saved : " + file.getPath(),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }

                                @Override
                                public void onError(ImageCaptureException error) {
                                    // insert your code here.
                                }
                            });
                });
    }

    public void setCameraResolution(int width, int height) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(width, height)).build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        Window window = getActivity().getWindow();
        View view = window.getDecorView();
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            insets.setAppearanceLightStatusBars(false);
            window.setStatusBarColor(0xFF212121);
        } else {
            insets.setAppearanceLightStatusBars(true);
            window.setStatusBarColor(0xFFFFFFFF);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        requestCameraPermission.launch(Manifest.permission.CAMERA);
    }
}
