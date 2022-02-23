package gq.fora.app.models.interfaces;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import gq.fora.app.activities.picker.ImagePickerActivity;

public class ImagePicker {

    private static ImagePicker instance = null;
    private Context context;

    public Uri uri;

    private OnImagePickerListener pickListener;

    public ImagePicker(Context context) {
        this.context = context;
        this.pickListener = null;
    }

    public static ImagePicker newInstance(Context context) {
        if (instance == null) {
            instance = new ImagePicker(context);
        }

        return instance;
    }

    public interface OnImagePickerListener {
        public void onImagePick(Uri uri);
    }

    public void onImagePick(Uri uri) {
        this.uri = uri;
    }

    public void setOnImagePickerListener(OnImagePickerListener listener) {
        this.pickListener = listener;
    }

    public Uri getResult() {
        return this.uri;
    }

    public void show() {
		try {
        Intent openGallery = new Intent(context, ImagePickerActivity.class);
        context.startActivity(openGallery);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
