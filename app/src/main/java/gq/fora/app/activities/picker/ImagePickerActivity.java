package gq.fora.app.activities.picker;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.style.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.*;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import gq.fora.app.R;
import gq.fora.app.activities.picker.listener.SwipeDismissTouchListener;
import gq.fora.app.widgets.GridSpacingItemDecoration;
import java.io.*;
import java.io.File;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.regex.*;

public class ImagePickerActivity extends DialogFragment {

    private LinearLayout linear4;
    private ImageView imageview1;
    private LinearLayout linear5;
    private ImageView imageview2;
    private LinearLayout linear3;
    private TextView textview2;
    private ArrayList<String> images;
    private RecyclerView imageList;
    private OnImagePickerListener pickListener;
    private static ImagePickerActivity instance = null;
    private Context context;
    private Uri uri;
    private boolean isLight;
    private String color;
    private String finalPath;
    private SmartMaterialSpinner<String> spinner1;

    public ImagePickerActivity() {
        // Default
    }

    public ImagePickerActivity(Context context) {
        this.context = context;
        this.pickListener = null;
        this.color = "#FFFFFF";
    }

    @NonNull
    public static ImagePickerActivity newInstance(Context context) {
        if (instance == null) {
            instance = new ImagePickerActivity(context);
        }

        return instance;
    }

