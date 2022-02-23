package gq.fora.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.utils.Utils;
import gq.fora.app.widgets.appcompat.ImageViewCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class WallpaperActivity extends BaseFragment {

    private ViewPager2 pager;
    private WallpaperAdapter adapter;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ImageView back;
    private LinearLayout toolbar;
    private TextView title;
    private FrameLayout actionbar;
    private MaterialButton save_button;
    private SharedPreferences themes;

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstance) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_wallpaper, parent, false);
        initViews(savedInstance, view);
        return view;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    public void initViews(Bundle savedInstance, View view) {
        pager = view.findViewById(R.id.pager);
        actionbar = view.findViewById(R.id.actionbar);
        save_button = view.findViewById(R.id.save_button);

        themes = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);

        for (int i = 0; i < 18; i++) {
            arrayList.add("wallpapers/wallpaper_" + String.format("%02d", i) + ".jpg");
        }
        adapter = new WallpaperAdapter(getActivity(), arrayList);
        pager.setAdapter(adapter);
        adapter.notifyItemInserted((int) 0);

        pager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_00.jpg")) {
            pager.setCurrentItem((int) 0);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_01.jpg")) {
            pager.setCurrentItem((int) 1);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_02.jpg")) {
            pager.setCurrentItem((int) 2);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_03.jpg")) {
            pager.setCurrentItem((int) 3);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_04.jpg")) {
            pager.setCurrentItem((int) 4);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_05.jpg")) {
            pager.setCurrentItem((int) 5);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_06.jpg")) {
            pager.setCurrentItem((int) 6);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_07.jpg")) {
            pager.setCurrentItem((int) 7);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_08.jpg")) {
            pager.setCurrentItem((int) 8);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_09.jpg")) {
            pager.setCurrentItem((int) 9);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_10.jpg")) {
            pager.setCurrentItem((int) 10);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_11.jpg")) {
            pager.setCurrentItem((int) 11);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_12.jpg")) {
            pager.setCurrentItem((int) 12);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_13.jpg")) {
            pager.setCurrentItem((int) 13);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_14.jpg")) {
            pager.setCurrentItem((int) 14);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_15.jpg")) {
            pager.setCurrentItem((int) 15);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_16.jpg")) {
            pager.setCurrentItem((int) 16);
        } else if (themes.getString("wallpaper", "").equals("wallpapers/wallpaper_17.jpg")) {
            pager.setCurrentItem((int) 17);
        }

        pager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                    }

                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);

                        save_button.setOnClickListener(
                                (View v) -> {
                                    themes.edit()
                                            .putString("wallpaper", arrayList.get((int) position))
                                            .apply();
                                    getActivity().getSupportFragmentManager().popBackStack();
                                    Toast.makeText(
                                                    getActivity(),
                                                    "Wallpaper Saved.",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });

                        Log.e("Selected_Page", String.valueOf(position));
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                    }
                });

        actionbar.bringToFront();

        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText("Wallpaper");
        title.setTextColor(0xFFFFFFFF);
        back.setColorFilter(0xFFFFFFFF);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });
    }

    // An equivalent ViewPager2 adapter class
    public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

        private Context context;
        private ArrayList<String> wallpaperList = new ArrayList<>();

        public WallpaperAdapter(final Context context, final ArrayList<String> wallpaperList) {
            this.context = context;
            this.wallpaperList = wallpaperList;
        }

        @Override
        public int getItemCount() {
            return wallpaperList.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View _view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.wallpapers, parent, false);
            return new ViewHolder(_view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            try {
                // get input stream
                InputStream ims = context.getAssets().open(wallpaperList.get(position));
                // load image as Drawable
                Drawable d = Drawable.createFromStream(ims, null);
                // set image to ImageView
                holder.mImage.setImageDrawable(d);
            } catch (IOException ex) {
                // Toast.makeText(getActivity(), ex.toString(), Toast.LENGTH_LONG).show();
                return;
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private ImageViewCompat mImage;

            public ViewHolder(View view) {
                super(view);
                mImage = view.findViewById(R.id.mImage);
            }
        }
    }
}
