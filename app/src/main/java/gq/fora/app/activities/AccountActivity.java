package gq.fora.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.activities.AccountSettingsActivity;
import gq.fora.app.activities.surface.AccountSwitcherActivity;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.Utils;

public class AccountActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase.getReference("users");

    private LinearLayout toolbar,
            linear1,
            online_status,
            dark_mode_layout,
            message_request_layout,
            switch_account_layout,
            username_layout,
            my_account_layout,
            my_status_layout;
    private ImageView back,
            online_indicator,
            dark_mode,
            message_request,
            switch_account,
            username,
            my_account,
            my_status;
    private TextView title,
            display_name,
            status,
            dark_mode_status,
            message_request_count,
            username_text,
            dark_mode_text,
            request_text,
            switch_text,
            profile,
            username_title,
            my_account_title,
            my_status_title;
    private CircleImageView avatar;
    private SharedPreferences sharedPreferences;

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
        View _view = _inflater.inflate(R.layout.activity_account, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            darkMode(true);
            toolBarDark(true);
        } else {
            darkMode(false);
            toolBarDark(false);
        }
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        toolbar = view.findViewById(R.id.toolbar);
        linear1 = view.findViewById(R.id.linear1);
        back = view.findViewById(R.id.back);
        title = view.findViewById(R.id.title);
        status = view.findViewById(R.id.status);
        online_status = view.findViewById(R.id.online_status);
        display_name = view.findViewById(R.id.display_name);
        avatar = view.findViewById(R.id.avatar);
        dark_mode = view.findViewById(R.id.dark_mode);
        dark_mode_layout = view.findViewById(R.id.dark_mode_layout);
        dark_mode_status = view.findViewById(R.id.dark_mode_status);
        message_request = view.findViewById(R.id.message_request);
        message_request_layout = view.findViewById(R.id.message_request_layout);
        message_request_count = view.findViewById(R.id.message_request_count);
        switch_account_layout = view.findViewById(R.id.switch_account_layout);
        switch_account = view.findViewById(R.id.switch_account);
        username = view.findViewById(R.id.username);
        username_layout = view.findViewById(R.id.username_layout);
        username_text = view.findViewById(R.id.username_text);
        dark_mode_text = view.findViewById(R.id.dark_mode_text);
        request_text = view.findViewById(R.id.request_text);
        switch_text = view.findViewById(R.id.switch_text);
        profile = view.findViewById(R.id.profile);
        username_title = view.findViewById(R.id.username_title);
        my_account_layout = view.findViewById(R.id.my_account_layout);
        my_account = view.findViewById(R.id.my_account);
        my_account_title = view.findViewById(R.id.my_account_title);
        my_status_layout = view.findViewById(R.id.my_status_layout);
        my_status = view.findViewById(R.id.my_status);
        my_status_title = view.findViewById(R.id.my_status_title);

        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);

        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        dark_mode_layout.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new ThemeSettingsActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });
        avatar.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new AddProfilePictureActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });
        message_request_layout.setOnClickListener((View v) -> {});

        switch_account_layout.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new AccountSwitcherActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });

        username_layout.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new ChangeUsernameActivity())
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                });

        my_account_layout.setOnClickListener(
                (v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new AccountSettingsActivity())
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                });

        my_status_layout.setOnClickListener((v) -> {});
		
		title.setText(getResources().getString(R.string.me));
    }

    public void initializeLogic() {
        toolbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        display_name.setText(UserConfig.getInstance().getDisplayName());
        if (UserConfig.getInstance().getUserPhoto() != null) {
            String photo = UserConfig.getInstance().getUserPhoto();
            Glide.with(getActivity()).load(Uri.parse(photo)).into(avatar);
        }

        UserConfig.getInstance()
                .setStatus(
                        getActivity(),
                        status,
                        FirebaseAuth.getInstance().getCurrentUser().getUid());
        if (UserConfig.getInstance()
                .isOnline(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            online_status.setVisibility(View.VISIBLE);
        } else {
            online_status.setVisibility(View.GONE);
        }
		
		database.collection("users")
				.document(UserConfig.getInstance().getUid())
				.addSnapshotListener(new EventListener<DocumentSnapshot>() {
					@Override
					public void onEvent(DocumentSnapshot value, FirebaseFirestoreException error) {
						username_text.setText("me.fora.gq/" + value.getString("username"));
					}
				});

        Utils.rippleRoundStroke(dark_mode_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(message_request_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(switch_account_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(username_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(my_account_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(my_status_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
    }

    public void darkMode(boolean isNight) {
        if (isNight) {
            dark_mode_status.setText("On");
            dark_mode.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            message_request.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            switch_account.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            username.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            message_request_count.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view_night));
            linear1.setBackgroundColor(0xFF212121);
            display_name.setTextColor(0xFFFFFFFF);
            status.setTextColor(0xFFFFFFFF);
            dark_mode_text.setTextColor(0xFFFFFFFF);
            dark_mode_status.setTextColor(0xFFFFFFFF);
            request_text.setTextColor(0xFFFFFFFF);
            switch_text.setTextColor(0xFFFFFFFF);
            profile.setTextColor(0xFFFFFFFF);
            username_text.setTextColor(0xFFFFFFFF);
            username_title.setTextColor(0xFFFFFFFF);
            my_account_title.setTextColor(0xFFFFFFFF);
            my_status_title.setTextColor(0xFFFFFFFF);
            dark_mode.setColorFilter(0xFFFFFFFF);
            message_request.setColorFilter(0xFFFFFFFF);
            switch_account.setColorFilter(0xFFFFFFFF);
            my_status.setColorFilter(0xFFFFFFFF);
            username.setColorFilter(0xFFFFFFFF);
            my_account.setColorFilter(0xFFFFFFFF);
            Utils.rippleRoundStroke(dark_mode_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(message_request_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(switch_account_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(username_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(my_account_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(my_account_layout, "#212121", "#e0e0e0", 0, 0, "#ffffff");
        } else {
            dark_mode_status.setText("Off");
            dark_mode.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            message_request.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            switch_account.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            username.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            message_request_count.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_view));
            linear1.setBackgroundColor(0xFFFFFFFF);
            display_name.setTextColor(0xFF000000);
            status.setTextColor(0xFF9e9e9e);
            dark_mode_text.setTextColor(0xFF000000);
            dark_mode_status.setTextColor(0xFF9e9e9e);
            request_text.setTextColor(0xFF000000);
            switch_text.setTextColor(0xFF000000);
            profile.setTextColor(0xFF000000);
            username_text.setTextColor(0xFF9e9e9e);
            username_title.setTextColor(0xFF000000);
            my_account_title.setTextColor(0xFF000000);
            my_status_title.setTextColor(0xFF000000);
            dark_mode.setColorFilter(0xFF000000);
            message_request.setColorFilter(0xFF000000);
            switch_account.setColorFilter(0xFF000000);
            username.setColorFilter(0xFF000000);
            my_account.setColorFilter(0xFF000000);
            my_status.setColorFilter(0xFF000000);
            Utils.rippleRoundStroke(dark_mode_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(message_request_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(switch_account_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(username_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(my_account_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(my_account_layout, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        }
    }

    public void toolBarDark(boolean isNight) {
        if (isNight) {
            title.setTextColor(0xFFFFFFFF);
            toolbar.setBackgroundColor(0xFF212121);
            back.setColorFilter(0xFFFFFFFF);
        } else {
            title.setTextColor(0xFF000000);
            toolbar.setBackgroundColor(0xFFFFFFFF);
            back.setColorFilter(0xFF000000);
        }
    }
}