    private final ActivityResultLauncher<String> requestStoragePermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    result -> {
                        if (result) {
                            initializeLogic();
                        } else {
                            Snackbar.make(
                                            imageList,
                                            "Storage permission is required.",
                                            Snackbar.LENGTH_SHORT)
                                    .show();
                        }
                    });

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_image_picker_fora, _container, false);

        ViewGroup.LayoutParams lp =
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        _view.setLayoutParams(lp);
        initialize(_savedInstanceState, _view);
        imageList.setAdapter(new ImageAdapter(getActivity()));
        imageList.setHasFixedSize(true);
        imageList.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        int spanCount = 2;
        int spacing = 5;
        boolean includeEdge = false;
        imageList.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                requestStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                // Granted
                initializeLogic();
            }
        } else {
            initializeLogic();
        }
        return _view;
    }

    private void initialize(Bundle _savedInstanceState, View _view) {
        linear4 = _view.findViewById(R.id.linear4);
        imageview1 = _view.findViewById(R.id.imageview1);
        linear5 = _view.findViewById(R.id.linear5);
        imageview2 = _view.findViewById(R.id.imageview2);
        imageList = _view.findViewById(R.id.imageList);
        linear3 = _view.findViewById(R.id.linear3);
        textview2 = _view.findViewById(R.id.textview2);
        spinner1 = _view.findViewById(R.id.spinner1);

        imageview1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View _view) {
                        dismiss();
                    }
                });

        imageview2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View _view) {}
                });

        spinner1.setItem(getAllBuckets());
        spinner1.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int position, long id) {
                        imageList.setAdapter(
                                new ImageAdapter(
                                        getActivity(), getAllBuckets().get((int) position)));
                        imageList.setHasFixedSize(true);
                        imageList.setLayoutManager(new GridLayoutManager(getActivity(), 3));
						spinner1.setHint(getAllBuckets().get((int) position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.getDecorView()
                .setOnTouchListener(
                        new SwipeDismissTouchListener(
                                window.getDecorView(),
                                null,
                                new SwipeDismissTouchListener.DismissCallbacks() {
                                    @Override
                                    public boolean canDismiss(Object token) {
                                        return true;
                                    }

                                    @Override
                                    public void onDismiss(View view, Object token) {
                                        dismiss();
                                    }
                                }));
    }

    private void initializeLogic() {
        linear4.setElevation((int) 3);
        linear3.setVisibility(View.GONE);
        _RippleEffects("#E0E0E0", imageview1);
        _RippleEffects("#E0E0E0", imageview2);
        imageview1.setColorFilter(0xFF000000);
        imageview2.setColorFilter(0xFF000000);
        spinner1.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 0, 0xFFFFFFFF));
    }

    private ArrayList<String> getAllShownImagesPath(Activity activity) {
        Uri uri;
        Cursor cursor;
        int column_index_data, column_index_folder_name;
        ArrayList<String> listOfAllImages = new ArrayList<String>();
        String absolutePathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            android.provider.MediaStore.MediaColumns.DATA,
            android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            android.provider.MediaStore.MediaColumns.MIME_TYPE
        };

        cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        column_index_data =
                cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA);
        column_index_folder_name =
                cursor.getColumnIndexOrThrow(
                        android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            listOfAllImages.add(absolutePathOfImage);
        }
        return listOfAllImages;
    }

    public ArrayList<String> getAllShownImagesPath(String bucketName) {
        Uri uri;
        Cursor cursor;
        int column_index_data, column_index_folder_name;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage;
        String[] selectionArgs = new String[] {bucketName};
        String selection = android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
            android.provider.MediaStore.MediaColumns.DATA,
            android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };
        cursor =
                requireContext()
                        .getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);

        column_index_data =
                cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA);
        column_index_folder_name =
                cursor.getColumnIndexOrThrow(
                        android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);
            listOfAllImages.add(absolutePathOfImage);
        }
        return listOfAllImages;
    }

    public ArrayList<String> getAllBuckets() {
        HashSet<String> hs = new HashSet<>();
        String[] projection =
                new String[] {
                    android.provider.MediaStore.Images.Media._ID,
                    android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    android.provider.MediaStore.Images.Media.DATE_TAKEN
                };
        Uri images = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur =
                requireContext().getContentResolver().query(images, projection, null, null, null);
        Log.i("ListingImages", " query count=" + cur.getCount());
        if (cur.moveToFirst()) {
            String bucket;
            String date;
            int bucketColumn =
                    cur.getColumnIndex(
                            android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int dateColumn =
                    cur.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_TAKEN);
            do {
                bucket = cur.getString(bucketColumn);
                date = cur.getString(dateColumn);
                hs.add(bucket);

                Log.i("ListingImages", " bucket=" + bucket + "  date_taken=" + date);
            } while (cur.moveToNext());
        }
        return new ArrayList<>(hs);
    }

    public class ImageAdapter extends RecyclerView.Adapter {

        private Activity context;

        public ImageAdapter(Activity localContext) {
            context = localContext;
            images = getAllShownImagesPath(context);
        }

        public ImageAdapter(Activity localContext, String bucketName) {
            context = localContext;
            images = getAllShownImagesPath(bucketName);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.grid_item, parent, false);
            return new ImageHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ((ImageHolder) holder).bind(position);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        private class ImageHolder extends RecyclerView.ViewHolder {

            private ImageView picturesView;

            private ImageHolder(View convertView) {
                super(convertView);
                picturesView = convertView.findViewById(R.id.imageview1);
                picturesView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }

            void bind(int position) {
                Glide.with(context)
                        .load(images.get(position))
                        .skipMemoryCache(true)
                        .thumbnail(0.1f)
                        .centerCrop()
                        .into(picturesView);

                picturesView.setOnClickListener(
                        (View v) -> {
                            if (images != null && !images.isEmpty()) {
                                if (pickListener != null) {
                                    pickListener.onImagePick(
                                            Uri.fromFile(new File(images.get((int) position))));
                                    dismiss();
                                }
                            }
                        });
            }
        }
    }

    public void _status_bar_color(final String _colour1, final String _colour2) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Window w = getActivity().getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.parseColor(_colour1));
            w.setNavigationBarColor(Color.parseColor(_colour2));
        }
    }

    public void _RippleEffects(final String _color, final View _view) {
        android.content.res.ColorStateList clr =
                new android.content.res.ColorStateList(
                        new int[][] {new int[] {}}, new int[] {Color.parseColor(_color)});
        android.graphics.drawable.RippleDrawable ripdr =
                new android.graphics.drawable.RippleDrawable(clr, null, null);
        _view.setBackground(ripdr);
    }

    public void _removeScollBar(final View _view) {
        _view.setVerticalScrollBarEnabled(false);
        _view.setHorizontalScrollBarEnabled(false);
    }

    public interface OnImagePickerListener {
        void onImagePick(@NonNull Uri uri);
    }

    public void setOnImagePickerListener(OnImagePickerListener listener) {
        this.pickListener = listener;
    }

    public static String ErrorResult() {
        return "File not found.";
    }

    public String openFileDescriptor(Uri path) {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            String filePath = new File(getActivity().getCacheDir(), "tmp").getAbsolutePath();
            android.os.ParcelFileDescriptor pfd =
                    getActivity().getContentResolver().openFileDescriptor(path, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                input = new FileInputStream(fd);
                output = new FileOutputStream(filePath);
                int read;
                byte[] bytes = new byte[4096];
                while ((read = input.read(bytes)) != -1) {
                    output.write(bytes, 0, read);
                }
                File sharedFile = new File(filePath);
                finalPath = sharedFile.getPath();
            }
        } catch (Exception ex) {
        } finally {
            try {
                input.close();
                output.close();
            } catch (Exception ignored) {
                finalPath = null;
            }
        }

        return finalPath;
    }
}
