package gq.fora.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentTransaction;

import com.bitvale.switcher.SwitcherX;
import com.wuyr.rippleanimation.RippleAnimation;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.utils.Utils;

import kotlin.Unit;

import java.io.IOException;
import java.io.InputStream;

public class ThemeSettingsActivity extends BaseFragment {

    private FrameLayout actionbar;
    private SwitcherX dark_mode_switch;
    private ImageView dark_mode, back, wallpapers, wallpapers_image;
    private LinearLayout rootLayout, toolbar, chat_wallpapers_layout;
    private TextView dark_mode_text,
            header_01,
            header_02,
            subheader_01,
            title,
            wallpapers_text,
            wallpaper_type;
    private int NightMode;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        View _view = inflater.inflate(R.layout.activity_theme_settings, parent, false);
        initializeViews(savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    @Override
    public void onStart() {
        super.onStart();

        int nightModeFlags =
                getContext().getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                darkMode(true);
                toolBarDark(true);
                dark_mode_switch.setChecked(true, true);
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                darkMode(false);
                toolBarDark(false);
                dark_mode_switch.setChecked(false, false);
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                darkMode(false);
                toolBarDark(false);
                dark_mode_switch.setChecked(false, false);
                break;
        }

        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            darkMode(true);
            toolBarDark(true);
            dark_mode_switch.setChecked(true, true);
        } else {
            darkMode(false);
            toolBarDark(false);
            dark_mode_switch.setChecked(false, false);
        }
    }

    public void initializeViews(Bundle savedInstanceState, View view) {
        actionbar = view.findViewById(R.id.actionbar);
        dark_mode_switch = view.findViewById(R.id.dark_mode_switch);
        dark_mode = view.findViewById(R.id.dark_mode);
        rootLayout = view.findViewById(R.id.rootLayout);
        dark_mode_text = view.findViewById(R.id.dark_mode_text);
        header_01 = view.findViewById(R.id.header_01);
        header_02 = view.findViewById(R.id.header_02);
        subheader_01 = view.findViewById(R.id.subheader_01);
        chat_wallpapers_layout = view.findViewById(R.id.chat_wallpapers_layout);
        wallpapers_image = view.findViewById(R.id.wallpapers_image);
        wallpaper_type = view.findViewById(R.id.wallpaper_type);
        wallpapers_text = view.findViewById(R.id.wallpapers_text);
        wallpapers = view.findViewById(R.id.wallpapers);

        dark_mode_switch.setEnabled(false);

        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("wallpaper")) {
            wallpaper_type.setText(
                    sharedPreferences.getString("wallpaper", "").replace("wallpapers/", ""));
        } else {
            wallpaper_type.setText("Default");
        }

        try {
            // get input stream
            InputStream ims =
                    getActivity().getAssets().open(sharedPreferences.getString("wallpaper", ""));
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            wallpapers_image.setImageDrawable(d);
        } catch (IOException ex) {
            return;
        }

        dark_mode_switch.setOnCheckedChangeListener(
                checked -> {
                    if (checked) {
                        darkMode(true);
                        toolBarDark(true);
                        sharedPreferences.edit().putString("dark_mode", "true").apply();
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        darkMode(false);
                        toolBarDark(false);
                        sharedPreferences.edit().putString("dark_mode", "false").apply();
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    RippleAnimation.create(view).setDuration(300).start();
                    return Unit.INSTANCE;
                });
    }

    public void initializeLogic() {
        View action_bar = LayoutInflater.from(getActivity()).inflate(R.layout.fora_toolbar, null);
        actionbar.addView(action_bar);

        toolbar = (LinearLayout) action_bar.findViewById(R.id.toolbar);
        back = (ImageView) action_bar.findViewById(R.id.back);
        title = (TextView) action_bar.findViewById(R.id.title);

        title.setText("Themes");
        actionbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        chat_wallpapers_layout.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new WallpaperActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });
    }

    public void darkMode(boolean isNight) {
        Window window = getActivity().getWindow();
        View view = window.getDecorView();
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);

        if (isNight) {
            rootLayout.setBackgroundColor(0xFF212121);
            dark_mode_text.setTextColor(0xFFFFFFFF);
            wallpapers_text.setTextColor(0xFFFFFFFF);
            wallpaper_type.setTextColor(0xFFFFFFFF);
            dark_mode.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            wallpapers.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            dark_mode.setColorFilter(0xFFFFFFFF);
            wallpapers.setColorFilter(0xFFFFFFFF);
            header_01.setTextColor(0xFFFFFFFF);
            header_02.setTextColor(0xFFFFFFFF);
            subheader_01.setTextColor(0xFFFFFFFF);
            insets.setAppearanceLightStatusBars(false);
            window.setStatusBarColor(0xFF212121);
            Utils.rippleRoundStroke(chat_wallpapers_layout, "#212121", "#e0e0e0", 0, 0, "#212121");
        } else {
            rootLayout.setBackgroundColor(0xFFFFFFFF);
            dark_mode_text.setTextColor(0xFF000000);
            wallpapers_text.setTextColor(0xFF000000);
            wallpaper_type.setTextColor(0xFF000000);
            dark_mode.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            wallpapers.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            dark_mode.setColorFilter(0xFF000000);
            wallpapers.setColorFilter(0xFF000000);
            header_01.setTextColor(0xFF000000);
            header_02.setTextColor(0xFF000000);
            subheader_01.setTextColor(0xFF000000);
            insets.setAppearanceLightStatusBars(true);
            window.setStatusBarColor(0xFFFFFFFF);
        }
    }

    public void toolBarDark(boolean isNight) {
        if (isNight) {
            title.setTextColor(0xFFFFFFFF);
            actionbar.setBackgroundColor(0xFF212121);
            back.setColorFilter(0xFFFFFFFF);
        } else {
            title.setTextColor(0xFF000000);
            actionbar.setBackgroundColor(0xFFFFFFFF);
            back.setColorFilter(0xFF000000);
        }
    }
}
