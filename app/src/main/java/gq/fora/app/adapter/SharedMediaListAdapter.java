package gq.fora.app.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.listeners.OnImageChangeListener;
import com.stfalcon.imageviewer.loader.ImageLoader;
import gq.fora.app.R;
import java.util.ArrayList;
import java.util.HashMap;

public class SharedMediaListAdapter extends RecyclerView.Adapter {

    private ArrayList<HashMap<String, Object>> item;
    private ArrayList<String> images;
    private double _position = 0;

    public SharedMediaListAdapter(ArrayList<HashMap<String, Object>> arr) {
        item = arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ImageHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((ImageHolder) holder).bind(position);
    }

    @Override
    public int getItemCount() {
        return item.size();
    }

    private class ImageHolder extends RecyclerView.ViewHolder {

        private ImageView picturesView;

        private ImageHolder(View convertView) {
            super(convertView);
            picturesView = convertView.findViewById(R.id.imageview1);
            picturesView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        void bind(int position) {

            Glide.with(picturesView)
                    .load(Uri.parse(item.get((int) position).get("imageUrl").toString()))
                    .skipMemoryCache(true)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .into(picturesView);

            images = new ArrayList<>();

            picturesView.setOnClickListener(
                    (View v) -> {
                        if (item != null && !item.isEmpty()) {
                            images.add(item.get((int) position).get("imageUrl").toString());
                            new StfalconImageViewer.Builder<String>(
                                            picturesView.getContext(),
                                            images,
                                            new ImageLoader<String>() {
                                                @Override
                                                public void loadImage(
                                                        ImageView imageView, String image) {
                                                    Glide.with(imageView)
                                                            .load(image)
                                                            .into(imageView);
                                                }
                                            })
                                    .withTransitionFrom(picturesView)
                                    .withImageChangeListener(
                                            new OnImageChangeListener() {
                                                @Override
                                                public void onImageChange(int position) {}
                                            })
                                    .show();
                        }
                    });
        }
    }
}
